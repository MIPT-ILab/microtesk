/*
 * Copyright 2014-2015 ISP RAS (http://www.ispras.ru)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package ru.ispras.microtesk.translator.nml.coverage;

import static ru.ispras.microtesk.translator.nml.coverage.Utility.dotConc;
import static ru.ispras.microtesk.translator.nml.coverage.Utility.literalOperand;
import static ru.ispras.microtesk.translator.nml.coverage.Utility.variableOperand;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ru.ispras.fortress.data.Data;
import ru.ispras.fortress.data.DataType;
import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.expression.ExprUtils;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeOperation;
import ru.ispras.fortress.expression.NodeValue;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.expression.Nodes;
import ru.ispras.fortress.expression.StandardOperation;
import ru.ispras.fortress.transformer.NodeTransformer;
import ru.ispras.fortress.transformer.Transformer;
import ru.ispras.fortress.transformer.TransformerRule;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.fortress.util.Pair;
import ru.ispras.microtesk.utils.StringUtils;

final class Prefix {
  public final String context;
  public final String expression;

  public Prefix(final String context, final String expression) {
    this.context = context;
    this.expression = expression;
  }

  public Prefix pushAll(final String suffix) {
    return new Prefix(dotConc(context, suffix), dotConc(expression, suffix));
  }

  public Prefix popAll() {
    final String context = popPrefix(this.context);
    final String expression = popPrefix(this.expression);

    return new Prefix(context, expression);
  }

  public static String popPrefix(final String prefix) {
    final Pair<String, String> splitted = StringUtils.splitOnLast(prefix, '.');
    if (splitted.second.isEmpty()) {
      return "";
    }
    return splitted.first;
  }
}

final class Parameters {
  private final NodeOperation node;

  public Parameters(final Node node) {
    InvariantChecks.checkTrue(ExprUtils.isOperation(node, SsaOperation.PARAMETERS));
    this.node = (NodeOperation) node;
  }

  public String getName(final int i) {
    return literalOperand(i, node);
  }

  final DataType getType(final int i) {
    return node.getOperand(i).getDataType();
  }

  final NodeVariable find(final String name) {
    for (final Node p : node.getOperands()) {
      final NodeVariable v = (NodeVariable) p;
      if (v.getName().equals(name)) {
        return v;
      }
    }
    return null;
  }
}

final class Closure {
  private final NodeOperation node;

  public Closure(final Node node) {
    InvariantChecks.checkTrue(ExprUtils.isOperation(node, SsaOperation.CLOSURE));
    this.node = (NodeOperation) node;
  }

  public Node getOriginRef() {
    return node.getOperand(0);
  }

  public String getOriginName() {
    return literalOperand(0, node);
  }

  public List<Node> getArguments() {
    return node.getOperands().subList(1, node.getOperandCount());
  }

  public Node getArgument(final int i) {
    return node.getOperand(i + 1);
  }
}

public final class SsaAssembler {
  private final Map<String, SsaForm> buildingBlocks;
  private Map<String, Object> buildingContext;

  private SsaScope scope;
  private int numTemps;

  private Map<String, Integer> contextEnum;
  private List<Node> statements;
  private Deque<Integer> batchSize;

  private Deque<Changes> changesStack;
  private Changes changes;

  public SsaAssembler(Map<String, SsaForm> buildingBlocks) {
    this.buildingBlocks = buildingBlocks;

    this.scope = new SsaScopeVariable();
    this.numTemps = 0;

    final Map<String, NodeVariable> changesStore = new HashMap<>();
    this.changes = new Changes(changesStore, changesStore);
  }

  public Node assemble(final Map<String, Object> context, final String entry) {
    return assemble(context, entry, entry);
  }

  public Node assemble(final Map<String, Object> context, final String entry, final String tag) {
    this.buildingContext = new HashMap<>(context);
    this.contextEnum = new HashMap<>();
    this.statements = new ArrayList<>();
    this.batchSize = new ArrayDeque<>();

    this.changesStack = new ArrayDeque<>();

    newBatch();
    step(new Prefix(entry, tag), "action");

    return endBatch();
  }

  private void step(final Prefix prefix, final String method) {
    final String name = (String) buildingContext.get(prefix.context);
    final SsaForm ssa = buildingBlocks.get(dotConc(name, method));
    embedBlock(prefix, ssa.getEntryPoint());
  }

  private void stepArgument(final Prefix prefix, final String name, final String method) {
    step(prefix.pushAll(name), method);
  }

  private Block embedBlock(final Prefix prefix, final Block block) {
    InvariantChecks.checkNotNull(block);

    walkStatements(prefix, block.getStatements());
    if (blockIsOp(block, SsaOperation.PHI)) {
      return block;
    }
    if (block.getChildren().size() > 1) {
      return embedBranches(prefix, block);
    }
    return embedSequence(prefix, block);
  }

  private Block embedSequence(final Prefix prefix, final Block block) {
    InvariantChecks.checkNotNull(block);

    if (block.getChildren().size() > 0) {
      changes.commit();
      return embedBlock(prefix, block.getChildren().get(0).block);
    }
    return null;
  }

  private Block embedBranches(final Prefix prefix, final Block block) {
    InvariantChecks.checkNotNull(block);
    changes.commit();

    Block fence = null;
    final int size = block.getChildren().size();
    final List<NodeOperation> branches = new ArrayList<>(size);
    final Collection<Changes> containers = changes.fork(size);
    final Iterator<Changes> rebasers = containers.iterator();

    changesStack.push(changes);

    final NodeTransformer xform = new NodeTransformer(createRuleset(prefix));
    for (GuardedBlock guard : block.getChildren()) {
      changes = rebasers.next();

      newBatch(Transformer.transform(guard.guard, xform));
      fence = sameNotNull(fence, embedBlock(prefix, guard.block));
      branches.add(endBatch());
    }
    changes = changesStack.pop();
    join(changes, block.getChildren(), containers, xform);

    addToBatch(Nodes.or(branches));

    newBatch();
    for (Map.Entry<String, Node> entry : changes.getSummary().entrySet()) {
      final Node node = entry.getValue();
      if (ExprUtils.isOperation(node, StandardOperation.ITE)) {
        addToBatch(Nodes.eq(changes.newLatest(entry.getKey()), node));
      }
    }
    addToBatch(endBatch());
    changes.getSummary().clear();

    return embedSequence(prefix, fence);
  }

  private static void join(Changes repo, Collection<GuardedBlock> blocks, Collection<Changes> containers, NodeTransformer xform) {
    final Iterator<GuardedBlock> block = blocks.iterator();
    for (Changes diff : containers) {
      final Node guard = Transformer.transform(block.next().guard, xform);
      for (Map.Entry<String, Node> entry : diff.getSummary().entrySet()) {
        final Node fallback = getJointFallback(entry.getKey(), repo, diff);
        if (!fallback.equals(entry.getValue()) ||
            fallback.getUserData() != entry.getValue().getUserData()) {
          final Node ite = Nodes.ite(guard, entry.getValue(), fallback);
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

  private static <T> T sameNotNull(T stored, T input) {
    InvariantChecks.checkNotNull(input);
    InvariantChecks.checkTrue(stored == null || stored == input);

    return input;
  }

  private static boolean blockIsOp(Block block, Enum<?> id) {
    final List<NodeOperation> stmts = block.getStatements();
    return stmts.size() == 1 &&
           ExprUtils.isOperation(stmts.get(0), id);
  }

  private final class ModeRule implements TransformerRule {
    private final Enum<?> operation;
    private final Prefix prefix;
    private final String suffix;

    ModeRule(final Enum<?> operation, final Prefix prefix, final String suffix) {
      this.operation = operation;
      this.prefix = prefix;
      this.suffix = suffix;
    }

    @Override
    public boolean isApplicable(Node node) {
      return ExprUtils.isOperation(node, this.operation);
    }

    @Override
    public Node apply(Node node) {
      final Pair<String, String> pair =
          StringUtils.splitOnLast(variableOperand(0, node).getName(), '.');

      return linkMacro(prefix,
                       pair.second,
                       suffix,
                       (this.operation == SsaOperation.UPDATE) ? 2 : 1);
    }
  }

  private NodeVariable linkMacro(final Prefix prefix, final String name, final String method, final int version) {
    stepArgument(prefix, name, method);

    final NodeVariable tmp =
        scope.fetch(String.format("__tmp_%d", numTemps - 1));

    final String path = dotConc(prefix.expression, name);
    final NodeVariable var = changes.rebase(path, tmp.getData(), version);
    addToBatch(Nodes.eq(var, tmp));

    return tmp;
  }

  private void linkClosure(final Prefix origin,
                           final Prefix current,
                           final Closure closure) {
    final Parameters parameters = getParameters(closure.getOriginName());

    buildingContext.put(current.context, closure.getOriginName());
    for (int i = 0; i < closure.getArguments().size(); ++i) {
      final Node operand = closure.getArgument(i);
      final Prefix inner = current.pushAll(parameters.getName(i));

      if (ExprUtils.isOperation(operand, SsaOperation.CLOSURE)) {
        final Closure arg = new Closure(operand);
        buildingContext.put(inner.context, arg.getOriginName());
        linkClosure(origin, inner, arg);
      } else if (ExprUtils.isOperation(operand, SsaOperation.ARGUMENT_LINK)) {
        final String argName = literalOperand(0, operand);
        final Prefix srcPrefix = origin.pushAll(argName);

        final Map<String, Object> extension = new HashMap<>();
        for (final Map.Entry<String, Object> entry : buildingContext.entrySet()) {
          final Pair<String, String> pair =
              StringUtils.splitOnLast(entry.getKey(), '.');
          if (pair.first.equals(srcPrefix.context)) {
            final Prefix argPrefix = inner.pushAll(pair.second);
            extension.put(argPrefix.context, (String) entry.getValue());
            if (entry.getValue().equals("#IMM")) {
              linkArgument(argPrefix.expression,
                           srcPrefix,
                           pair.second);
            }
          }
        }
        extension.put(inner.context, buildingContext.get(srcPrefix.context));
        buildingContext.putAll(extension);

        linkArgument(inner.expression, origin, argName);
      } else {
        final NodeVariable arg =
            changes.rebase(inner.expression, //.substring(origin.expression.length()),
                           parameters.getType(i).valueUninitialized(),
                           1);
            addToBatch(Nodes.eq(arg, operand));
//        walkStatements(origin, Collections.singleton(EQ(arg, operand)));
      }
    }
  }

  private void linkArgument(final String targetName,
                            final Prefix prefix,
                            final String localName) {
    final String context = (String) buildingContext.get(prefix.context);
    final NodeVariable image = getParameters(context).find(localName);
    final Data data = image.getDataType().valueUninitialized();

    final NodeVariable target = changes.rebase(targetName, data, 1);

    final String sourceName = dotConc(prefix.expression, localName);
    final NodeVariable source = changes.rebase(sourceName, data, 1);

    addToBatch(Nodes.eq(target, source));
  }

  private Parameters getParameters(final String callee) {
    final SsaForm ssa = buildingBlocks.get(dotConc(callee, "parameters"));
    return new Parameters(ssa.getEntryPoint().getStatements().get(0));
  }

  private static boolean isArgumentCall(final Node node) {
    final NodeOperation op = (NodeOperation) node;
    return op.getOperand(0).getKind() == Node.Kind.VARIABLE &&
           op.getOperand(1).getKind() == Node.Kind.VARIABLE;
  }

  private static Pair<String, String> getArgumentCall(final Node node) {
    return new Pair<>(variableOperand(0, node).getName(),
                      variableOperand(1, node).getName());
  }

  private Map<Enum<?>, TransformerRule> createRuleset(final Prefix prefix) {
    final TransformerRule call = new TransformerRule() {
      @Override
      public boolean isApplicable(Node node) {
        return ExprUtils.isOperation(node, SsaOperation.CALL);
      }

      @Override
      public Node apply(Node node) {
        if (isArgumentCall(node)) {
          final Pair<String, String> pair = getArgumentCall(node);
          stepArgument(prefix, pair.first, pair.second);
        } else {
          final NodeOperation call = (NodeOperation) node;
          final NodeOperation instance = (NodeOperation) call.getOperand(0);

          final String callee = literalOperand(0, instance);
          final String ctxKey = dotConc(prefix.context, callee);
          Integer num = contextEnum.get(ctxKey);
          if (num == null ) {
            contextEnum.put(ctxKey, 1);
            num = 0;
          } else {
            contextEnum.put(ctxKey, num + 1);
          }
          final Prefix inner =
              prefix.pushAll(String.format("%s_%d", callee, num));

          linkClosure(prefix, inner, new Closure(instance));
          step(inner, literalOperand(1, call));
        }
        // Prune custom SSA operation
        return Nodes.TRUE;
      }
    };

    final TransformerRule thisCall = new TransformerRule() {
      @Override
      public boolean isApplicable(Node node) {
        return ExprUtils.isOperation(node, SsaOperation.THIS_CALL);
      }

      @Override
      public Node apply(Node node) {
        step(prefix, literalOperand(0, node));
        // Prune custom SSA operation
        return Nodes.TRUE;
      }
    };

    final TransformerRule substitute = new TransformerRule() {
      @Override
      public boolean isApplicable(Node node) {
        return ExprUtils.isOperation(node, SsaOperation.SUBSTITUTE);
      }

      @Override
      public Node apply(Node node) {
        return createTemporary(variableOperand(0, node).getDataType());
      }
    };

    final Map<Enum<?>, TransformerRule> rules = new IdentityHashMap<>();
    rules.put(SsaOperation.CALL, call);
    rules.put(SsaOperation.THIS_CALL, thisCall);
    rules.put(SsaOperation.SUBSTITUTE, substitute);

    rules.put(SsaOperation.EXPAND,
              new ModeRule(SsaOperation.EXPAND, prefix, "expand"));

    rules.put(SsaOperation.UPDATE,
              new ModeRule(SsaOperation.UPDATE, prefix, "update"));

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
            StringUtils.splitOnFirst(node.getName(), '.');

        return changes.rebase(dotConc(prefix.expression, pair.second),
                              node.getData(),
                              (Integer) node.getUserData());
      }
    };
    rules.put(Node.Kind.VARIABLE, rebase);

    final TransformerRule rotate = new TransformerRule() {
      @Override
      public boolean isApplicable(final Node node) {
        return ExprUtils.isOperation(node, StandardOperation.BVROR);
      }

      @Override
      public Node apply(final Node node) {
        final NodeOperation rotate = (NodeOperation) node;
        final List<Node> operands = rotate.getOperands();

        final Node amount = operands.get(1);
        final Node origin = operands.get(0);

        if (ExprUtils.isValue(amount) && amount.isType(DataType.INTEGER)) {
          // reverse argument order
          return new NodeOperation(rotate.getOperationId(), amount, origin);
        }
        // mask = 2^n - 1
        // --> (bvsub (bvshl 1 n) 1)
        //
        // x >>> n
        // --> (x >> n) | ((x & mask) << (size(x) - n)
        // --> (bvor (bvshr x n) (bvshl (bvand x mask) (bvsub size(x) n)))
        final int bitsize = origin.getDataType().getSize();
        final Node one = NodeValue.newBitVector(BitVector.valueOf(1, bitsize));
        final Node n = castBitVector(amount, origin);
        final Node size = NodeValue.newBitVector(BitVector.valueOf(bitsize, bitsize));

        final Node sizeMinusN = Nodes.bvsub(size, n);
        final Node pow2n = Nodes.bvlshl(one, n);
        final Node mask = Nodes.bvsub(pow2n, one);
        final Node shrX = Nodes.bvlshr(origin, n);
        final Node maskX = Nodes.bvand(origin, mask);
        final Node shlMasked = Nodes.bvlshl(maskX, sizeMinusN);

        return Nodes.bvor(shrX, shlMasked);
      }
    };

    rules.put(StandardOperation.BVROR, rotate);
    return rules;
  }

  private static Node castBitVector(final Node src, final Node dst) {
    final int srcSize = src.getDataType().getSize();
    final int dstSize = dst.getDataType().getSize();

    if (srcSize < dstSize) {
      return Nodes.bvzeroext(dstSize - srcSize, src);
    }

    if (srcSize > dstSize) {
      return Nodes.bvextract(dstSize - 1, 0, src);
    }

    return src;
  }

  private void walkStatements(final Prefix prefix, final Collection<? extends Node> statements) {
    final NodeTransformer transformer =
        new NodeTransformer(createRuleset(prefix));
    transformer.walk(statements);

    // It is known that resulting sequence will be inverted on block granularity
    for (Node node : transformer.getResult()) {
      addToBatch(node);
    }
  }

  private void addToBatch(Node node) {
    if (ExprUtils.isOperation(node, SsaOperation.PHI) ||
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
    if (node.equals(Nodes.TRUE)) {
      return true;
    }
    if (!ExprUtils.isOperation(node, StandardOperation.AND)) {
      return false;
    }
    final NodeOperation op = (NodeOperation) node;
    return op.getOperandCount() == 1 &&
           op.getOperand(0).equals(Nodes.TRUE);
  }

  private void newBatch() {
    this.batchSize.push(0);
  }

  private void newBatch(final Node node) {
    newBatch();
    addToBatch(node);
  }

  private NodeOperation endBatch() {
    final List<Node> operands =
        this.statements.subList(this.statements.size() - this.batchSize.pop(),
                                this.statements.size());

    if (operands.isEmpty()) {
      return Nodes.and(Nodes.TRUE);
    }
    final NodeOperation batch = Nodes.and(operands);
    operands.clear();

    return batch;
  }

  private NodeVariable createTemporary(DataType type) {
    return scope.create(String.format("__tmp_%d", numTemps++),
                        type.valueUninitialized());
  }
}
