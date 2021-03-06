/*
 * Copyright 2015-2018 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.mmu.translator;

import ru.ispras.fortress.data.DataTypeId;
import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.expression.ExprUtils;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeOperation;
import ru.ispras.fortress.expression.NodeValue;
import ru.ispras.fortress.expression.Nodes;
import ru.ispras.fortress.expression.StandardOperation;
import ru.ispras.fortress.transformer.NodeTransformer;
import ru.ispras.fortress.transformer.ReduceOptions;
import ru.ispras.fortress.transformer.Reducer;
import ru.ispras.fortress.transformer.TransformerRule;
import ru.ispras.fortress.util.InvariantChecks;

import java.util.ArrayList;
import java.util.List;

final class DistanceCalculator {
  private static DistanceCalculator instance = null;

  public static DistanceCalculator get() {
    if (null == instance) {
      instance = new DistanceCalculator();
    }

    return instance;
  }

  private final NodeTransformer transformer;
  private final NodeTransformer minimizer;

  private DistanceCalculator() {
    transformer = new NodeTransformer();

    final ExpandAddRule addRule = new ExpandAddRule();
    final ReplaceSubRule subRule = new ReplaceSubRule();

    transformer.addRule(StandardOperation.ADD,   addRule);
    transformer.addRule(StandardOperation.BVADD, addRule);
    transformer.addRule(StandardOperation.SUB,   subRule);
    transformer.addRule(StandardOperation.BVSUB, subRule);

    minimizer = new NodeTransformer();
    minimizer.addRule(StandardOperation.ADD, new MinimizeAddRule());
  }

  public Node distance(final Node from, final Node to) {
    final Node expr = new NodeOperation(
        from.isType(DataTypeId.BIT_VECTOR)
            || to.isType(DataTypeId.BIT_VECTOR) ? StandardOperation.BVSUB : StandardOperation.SUB,
        to,
        from
        );

    final Node reduced = Reducer.reduce(ReduceOptions.NEW_INSTANCE, expr);
    InvariantChecks.checkNotNull(reduced);

    if (ExprUtils.isValue(reduced)) {
      return reduced;
    }

    final Node flattened = transform(transformer, reduced);
    InvariantChecks.checkNotNull(flattened);

    final Node minimized = transform(minimizer, transform(transformer, flattened));
    InvariantChecks.checkNotNull(minimized);

    return minimized;
  }

  private static Node transform(final NodeTransformer transformer, final Node expr) {
    InvariantChecks.checkNotNull(transformer);
    InvariantChecks.checkNotNull(expr);

    try {
      transformer.walk(expr);
      return transformer.getResult().iterator().next();
    } finally {
      transformer.reset();
    }
  }

  private static final class ExpandAddRule implements TransformerRule {
    @Override
    public boolean isApplicable(final Node expr) {
      if (!ExprUtils.isOperation(expr, StandardOperation.ADD)
          && !ExprUtils.isOperation(expr, StandardOperation.BVADD)) {
        return false;
      }

      final NodeOperation operation = (NodeOperation) expr;
      for (final Node operand : operation.getOperands()) {
        if (ExprUtils.isOperation(operand, StandardOperation.ADD)
            || ExprUtils.isOperation(operand, StandardOperation.BVADD)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public Node apply(final Node expr) {
      final NodeOperation operation = (NodeOperation) expr;
      final List<Node> operands = new ArrayList<>(operation.getOperandCount());

      for (final Node operand : operation.getOperands()) {
        if (ExprUtils.isOperation(operand, StandardOperation.ADD)
            || ExprUtils.isOperation(operand, StandardOperation.BVADD)) {
          operands.addAll(((NodeOperation) operand).getOperands());
        } else {
          operands.add(operand);
        }
      }

      return Nodes.add(operands);
    }
  }

  private static final class ReplaceSubRule implements TransformerRule {
    @Override
    public boolean isApplicable(final Node expr) {
      return ExprUtils.isOperation(expr, StandardOperation.SUB)
          || ExprUtils.isOperation(expr, StandardOperation.BVSUB);
    }

    @Override
    public Node apply(final Node expr) {
      final NodeOperation operation = (NodeOperation) expr;
      final List<Node> operands = new ArrayList<>(operation.getOperandCount());

      final Node first = operation.getOperand(0);
      operands.add(first);

      for (int index = 1; index < operation.getOperandCount(); ++index) {
        final Node current = operation.getOperand(index);
        if (ExprUtils.isOperation(current, StandardOperation.MINUS)) {
          InvariantChecks.checkTrue(((NodeOperation) current).getOperandCount() == 1);
          operands.add(((NodeOperation) current).getOperand(0));
        } else {
          operands.add(Nodes.minus(current));
        }
      }

      return Nodes.add(operands);
    }
  }

  private static final class MinimizeAddRule implements TransformerRule {
    @Override
    public boolean isApplicable(final Node expr) {
      if (!ExprUtils.isOperation(expr, StandardOperation.ADD)
          && !ExprUtils.isOperation(expr, StandardOperation.BVADD)) {
        return false;
      }

      final NodeOperation operation = (NodeOperation) expr;
      for (final Node operand : operation.getOperands()) {
        if (ExprUtils.isOperation(operand, StandardOperation.MINUS)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public Node apply(final Node expr) {
      final NodeOperation operation = (NodeOperation) expr;

      final List<Node> operands = new ArrayList<>(operation.getOperands());
      for (int i = 0; i < operands.size(); ++i) {
        if (null == operands.get(i)) {
          continue;
        }

        for (int j = i + 1; j < operands.size(); ++j) {
          if (null == operands.get(j)) {
            continue;
          }

          if (isNegation(operands.get(i), operands.get(j))) {
            operands.set(i, null);
            operands.set(j, null);
            break;
          }
        }
      }

      final List<Node> newOperands = new ArrayList<>(operands.size());
      for (final Node operand : operands) {
        if (null != operand) {
          newOperands.add(operand);
        }
      }

      if (operands.size() == newOperands.size()) {
        return expr;
      }

      if (0 == newOperands.size()) {
        return expr.isType(DataTypeId.BIT_VECTOR)
            ? NodeValue.newBitVector(BitVector.newEmpty(expr.getDataType().getSize()))
            : NodeValue.newInteger(0);
      }

      if (1 == newOperands.size()) {
        return newOperands.get(0);
      }

      return Nodes.add(newOperands);
    }

    private boolean isNegation(final Node node1, final Node node2) {
      if (ExprUtils.isOperation(node1, StandardOperation.MINUS)
          && !ExprUtils.isOperation(node2, StandardOperation.MINUS)) {
        return node2.equals(((NodeOperation) node1).getOperand(0));
      }

      if (ExprUtils.isOperation(node2, StandardOperation.MINUS)
          && !ExprUtils.isOperation(node1, StandardOperation.MINUS)) {
        return node1.equals(((NodeOperation) node2).getOperand(0));
      }

      return false;
    }
  }
}
