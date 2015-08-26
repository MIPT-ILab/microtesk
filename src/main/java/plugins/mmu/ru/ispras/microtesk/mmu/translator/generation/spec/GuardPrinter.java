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

import static ru.ispras.fortress.util.InvariantChecks.checkNotNull;

import java.util.HashSet;
import java.util.Set;

import ru.ispras.fortress.data.DataTypeId;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeOperation;
import ru.ispras.fortress.expression.StandardOperation;
import ru.ispras.fortress.util.Pair;
import ru.ispras.microtesk.mmu.translator.ir.AbstractStorage;
import ru.ispras.microtesk.mmu.translator.ir.Attribute;
import ru.ispras.microtesk.mmu.translator.ir.AttributeRef;
import ru.ispras.microtesk.mmu.translator.ir.Ir;
import ru.ispras.microtesk.mmu.translator.ir.Segment;

final class GuardPrinter {
  private final Ir ir;
  private final Pair<String, String> guards;

  public GuardPrinter(final Ir ir, final Node condition) {
    checkNotNull(ir);
    checkNotNull(condition);

    this.ir = ir;
    this.guards = getGuards(condition, false);
  }

  public String getGuard() {
    return guards.first;
  }

  public String getNegatedGuard() {
    return guards.second;
  }

  private Pair<String, String> getGuards(final Node cond, boolean reversed) {
    if (!cond.isType(DataTypeId.LOGIC_BOOLEAN)) {
      throw new IllegalStateException("Boolean expression is expected: " + cond);
    }

    final Pair<String, String> guards;
    if (cond.getKind() == Node.Kind.VARIABLE && cond.getUserData() instanceof AttributeRef) {
      final AttributeRef attrRef = (AttributeRef) cond.getUserData();
      guards = getEventBasedGuards(attrRef);
    } else if (cond.getKind() == Node.Kind.OPERATION) {
      final NodeOperation expr = (NodeOperation) cond;
      if (expr.getOperationId() == StandardOperation.NOT) {
        guards = getGuards(expr.getOperand(0), true);
      } else {
        guards = new Pair<String, String>(
            "null /* TODO: GUARD -> if true */",
            "null /* TODO: GUARD -> if false */");
        //guards = getConditionGuards(expr);
      }
    } else {
      throw new IllegalStateException("Unsupported condition expression format: " + cond);
    }

    if (reversed) {
      return new Pair<>(guards.second, guards.first);
    }

    return guards;
  }

  private Pair<String, String> getEventBasedGuards(final AttributeRef attrRef) {
    final Attribute attr = attrRef.getAttribute();
    if (!attr.getId().equals(AbstractStorage.HIT_ATTR_NAME)) {
      throw new IllegalStateException("Unsupported attribute call: " + attr.getId());
    }

    final String hit;
    final String miss;

    final AbstractStorage target = attrRef.getTarget();
    if (target instanceof Segment) {
      final String segmentId = ((Segment) target).getId();

      final Set<String> allSegmentIds = new HashSet<>(ir.getSegments().keySet());
      allSegmentIds.remove(segmentId);

      final String allOtherSegmentId =
          Utils.toString(allSegmentIds, ".get(), ") + ".get()";

      hit = String.format(
          "new MmuGuard(null, Arrays.<MmuSegment>asList(%s.get()))", segmentId);
      miss = String.format(
          "new MmuGuard(null, Arrays.<MmuSegment>asList(%s))", allOtherSegmentId);
    } else {
      final String bufferId = attrRef.getTarget().getId();
      hit = String.format("new MmuGuard(%s.get(), BufferAccessEvent.HIT)", bufferId);
      miss = String.format("new MmuGuard(%s.get(), BufferAccessEvent.MISS)", bufferId);
    }

    return new Pair<>(hit, miss);
  }
/*
  private Pair<String, String> getConditionGuards(final NodeOperation e) {
    final Enum<?> opId = e.getOperationId();

    if (opId == StandardOperation.AND || opId == StandardOperation.OR) {
      final List<String> atoms = getConditionAtoms(e.getOperands());
      final List<String> negated = new ArrayList<>(atoms.size());

      for (final String atom : atoms) {
        negated.add(String.format("MmuConditionAtom.not(%s)", atom));
      }

      final String directGuard;
      final String negatedGuard;

      if (opId == StandardOperation.AND) {
        directGuard = String.format(
            "new MmuGuard(MmuCondition.and(%s))", Utils.toString(atoms, ", "));
        negatedGuard = String.format(
            "new MmuGuard(MmuCondition.or(%s))", Utils.toString(negated, ", "));
      } else {
        directGuard = String.format(
            "new MmuGuard(MmuCondition.or(%s))", Utils.toString(atoms, ", "));
        negatedGuard = String.format(
            "new MmuGuard(MmuCondition.and(%s))", Utils.toString(negated, ", "));
      }

      return new Pair<>(directGuard, negatedGuard);
    }

    return getEqualityBasedGuards(e);
  }

  private List<MmuConditionAtom> getConditionAtoms(final List<? extends Node> nodes) {
    final List<MmuConditionAtom> atoms = new ArrayList<>();
    for (final Node node : nodes) {
      final MmuGuard[] guards = getEqualityBasedGuards((NodeOperation) node);
      atoms.addAll(guards[0].getCondition().getAtoms());
    }
    return atoms;
  }

  private MmuGuard[] getEqualityBasedGuards(final NodeOperation expr) {
    final Enum<?> operator = expr.getOperationId();
    if (StandardOperation.EQ != operator && StandardOperation.NOTEQ != operator) {
      throw new IllegalStateException("Not an equality based condition: " + expr);
    }

    final Atom lhs = atomExtractor.extract(expr.getOperand(0));
    final Atom rhs = atomExtractor.extract(expr.getOperand(1));

    final BigInteger value;
    final Atom variableAtom;

    if (Atom.Kind.VALUE == lhs.getKind()) {
      value = (BigInteger) lhs.getObject();
      variableAtom = rhs;
    } else if (Atom.Kind.VALUE == rhs.getKind()) {
      value = (BigInteger) rhs.getObject();
      variableAtom = lhs;
    } else {
      throw new IllegalArgumentException(
          "Both sides of an equality expression are constants.");
    }

    final MmuGuard eq;
    final MmuGuard noteq;
    switch (variableAtom.getKind()) {
      case VARIABLE: {
        final IntegerVariable intVar = (IntegerVariable) variableAtom.getObject();
        eq = new MmuGuard(MmuCondition.eq(intVar, value));
        noteq = new MmuGuard(MmuCondition.neq(intVar, value));
        break;
      }

      case FIELD: {
        final IntegerField intField = (IntegerField) variableAtom.getObject();
        final IntegerVariable intVar = intField.getVariable();
        final int lo = intField.getLoIndex();
        final int hi = intField.getHiIndex();
        eq = new MmuGuard(MmuCondition.eq(new IntegerField(intVar, lo, hi), value));
        noteq = new MmuGuard(MmuCondition.neq(new IntegerField(intVar, lo, hi), value));
        break;
      }

      case CONCAT: {
        final MmuExpression mmuExpr = (MmuExpression) variableAtom.getObject();
        eq = new MmuGuard(MmuCondition.eq(mmuExpr));
        noteq = new MmuGuard(MmuCondition.neq(mmuExpr));
        break;
      }

      default: {
        throw new IllegalArgumentException("Variable is expected.");
      }
    }

    return (StandardOperation.EQ == operator) ?
        new MmuGuard[] {eq, noteq} : new MmuGuard[] {noteq, eq};
  }
*/
}
