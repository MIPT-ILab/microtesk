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

package ru.ispras.microtesk.utils;

import java.math.BigInteger;

import ru.ispras.fortress.data.Data;
import ru.ispras.fortress.data.DataTypeId;
import ru.ispras.fortress.data.Variable;
import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeOperation;
import ru.ispras.fortress.expression.NodeValue;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.expression.StandardOperation;
import ru.ispras.fortress.transformer.Reducer;
import ru.ispras.fortress.transformer.ValueProvider;
import ru.ispras.fortress.util.InvariantChecks;

public final class FortressUtils {
  private FortressUtils() {}

  public static BigInteger getInteger(final Data data) {
    return data.getType().getTypeId() == DataTypeId.BIT_VECTOR
        ? data.getBitVector().bigIntegerValue(false)
        : data.getInteger();
  }

  public static BigInteger getInteger(final Node expr) {
    checkConstantValue(expr);

    final NodeValue value = (NodeValue) expr;
    return value.getDataTypeId() == DataTypeId.BIT_VECTOR
        ? value.getBitVector().bigIntegerValue(false)
        : value.getInteger();
  }

  public static int extractInt(final Node expr) {
    checkConstantValue(expr);
    final NodeValue value = (NodeValue) expr;

    switch (value.getDataTypeId()) {
      case LOGIC_INTEGER:
        return value.getInteger().intValue();

      case BIT_VECTOR:
        return value.getBitVector().intValue();

      default:
        throw new IllegalStateException(String.format("%s cannot be converted to int", value));
    }
  }

  public static BitVector extractBitVector(final Node expr) {
    checkConstantValue(expr);
    return ((NodeValue) expr).getBitVector();
  }

  private static void checkConstantValue(final Node expr) {
    InvariantChecks.checkNotNull(expr);
    if (expr.getKind() != Node.Kind.VALUE) {
      throw new IllegalStateException(String.format("%s is not a constant value.", expr));
    }
  }

  public static Variable getVariable(final Node node) {
    switch (node.getKind()) {
      case VARIABLE:
        final NodeVariable variable = (NodeVariable) node;
        return variable.getVariable();
      case OPERATION:
        final NodeOperation operation = (NodeOperation) node;
        InvariantChecks.checkTrue(operation.getOperationId() == StandardOperation.BVEXTRACT);

        return getVariable(operation.getOperand(2));
      default:
        return null;
    }
  }

  public static int getLowerBit(final Node node) {
    switch (node.getKind()) {
      case VALUE:
        return 0;
      case VARIABLE:
        return 0;
      case OPERATION:
        final NodeOperation operation = (NodeOperation) node;
        InvariantChecks.checkTrue(operation.getOperationId() == StandardOperation.BVEXTRACT);

        final NodeValue lowerBitValue = (NodeValue) operation.getOperand(1);
        return lowerBitValue.getInteger().intValue();
      default:
        InvariantChecks.checkTrue(false);
        return -1;
    }
  }

  public static int getUpperBit(final Node node) {
    switch (node.getKind()) {
      case VALUE:
        final NodeValue value = (NodeValue) node;
        return value.getDataType().getSize() - 1;
      case VARIABLE:
        final NodeVariable variable = (NodeVariable) node;
        return variable.getVariable().getType().getSize() - 1;
      case OPERATION:
        final NodeOperation operation = (NodeOperation) node;
        InvariantChecks.checkTrue(operation.getOperationId() == StandardOperation.BVEXTRACT);

        final NodeValue upperBitValue = (NodeValue) operation.getOperand(0);
        return upperBitValue.getInteger().intValue();
      default:
        InvariantChecks.checkTrue(false);
        return -1;
    }
  }

  public static int getBitSize(final Node node) {
    return (getUpperBit(node) - getLowerBit(node)) + 1;
  }

  public static BigInteger evaluateInteger(final Node node, final ValueProvider valueProvider) {
    final Node result = Reducer.reduce(valueProvider, node);
    InvariantChecks.checkNotNull(result);

    return result.getKind() == Node.Kind.VALUE ? getInteger(result) : null;
  }

  public static BigInteger evaluateInteger(final Node node) {
    final Node result = Reducer.reduce(node);
    InvariantChecks.checkNotNull(result);

    return result.getKind() == Node.Kind.VALUE ? getInteger(result) : null;
  }

  public static BitVector evaluateBitVector(final Node node, final ValueProvider valueProvider) {
    final Node result = Reducer.reduce(valueProvider, node);
    InvariantChecks.checkNotNull(result);

    if (result.getKind() != Node.Kind.VALUE) {
      return null;
    }

    final NodeValue value = (NodeValue) result;
    return value.getBitVector();
  }

  public static BitVector evaluateBitVector(final Node node) {
    final Node result = Reducer.reduce(node);
    InvariantChecks.checkNotNull(result);

    if (result.getKind() != Node.Kind.VALUE) {
      return null;
    }

    final NodeValue value = (NodeValue) result;
    return value.getBitVector();
  }

  public static Boolean check(final Node node, final ValueProvider valueProvider) {
    final Node result = Reducer.reduce(valueProvider, node);
    InvariantChecks.checkNotNull(result);

    if (result.getKind() != Node.Kind.VALUE) {
      return null;
    }

    final NodeValue value = (NodeValue) result;
    return value.getBoolean();
  }

  public static Boolean check(final Node node) {
    final Node result = Reducer.reduce(node);
    InvariantChecks.checkNotNull(result);

    if (result.getKind() != Node.Kind.VALUE) {
      return null;
    }

    final NodeValue value = (NodeValue) result;
    return value.getBoolean();
  }
}
