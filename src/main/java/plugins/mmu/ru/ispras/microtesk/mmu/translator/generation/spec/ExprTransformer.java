/*
 * Copyright 2015 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.mmu.translator.generation.spec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.ispras.fortress.expression.ExprUtils.isOperation;
import static ru.ispras.fortress.expression.ExprUtils.isValue;

import ru.ispras.fortress.data.DataTypeId;
import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeOperation;
import ru.ispras.fortress.expression.NodeValue;
import ru.ispras.fortress.expression.StandardOperation;
import ru.ispras.fortress.transformer.NodeTransformer;
import ru.ispras.fortress.transformer.TransformerRule;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.utils.FortressUtils;

/**
 * Class {@link ExprTransformer} transforms operations that involve shifts and 
 * bit masks into concatenation-based expressions. The shift amount and the bitmask
 * value must be constants. In addition, the transformer helps get rid of nested 
 * bit field extraction operations ({@link StandardOperation#BVEXTRACT}) by replacing
 * them with a single bit field extraction. Supported operations include:
 * <ol>
 * <li>{@link StandardOperation#BVZEROEXT}
 * <li>{@link StandardOperation#BVLSHL}
 * <li>{@link StandardOperation#BVASHL}
 * <li>{@link StandardOperation#BVLSHR}
 * <li>{@link StandardOperation#BVAND}
 * <li>{@link StandardOperation#BVOR}
 * </ol>
 * 
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class ExprTransformer {
  private final NodeTransformer transformer;

  public ExprTransformer() {
    this.transformer = new NodeTransformer();
    this.transformer.addRule(StandardOperation.BVZEROEXT, new ZeroExtendRule());
    this.transformer.addRule(StandardOperation.BVEXTRACT, new NestedFieldRule());

    final LeftShiftRule leftShiftRule = new LeftShiftRule();
    this.transformer.addRule(StandardOperation.BVLSHL, leftShiftRule);
    this.transformer.addRule(StandardOperation.BVASHL, leftShiftRule);

    this.transformer.addRule(StandardOperation.BVLSHR, new RightShiftRule());

    final BitMaskRule bitMaskRule = new BitMaskRule();
    this.transformer.addRule(StandardOperation.BVAND, bitMaskRule);
    this.transformer.addRule(StandardOperation.BVOR, bitMaskRule);
  }

  public Node transform(final Node expr) {
    InvariantChecks.checkNotNull(expr);

    // Replaces shifts and bitmasks with concatenation
    transformer.walk(expr);
    final Node result = transformer.getResult().iterator().next();
    transformer.reset();

    return result;
  }

  private static final class ZeroExtendRule implements TransformerRule {
    @Override
    public boolean isApplicable(final Node expr) {
      return isOperation(expr, StandardOperation.BVZEROEXT);
    }

    @Override
    public Node apply(final Node expr) {
      final NodeOperation op = (NodeOperation) expr;

      final Node sizeExpr = op.getOperand(0);
      final Node argExpr = op.getOperand(1);

      // BVZEROEXT specifies the amount by which the argument must be extended.
      // In other words, it is a delta size, not the result size.

      final int deltaSize = FortressUtils.extractInt(sizeExpr);
      InvariantChecks.checkGreaterThanZero(deltaSize);

      return newConcat(
          NodeValue.newBitVector(BitVector.newEmpty(deltaSize)),
          argExpr
          );
    }
  }

  private static final class LeftShiftRule implements TransformerRule {
    @Override
    public boolean isApplicable(final Node expr) {
      if (!isOperation(expr, StandardOperation.BVLSHL) &&
          !isOperation(expr, StandardOperation.BVASHL)) {
        return false;
      }

      final Node shiftAmount = ((NodeOperation) expr).getOperand(1);
      return shiftAmount.getKind() == Node.Kind.VALUE;
    }

    @Override
    public Node apply(final Node expr) {
      final NodeOperation op = (NodeOperation) expr;

      final Node argExpr = op.getOperand(0);
      final Node shiftAmountExpr = op.getOperand(1);

      final int argSize = argExpr.getDataType().getSize();
      final int shiftAmount = FortressUtils.extractInt(shiftAmountExpr) % argSize;

      if (shiftAmount == 0) {
        return argExpr;
      }

      final Node fieldExpr =
          newField(argExpr, 0, argSize - 1 - shiftAmount);

      return newConcat(
          fieldExpr,
          NodeValue.newBitVector(BitVector.newEmpty(shiftAmount))
          );
    }
  }

  private static final class RightShiftRule implements TransformerRule {
    @Override
    public boolean isApplicable(final Node expr) {
      if (!isOperation(expr, StandardOperation.BVLSHR)) {
        return false;
      }

      final Node shiftAmount = ((NodeOperation) expr).getOperand(1);
      return shiftAmount.getKind() == Node.Kind.VALUE;
    }

    @Override
    public Node apply(final Node expr) {
      final NodeOperation op = (NodeOperation) expr;

      final Node argExpr = op.getOperand(0);
      final Node shiftAmountExpr = op.getOperand(1);

      final int argSize = argExpr.getDataType().getSize();
      final int shiftAmount = FortressUtils.extractInt(shiftAmountExpr) % argSize;

      if (shiftAmount == 0) {
        return argExpr;
      }

      final Node fieldExpr =
          newField(argExpr, shiftAmount, argSize - 1);

      return newConcat(
          NodeValue.newBitVector(BitVector.newEmpty(shiftAmount)),
          fieldExpr
          );
    }
  }

  private static final class NestedFieldRule implements TransformerRule {
    @Override
    public boolean isApplicable(final Node expr) {
      if (!isOperation(expr, StandardOperation.BVEXTRACT)) {
        return false;
      }

      final NodeOperation op = (NodeOperation) expr;
      if (op.getOperandCount() < 3) {
        return false;
      }

      final Node nested = op.getOperand(2);
      return isOperation(nested, StandardOperation.BVEXTRACT) ||
             isOperation(nested, StandardOperation.BVCONCAT);
    }

    @Override
    public Node apply(final Node expr) {
      final NodeOperation op = (NodeOperation) expr;
      final NodeOperation nestedOp = (NodeOperation) op.getOperand(2);

      final int from = FortressUtils.extractInt(op.getOperand(0));
      final int to = FortressUtils.extractInt(op.getOperand(1));

      final int realFrom = Math.min(from, to);
      final int realTo = Math.max(from, to);

      return newField(nestedOp, realFrom, realTo); 
    }
  }

  private static final class BitMaskRule implements TransformerRule {
    @Override
    public boolean isApplicable(final Node expr) {
      if (!isOperation(expr, StandardOperation.BVOR) && 
          !isOperation(expr, StandardOperation.BVAND)) {
        return false;
      }

      final NodeOperation op = (NodeOperation) expr;
      if (op.getOperandCount() < 2) {
        return false;
      }

      final Node operand1 = op.getOperand(0);
      final Node operand2 = op.getOperand(1);

      return ((isValue(operand1) && !isValue(operand2)) ||
              (isValue(operand2) && !isValue(operand1)));
    }

    @Override
    public Node apply(final Node expr) {
      final NodeOperation op = (NodeOperation) expr;
      final Node operand1 = op.getOperand(0);
      final Node operand2 = op.getOperand(1);

      final NodeValue mask;
      final Node operand;

      if (isValue(operand1) && !isValue(operand2)) {
        mask = (NodeValue) operand1;
        operand = operand2;
      } else if (isValue(operand2) && !isValue(operand1)) {
        mask = (NodeValue) operand2;
        operand = operand1;
      } else {
        throw new IllegalArgumentException();
      }

      InvariantChecks.checkTrue(mask.isType(DataTypeId.BIT_VECTOR));
      InvariantChecks.checkTrue(operand.isType(DataTypeId.BIT_VECTOR));

      InvariantChecks.checkTrue(
          mask.getDataType().getSize() ==
          operand.getDataType().getSize());

      final BitVector maskValue = mask.getData().getBitVector();
      if (op.getOperationId() == StandardOperation.BVAND) {
        return applyAndMask(operand, maskValue);
      } else if (op.getOperationId() == StandardOperation.BVOR) {
        return applyOrMask(operand, maskValue);
      } else {
        throw new IllegalArgumentException();
      }
    }

    private static Node applyAndMask(final Node expr, final BitVector mask) {
      int fieldEnds = mask.getBitSize() - 1;
      boolean isFieldBitSet = mask.getBit(fieldEnds);

      final List<Node> fields = new ArrayList<>();
      for (int bitIndex = mask.getBitSize() - 2; bitIndex >= 0; bitIndex--) {
        final boolean isBitSet = mask.getBit(bitIndex);

        if (isBitSet == isFieldBitSet) {
          continue;
        }

        if (isBitSet) {
          fields.add(NodeValue.newBitVector(BitVector.newEmpty(fieldEnds - bitIndex)));
        } else {
          fields.add(newField(expr, bitIndex + 1, fieldEnds));
        }

        fieldEnds = bitIndex;
        isFieldBitSet = isBitSet;
      }

      if (isFieldBitSet) {
        fields.add(newField(expr, 0, fieldEnds));
      } else {
        fields.add(NodeValue.newBitVector(BitVector.newEmpty(fieldEnds + 1)));
      }

      return newConcat(fields);
    }

    private static Node applyOrMask(final Node expr, final BitVector mask) {
      int fieldEnds = mask.getBitSize() - 1;
      boolean isFieldBitSet = mask.getBit(fieldEnds);

      final List<Node> fields = new ArrayList<>();
      for (int bitIndex = mask.getBitSize() - 2; bitIndex >= 0; bitIndex--) {
        final boolean isBitSet = mask.getBit(bitIndex);

        if (isBitSet == isFieldBitSet) {
          continue;
        }

        if (isBitSet) {
          fields.add(newField(expr, bitIndex + 1, fieldEnds));
        } else {
          final BitVector value = BitVector.newEmpty(fieldEnds - bitIndex);
          value.setAll();
          fields.add(NodeValue.newBitVector(value));
        }

        fieldEnds = bitIndex;
        isFieldBitSet = isBitSet;
      }

      if (isFieldBitSet) {
        final BitVector value = BitVector.newEmpty(fieldEnds + 1);
        value.setAll();
        fields.add(NodeValue.newBitVector(value));
      } else {
        fields.add(newField(expr, 0, fieldEnds));
      }

      return newConcat(fields);
    }
  }

  private static Node newField(final Node expr, final int from, final int to) {
    InvariantChecks.checkNotNull(expr);
    InvariantChecks.checkTrue(expr.isType(DataTypeId.BIT_VECTOR));

    final int bitSize = expr.getDataType().getSize();
    InvariantChecks.checkBounds(from, bitSize);
    InvariantChecks.checkBounds(to, bitSize);
    InvariantChecks.checkTrue(from <= to);

    if (from == 0 && to == bitSize - 1) {
      return expr;
    }

    if (isValue(expr)) {
      final BitVector value = ((NodeValue) expr).getBitVector();
      return NodeValue.newBitVector(value.field(from, to));
    }

    if (isOperation(expr, StandardOperation.BVEXTRACT)) {
      return newNestedField((NodeOperation) expr, from, to);
    }

    if (isOperation(expr, StandardOperation.BVCONCAT)) {
      return newFieldForConcat((NodeOperation) expr, from, to);
    }

    return new NodeOperation(
        StandardOperation.BVEXTRACT,
        NodeValue.newInteger(to),
        NodeValue.newInteger(from),
        expr
        );
  }

  private static Node newNestedField(final NodeOperation op, final int from, final int to) {
    InvariantChecks.checkNotNull(op);
    InvariantChecks.checkTrue(op.isType(DataTypeId.BIT_VECTOR));

    InvariantChecks.checkTrue(isOperation(op, StandardOperation.BVEXTRACT));
    InvariantChecks.checkTrue(op.getOperandCount() == 3);

    final int bitSize = op.getDataType().getSize();
    InvariantChecks.checkBounds(from, bitSize);
    InvariantChecks.checkBounds(to, bitSize);
    InvariantChecks.checkTrue(from <= to);

    if (from == 0 && to == bitSize - 1) {
      return op;
    }

    final int innerFrom = FortressUtils.extractInt(op.getOperand(0));
    final int innerTo = FortressUtils.extractInt(op.getOperand(1));
    final Node operand = op.getOperand(2);

    final int realInnerFrom = Math.min(innerFrom, innerTo);
    final int realInnerTo = Math.max(innerFrom, innerTo);

    final int newFrom = realInnerFrom + from;
    final int newTo = realInnerFrom + to;

    InvariantChecks.checkTrue(realInnerFrom <= newFrom && newFrom <= realInnerTo);
    InvariantChecks.checkTrue(realInnerFrom <= newTo && newTo <= realInnerTo);

    return newField(operand, newFrom, newTo);
  }

  private static Node newFieldForConcat(final NodeOperation op, final int from, final int to) {
    InvariantChecks.checkNotNull(op);
    InvariantChecks.checkTrue(op.isType(DataTypeId.BIT_VECTOR));
    InvariantChecks.checkTrue(isOperation(op, StandardOperation.BVCONCAT));

    final int bitSize = op.getDataType().getSize();
    InvariantChecks.checkBounds(from, bitSize);
    InvariantChecks.checkBounds(to, bitSize);
    InvariantChecks.checkTrue(from <= to);

    if (from == 0 && to == bitSize - 1) {
      return op;
    }

    final List<Node> fields = new ArrayList<>();
    int operandTo = bitSize - 1;

    for (final Node operand : op.getOperands()) {
      InvariantChecks.checkTrue(operand.isType(DataTypeId.BIT_VECTOR));

      final int operandBitSize = operand.getDataType().getSize();
      final int operandFrom = operandTo - operandBitSize + 1;

      final int fieldFrom = from >= operandFrom ? from - operandFrom : 0;
      final int fieldTo = to <= operandTo ? to - operandFrom : operandBitSize - 1;

      if (0 <= fieldFrom && fieldTo < operandBitSize) {
        fields.add(newField(operand, fieldFrom, fieldTo));
      } 

      operandTo = operandFrom - 1; 
    }

    return newConcat(fields);
  }

  private static Node newConcat(final Node... fields) {
    return newConcat(Arrays.asList(fields));
  }

  private static Node newConcat(final List<? extends Node> fields) {
    InvariantChecks.checkNotEmpty(fields);

    if (fields.size() == 1) {
      return fields.get(0);
    }

    final List<Node> newFields = new ArrayList<>(fields.size());
    for (final Node field : fields) {
      if (isOperation(field, StandardOperation.BVCONCAT)) {
        newFields.addAll(((NodeOperation) field).getOperands());
      } else {
        newFields.add(field);
      }
    }

    return new NodeOperation(StandardOperation.BVCONCAT, newFields);
  }
}
