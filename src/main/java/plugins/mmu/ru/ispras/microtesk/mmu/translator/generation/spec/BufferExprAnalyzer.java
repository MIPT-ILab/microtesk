/*
 * Copyright 2015-2017 ISP RAS (http://www.ispras.ru)
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

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ru.ispras.fortress.data.Data;
import ru.ispras.fortress.data.DataType;
import ru.ispras.fortress.data.DataTypeId;
import ru.ispras.fortress.data.Variable;
import ru.ispras.fortress.expression.ExprTreeVisitorDefault;
import ru.ispras.fortress.expression.ExprTreeWalker;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeOperation;
import ru.ispras.fortress.expression.NodeValue;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.expression.StandardOperation;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.fortress.util.Pair;
import ru.ispras.microtesk.mmu.translator.ir.Address;
import ru.ispras.microtesk.mmu.translator.ir.Var;
import ru.ispras.microtesk.utils.FortressUtils;
import ru.ispras.microtesk.utils.StringUtils;

final class BufferExprAnalyzer {
  private final Var addressVariable;

  private final Variable variableForAddress;
  private final IntegerFieldTracker fieldTrackerForAddress;

  private final List<Node> indexFields;
  private final List<Node> tagFields;
  private final List<Node> offsetFields;
  private final List<Pair<Node, Node>> matchBindings;

  private Node newAddressField(final NodeOperation node) {
    InvariantChecks.checkTrue(node.getOperationId() == StandardOperation.BVEXTRACT);

    if (node.getOperandCount() != 3) {
      throw new IllegalStateException("Wrong operand count (3 is expected): " + node);
    }

    final Node variable = node.getOperand(2);
    if (variable.getKind() != Node.Kind.VARIABLE) {
      throw new IllegalStateException(variable + " is not a variable.");
    }

    if (!variable.equals(addressVariable.getNode())) {
      throw new IllegalStateException(
          variable + " is not equal to " + addressVariable.getNode());
    }

    final int lo = FortressUtils.extractInt(node.getOperand(1));
    final int hi = FortressUtils.extractInt(node.getOperand(0));

    fieldTrackerForAddress.exclude(lo, hi);
    return node;
  }

  private Node newAddressField(final NodeVariable node) {
    if (!node.equals(addressVariable.getNode())) {
      throw new IllegalStateException(
          node + " is not equal to " + addressVariable.getNode());
    }

    fieldTrackerForAddress.excludeAll();
    return new NodeVariable(variableForAddress);
  }

  private class VisitorIndex extends ExprTreeVisitorDefault {
    private final Set<StandardOperation> SUPPORTED_OPS = EnumSet.of(
        StandardOperation.BVEXTRACT, StandardOperation.BVCONCAT);

    private final List<Node> fields = new ArrayList<>();

    public List<Node> getFields() {
      return fields;
    }

    @Override
    public void onOperationBegin(final NodeOperation node) {
      final Enum<?> op = node.getOperationId();
      if (!SUPPORTED_OPS.contains(op)) {
        throw new IllegalStateException(String.format(
            "Operation %s is not supported in index expressions.", op));
      }

      if (op == StandardOperation.BVEXTRACT) {
        final Node addressField = newAddressField(node);
        fields.add(addressField);
        setStatus(Status.SKIP);
      }
    }

    @Override
    public void onVariable(final NodeVariable node) {
      final Node addressField = newAddressField(node);
      fields.add(addressField);
    }
  }

  private class VisitorMatch extends ExprTreeVisitorDefault {
    private final Set<StandardOperation> SUPPORTED_OPS = EnumSet.of(
        StandardOperation.EQ, StandardOperation.AND, StandardOperation.BVEXTRACT);

    private final Deque<Enum<?>> opStack = new ArrayDeque<Enum<?>>();
    private final List<Node> fields = new ArrayList<>();

    /** Match bindings, format: (<entry expression> == (<address expression> | constant)) */
    private final List<Pair<Node, Node>> bindings = new ArrayList<>();

    private Node entrySide = null;
    private Node addressSide = null;

    public List<Node> getTagFields() {
      return fields;
    }

    public List<Pair<Node, Node>> getMatchBindings() {
      return bindings;
    }

    @Override
    public void onOperationBegin(final NodeOperation node) {
      final Enum<?> op = node.getOperationId();
      if (!SUPPORTED_OPS.contains(op)) {
        throw new IllegalStateException(String.format(
            "Operation %s is not supported in match expressions.", op));
      }

      if (op == StandardOperation.BVEXTRACT) {
        if (opStack.isEmpty() || opStack.peek() != StandardOperation.EQ) {
          throw new IllegalStateException(
              "Illegal match expression. BVEXTRACT must be used only as part of EQ expression.");
        }

        final Node variable = node.getOperand(2);
        if (variable.equals(addressVariable.getNode())) {
          final Node addressField = newAddressField(node);
          fields.add(addressField);

          InvariantChecks.checkTrue(addressSide == null);
          addressSide = addressField;
        } else {
          InvariantChecks.checkTrue(entrySide == null);
          entrySide = node;
        }

        setStatus(Status.SKIP);
      } else if (op == StandardOperation.EQ) {
        if (!opStack.isEmpty() && opStack.peek() != StandardOperation.AND) {
          throw new IllegalStateException(
              "Illegal match expression. EQ cannot be used as part of " +
              opStack.peek() + " expression");
        }

        if (node.getOperandCount() != 2) {
          throw new IllegalStateException("Wrong operand count (2 is expected): " + node);
        }

        entrySide = null;
        addressSide = null;
      }

      opStack.push(op);
    }

    @Override
    public void onOperationEnd(final NodeOperation node) {
      final Enum<?> op = node.getOperationId();
      if (op == StandardOperation.EQ) {
        InvariantChecks.checkNotNull(addressSide);
        InvariantChecks.checkNotNull(entrySide);

        final Variable variable = FortressUtils.getVariable(addressSide);

        final int addressSideSize = FortressUtils.getBitSize(addressSide);
        final int entrySideSize = FortressUtils.getBitSize(entrySide);

        if (variable.hasValue() && addressSideSize != entrySideSize) {
          final Variable newVariable = new Variable(variable.getName(),
              Data.newBitVector(variable.getData().getInteger(), entrySideSize));

          addressSide = new NodeVariable(newVariable);
        }

        bindings.add(new Pair<>(entrySide, addressSide));
      } else if (op == StandardOperation.BVEXTRACT) {
        setStatus(Status.OK);
      }

      opStack.pop();
    }

    @Override
    public void onVariable(final NodeVariable node) {
      if (node.equals(addressVariable.getNode())) {
        final Node addressField = newAddressField(node);
        fields.add(addressField);

        InvariantChecks.checkTrue(addressSide == null);
        addressSide = addressField; 
      } else {
        InvariantChecks.checkTrue(entrySide == null);
        entrySide = node;
      }
    }

    @Override
    public void onValue(final NodeValue node) {
      InvariantChecks.checkTrue(addressSide == null);

      final BigInteger value;
      final int width;

      if (node.isType(DataTypeId.LOGIC_INTEGER)) {
        value = node.getInteger();
        width = value.bitLength() + 1;
      } else if (node.isType(DataTypeId.BIT_VECTOR)) {
        value = node.getBitVector().bigIntegerValue(false);
        width = node.getBitVector().getBitSize();
      } else {
        throw new IllegalStateException("Unsupported value type: " + node.getDataType());
      }

      addressSide = new NodeVariable(new Variable("#fake", Data.newBitVector(value, width)));
    }
  }

  public BufferExprAnalyzer(
      final Address address,
      final Var addressVariable,
      final Node index,
      final Node match) {
    InvariantChecks.checkNotNull(address);
    InvariantChecks.checkNotNull(addressVariable);
    InvariantChecks.checkNotNull(index);
    InvariantChecks.checkNotNull(match);

    this.addressVariable = addressVariable.accessNested(address.getAccessChain());

    final int addressSize = address.getAddressType().getBitSize();
    final String addressName =
        address.getId() + "." + StringUtils.toString(address.getAccessChain(), ".");

    this.variableForAddress = new Variable(addressName, DataType.BIT_VECTOR(addressSize));
    this.fieldTrackerForAddress = new IntegerFieldTracker(variableForAddress);

    final VisitorIndex visitorIndex = new VisitorIndex();
    final ExprTreeWalker walkerIndex = new ExprTreeWalker(visitorIndex);
    walkerIndex.visit(index);
    this.indexFields = visitorIndex.getFields();

    final VisitorMatch visitorMatch = new VisitorMatch();
    final ExprTreeWalker walkerMatch = new ExprTreeWalker(visitorMatch);
    walkerMatch.visit(match);
    this.tagFields = visitorMatch.getTagFields();
    this.matchBindings = visitorMatch.getMatchBindings();

    this.offsetFields = fieldTrackerForAddress.getFields();
  }

  public List<Node> getIndexFields() {
    return indexFields;
  }

  public List<Node> getTagFields() {
    return tagFields;
  }

  public List<Node> getOffsetFields() {
    return offsetFields;
  }

  public List<Pair<Node, Node>> getMatchBindings() {
    return matchBindings;
  }
}
