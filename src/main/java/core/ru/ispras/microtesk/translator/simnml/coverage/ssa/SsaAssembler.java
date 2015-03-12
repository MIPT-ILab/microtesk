package ru.ispras.microtesk.translator.simnml.coverage.ssa;

import ru.ispras.fortress.data.DataType;
import ru.ispras.fortress.data.Variable;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeOperation;
import ru.ispras.fortress.expression.NodeValue;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.expression.StandardOperation;
import ru.ispras.fortress.transformer.NodeTransformer;
import ru.ispras.fortress.transformer.TransformerRule;

import ru.ispras.microtesk.test.template.Argument;
import ru.ispras.microtesk.test.template.Primitive;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static ru.ispras.microtesk.translator.simnml.coverage.ssa.Expression.AND;
import static ru.ispras.microtesk.translator.simnml.coverage.ssa.Expression.EQ;
import static ru.ispras.microtesk.translator.simnml.coverage.ssa.Expression.OR;

public final class SsaAssembler {
  final Map<String, SsaForm> buildingBlocks;
  final Primitive spec;
  SsaScope scope;
  int numTemps;

  Deque<Primitive> context;
  String prefix;
  ArrayList<Node> statements;
  Deque<Integer> batchSize;
  Map<Enum<?>, TransformerRule> ruleset;

  Deque<Changes> changesStack;
  Changes changes;

  public SsaAssembler(Map<String, SsaForm> buildingBlocks, Primitive spec) {
    this.buildingBlocks = buildingBlocks;
    this.spec = spec;
    this.scope = SsaScopeFactory.createScope();
    this.numTemps = 0;

    this.prefix = "";
    this.context = null;
    this.ruleset = null;
  }

  public Node assemble(String tag) {
    this.context = new ArrayDeque<>();
    this.statements = new ArrayList<>();
    this.batchSize = new ArrayDeque<>();
    this.ruleset = createRuleset();

    this.changesStack = new ArrayDeque<>();

    final Map<String, NodeVariable> changesStore = new HashMap<>();
    this.changes = new Changes(changesStore, changesStore);

    newBatch();
    step(spec, tag, "action");
    return endBatch();
  }

  private void step(Primitive spec, String alias, String method) {
    context.push(spec);
    pushPrefix(alias);

    final SsaForm ssa =
        buildingBlocks.get(dotConc(spec.getName(), method));
    embedBlock(ssa.getEntryPoint(), spec);

    popPrefix();
    context.pop();
  }

  private void stepArgument(String name, String method) {
    step((Primitive) context.peek().getArguments().get(name).getValue(),
         name,
         method);
  }

  private void pushPrefix(String section) {
    if (this.prefix.isEmpty()) {
      this.prefix = section;
    } else {
      this.prefix = dotConc(this.prefix, section);
    }
  }

  private void popPrefix() {
    final Pair<String, String> splitted = Utility.splitOnLast(this.prefix, '.');
    if (splitted.second.isEmpty()) {
      this.prefix = "";
    } else {
      this.prefix = splitted.first;
    }
  }

  private Block embedBlock(Block block, Primitive spec) {
    walkStatements(block, spec);
    if (blockIsOp(block, SsaOperation.PHI)) {
      return block;
    }
    if (block.getChildren().size() > 1) {
      return embedBranches(block, spec);
    }
    return embedSequence(block, spec);
  }

  private Block embedSequence(Block block, Primitive spec) {
    if (block.getChildren().size() > 0) {
      changes.commit();
      return embedBlock(block.getChildren().get(0).block, spec);
    }
    return null;
  }

  private Block embedBranches(Block block, Primitive spec) {
    Block fence = null;
    final int size = block.getChildren().size();
    final List<NodeOperation> branches = new ArrayList(size);
    final Collection<Changes> containers = changes.fork(size);
    final Iterator<Changes> rebasers = containers.iterator();

    changes.commit();
    changesStack.push(changes);

    final NodeTransformer xform = new NodeTransformer(this.ruleset);
    for (GuardedBlock guard : block.getChildren()) {
      changes = rebasers.next();

      newBatch(transformNode(guard.guard, xform));
      fence = sameNotNull(fence, embedBlock(guard.block, spec));
      branches.add(endBatch());
    }
    changes = changesStack.pop();
    join(changes, block.getChildren(), containers, xform);

    addToBatch(OR(branches));

    newBatch();
    for (Map.Entry<String, Node> entry : changes.getSummary().entrySet()) {
      final Node node = entry.getValue();
      if (nodeIsOperation(node, StandardOperation.ITE)) {
        addToBatch(EQ(changes.newLatest(entry.getKey()), node));
      }
    }
    addToBatch(endBatch());
    changes.getSummary().clear();

    return embedSequence(fence, spec);
  }

  private static Node transformNode(Node node, NodeTransformer xform) {
    xform.walk(node);
    final Node result = xform.getResult().iterator().next();
    xform.reset();
    return result;
  }

  private static void join(Changes repo, Collection<GuardedBlock> blocks, Collection<Changes> containers, NodeTransformer xform) {
    final Map<String, Node> summary = repo.getSummary();
    final Iterator<GuardedBlock> block = blocks.iterator();
    for (Changes diff : containers) {
      final Node guard = transformNode(block.next().guard, xform);
      for (Map.Entry<String, Node> entry : diff.getSummary().entrySet()) {
        final Node fallback = getJointFallback(entry.getKey(), repo, diff);
        if (!fallback.equals(entry.getValue()) ||
            fallback.getUserData() != entry.getValue().getUserData()) {
          final Node ite = ITE(guard, entry.getValue(), fallback);
          repo.getSummary().put(entry.getKey(), ite);
        }
      }
    }
  }

  private static Node getJointFallback(String name, Changes master, Changes branch) {
    if (master.getSummary().containsKey(name)) {
      return master.getSummary().get(name);
    }
    final NodeVariable base = branch.getBase(name);
    if (base != null) {
      return base;
    }
    return branch.getLatest(name);
  }

  private static NodeOperation ITE(Node cond, Node lhs, Node rhs) {
    return new NodeOperation(StandardOperation.ITE, cond, lhs, rhs);
  }

  private static <T> T sameNotNull(T stored, T input) {
    if (input == null) {
      throw new NullPointerException();
    }
    if (stored != null && stored != input) {
      throw new IllegalArgumentException();
    }
    return input;
  }

  private static boolean blockIsOp(Block block, Enum<?> id) {
    final List<NodeOperation> stmts = block.getStatements();
    return stmts.size() == 1 &&
           nodeIsOperation(stmts.get(0), id);
  }

  private final class ModeRule implements TransformerRule {
    private final Enum<?> operation;
    private final String suffix;

    ModeRule(Enum<?> operation, String suffix) {
      this.operation = operation;
      this.suffix = suffix;
    }

    @Override
    public boolean isApplicable(Node node) {
      return nodeIsOperation(node, this.operation);
    }

    @Override
    public Node apply(Node node) {
      final Pair<String, String> name =
          Utility.splitOnLast(variableOperand(0, node).getName(), '.');

      stepArgument(name.second, suffix);

      return scope.fetch(String.format("__tmp_%d", numTemps - 1));
    }
  }

  private Map<Enum<?>, TransformerRule> createRuleset() {
    final TransformerRule call = new TransformerRule() {
      @Override
      public boolean isApplicable(Node node) {
        return nodeIsOperation(node, SsaOperation.CALL);
      }

      @Override
      public Node apply(Node node) {
        final Pair<String, String> pair =
            (Pair<String, String>) node.getUserData();
        stepArgument(pair.first, pair.second);

        return node;
      }
    };

    final TransformerRule substitute = new TransformerRule() {
      @Override
      public boolean isApplicable(Node node) {
        return nodeIsOperation(node, SsaOperation.SUBSTITUTE);
      }

      @Override
      public Node apply(Node node) {
        return createTemporary(variableOperand(0, node).getDataType());
      }
    };

    final Map<Enum<?>, TransformerRule> rules = new IdentityHashMap<>();
    rules.put(SsaOperation.CALL, call);
    rules.put(SsaOperation.SUBSTITUTE, substitute);
    rules.put(SsaOperation.EXPAND, new ModeRule(SsaOperation.EXPAND, "expand"));
    rules.put(SsaOperation.UPDATE, new ModeRule(SsaOperation.UPDATE, "update"));

    final TransformerRule rebase = new TransformerRule() {
      @Override
      public boolean isApplicable(Node node) {
        return node.getKind() == Node.Kind.VARIABLE &&
               node.getUserData() != null;
      }

      @Override
      public Node apply(Node node) {
        final NodeVariable var = (NodeVariable) node;
        if (var.getName().indexOf('.') >= 0) {
          return rebaseLocal(var);
        }
        return changes.rebase(var);
      }

      private Node rebaseLocal(NodeVariable node) {
        final Pair<String, String> pair =
            Utility.splitOnFirst(node.getName(), '.');

        return changes.rebase(dotConc(prefix, pair.second),
                              node.getData(),
                              (Integer) node.getUserData());
      }
    };
    rules.put(Node.Kind.VARIABLE, rebase);

    return rules;
  }

  private void walkStatements(final Block block, final Primitive spec) {
    final NodeTransformer transformer = new NodeTransformer(this.ruleset);
    transformer.walk(block.getStatements());

    // It is known that resulting sequence will be inverted on block granularity
    for (Node node : transformer.getResult()) {
      addToBatch(node);
    }
  }

  private void addToBatch(Node node) {
    if (nodeIsOperation(node, SsaOperation.CALL) ||
        nodeIsOperation(node, SsaOperation.PHI) ||
        nodeIsTrue(node)) {
      return;
    }
    this.statements.add(node);
    this.batchSize.push(this.batchSize.pop() + 1);
  }

  private void addToBatch(Collection<? extends Node> batch) {
    this.statements.addAll(batch);
    this.batchSize.push(this.batchSize.pop() + batch.size());
  }

  private boolean nodeIsTrue(Node node) {
    if (node.equals(Expression.TRUE)) {
      return true;
    }
    if (!nodeIsOperation(node, StandardOperation.AND)) {
      return false;
    }
    final NodeOperation op = (NodeOperation) node;
    return op.getOperandCount() == 1 &&
           op.getOperand(0).equals(Expression.TRUE);
  }

  private void newBatch() {
    this.batchSize.push(0);
  }

  private void newBatch(Node node) {
    newBatch();
    addToBatch(node);
  }

  private NodeOperation endBatch() {
    final List<Node> operands =
        this.statements.subList(this.statements.size() - this.batchSize.pop(),
                                this.statements.size());
    final NodeOperation batch = AND(operands);
    operands.clear();

    return batch;
  }

  private NodeVariable createTemporary(DataType type) {
    return scope.create(String.format("__tmp_%d", numTemps++),
                        type.valueUninitialized());
  }

  private static NodeVariable variableOperand(int i, Node op) {
    return (NodeVariable) ((NodeOperation) op).getOperand(i);
  }

  private static boolean nodeIsOperation(Node node, Enum<?> opId) {
    if (node.getKind() != Node.Kind.OPERATION) {
      return false;
    }
    return ((NodeOperation) node).getOperationId() == opId;
  }

  private static String dotConc(String lhs, String rhs) {
    return lhs + "." + rhs;
  }
}
