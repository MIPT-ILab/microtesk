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

package ru.ispras.microtesk.mmu.translator.ir.spec;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.basis.solver.integer.IntegerField;
import ru.ispras.microtesk.basis.solver.integer.IntegerVariable;
import ru.ispras.microtesk.mmu.basis.MemoryAccessContext;

/**
 * {@link MmuCondition} represents a set of {@code AND}- or {@code OR}-connected equalities or
 * inequalities ({@link MmuConditionAtom}).
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class MmuCondition {

  public static enum Type {
    AND,
    OR
  }

  //------------------------------------------------------------------------------------------------
  // Composed Conditions
  //------------------------------------------------------------------------------------------------

  public static MmuCondition not(final MmuCondition condition) {
    final Type negatedType = (condition.getType() == Type.AND ? Type.OR : Type.AND);
    final List<MmuConditionAtom> negatedAtoms = condition.getAtoms();

    for (final MmuConditionAtom atom : condition.getAtoms()) {
      negatedAtoms.add(MmuConditionAtom.not(atom));
    }

    return new MmuCondition(negatedType, negatedAtoms);
  }

  public static MmuCondition and(final List<MmuConditionAtom> equalities) {
    return new MmuCondition(Type.AND, equalities);
  }

  public static MmuCondition or(final List<MmuConditionAtom> equalities) {
    return new MmuCondition(Type.OR, equalities);
  }

  public static MmuCondition and(final MmuConditionAtom... equalities) {
    return and(Arrays.asList(equalities));
  }

  public static MmuCondition or(final MmuConditionAtom... equalities) {
    return or(Arrays.asList(equalities));
  }

  //------------------------------------------------------------------------------------------------
  // Atomic Conditions
  //------------------------------------------------------------------------------------------------

  public static MmuCondition eq(final MmuStruct struct) {
    InvariantChecks.checkNotNull(struct);

    final List<MmuConditionAtom> atoms = new ArrayList<>(struct.getFieldCount());
    for (final IntegerVariable field : struct.getFields()) {
      atoms.add(MmuConditionAtom.eq(field));
    }

    return new MmuCondition(Type.AND, atoms);
  }

  public static MmuCondition eq(final MmuExpression expression) {
    return new MmuCondition(MmuConditionAtom.eq(expression));
  }

  public static MmuCondition eq(final IntegerField field) {
    return eq(MmuExpression.field(field));
  }

  public static MmuCondition eq(final IntegerVariable variable) {
    return eq(MmuExpression.var(variable));
  }

  public static MmuCondition eq(final MmuExpression expression, final BigInteger value) {
    return new MmuCondition(MmuConditionAtom.eq(expression, value));
  }

  public static MmuCondition eq(final IntegerField field, final BigInteger value) {
    return eq(MmuExpression.field(field), value);
  }

  public static MmuCondition eq(final IntegerVariable variable, final BigInteger value) {
    return eq(MmuExpression.var(variable), value);
  }

  public static MmuCondition eq(final MmuStruct lhsStruct, final MmuStruct rhsStruct) {
    InvariantChecks.checkNotNull(lhsStruct);
    InvariantChecks.checkNotNull(rhsStruct);

    InvariantChecks.checkTrue(lhsStruct.getBitSize() == rhsStruct.getBitSize());
    InvariantChecks.checkTrue(lhsStruct.getFieldCount() == rhsStruct.getFieldCount());

    final List<MmuConditionAtom> atoms = new ArrayList<>(lhsStruct.getFieldCount());

    final Iterator<IntegerVariable> leftIt = lhsStruct.getFields().iterator();
    final Iterator<IntegerVariable> rightIt = rhsStruct.getFields().iterator();

    while(leftIt.hasNext() && rightIt.hasNext()) {
      final IntegerVariable leftVar = leftIt.next();
      final IntegerVariable rightVar = rightIt.next();

      InvariantChecks.checkTrue(leftVar.getWidth() == rightVar.getWidth());
      atoms.add(MmuConditionAtom.eq(leftVar, rightVar));
    }

    return new MmuCondition(Type.AND, atoms);
  }

  public static MmuCondition range(
      final MmuExpression expression, final BigInteger min, final BigInteger max) {

    if (min.compareTo(max) == 0) {
      return eq(expression, min);
    }

    return new MmuCondition(MmuConditionAtom.range(expression, min, max));
  }

  public static MmuCondition range(
      final IntegerField field, final BigInteger min, final BigInteger max) {
    return range(MmuExpression.field(field), min, max);
  }

  public static MmuCondition range(
      final IntegerVariable variable, final BigInteger min, final BigInteger max) {
    return range(MmuExpression.var(variable), min, max);
  }

  public static MmuCondition eqReplaced(final MmuExpression expression) {
    return new MmuCondition(MmuConditionAtom.eqReplaced(expression));
  }

  public static MmuCondition eqReplaced(final IntegerField field) {
    return eqReplaced(MmuExpression.field(field));
  }

  public static MmuCondition eqReplaced(final IntegerVariable variable) {
    return eqReplaced(MmuExpression.var(variable));
  }

  //------------------------------------------------------------------------------------------------
  // Negated Atomic Conditions
  //------------------------------------------------------------------------------------------------

  public static MmuCondition neq(final MmuStruct struct) {
    InvariantChecks.checkNotNull(struct);

    final List<MmuConditionAtom> atoms = new ArrayList<>(struct.getFieldCount());
    for (final IntegerVariable field : struct.getFields()) {
      atoms.add(MmuConditionAtom.neq(field));
    }

    return new MmuCondition(Type.OR, atoms);
  }

  public static MmuCondition neq(final MmuExpression expression) {
    return new MmuCondition(MmuConditionAtom.neq(expression));
  }

  public static MmuCondition neq(final IntegerField field) {
    return neq(MmuExpression.field(field));
  }

  public static MmuCondition neq(final IntegerVariable variable) {
    return neq(MmuExpression.var(variable));
  }

  public static MmuCondition neq(final MmuExpression expression, final BigInteger value) {
    return new MmuCondition(MmuConditionAtom.neq(expression, value));
  }

  public static MmuCondition neq(final IntegerField field, final BigInteger value) {
    return neq(MmuExpression.field(field), value);
  }

  public static MmuCondition neq(final IntegerVariable variable, final BigInteger value) {
    return neq(MmuExpression.var(variable), value);
  }

  public static MmuCondition neq(final MmuStruct lhsStruct, final MmuStruct rhsStruct) {
    InvariantChecks.checkNotNull(lhsStruct);
    InvariantChecks.checkNotNull(rhsStruct);

    InvariantChecks.checkTrue(lhsStruct.getBitSize() == rhsStruct.getBitSize());
    InvariantChecks.checkTrue(lhsStruct.getFieldCount() == rhsStruct.getFieldCount());

    final List<MmuConditionAtom> atoms = new ArrayList<>(lhsStruct.getFieldCount());

    final Iterator<IntegerVariable> leftIt = lhsStruct.getFields().iterator();
    final Iterator<IntegerVariable> rightIt = rhsStruct.getFields().iterator();

    while(leftIt.hasNext() && rightIt.hasNext()) {
      final IntegerVariable leftVar = leftIt.next();
      final IntegerVariable rightVar = rightIt.next();

      InvariantChecks.checkTrue(leftVar.getWidth() == rightVar.getWidth());
      atoms.add(MmuConditionAtom.neq(leftVar, rightVar));
    }

    return new MmuCondition(Type.OR, atoms);
  }

  public static MmuCondition nrange(
      final MmuExpression expression, final BigInteger min, final BigInteger max) {

    if (min.compareTo(max) == 0) {
      return neq(expression, min);
    }

    return new MmuCondition(MmuConditionAtom.nrange(expression, min, max));
  }

  public static MmuCondition nrange(
      final IntegerField field, final BigInteger min, final BigInteger max) {
    return nrange(MmuExpression.field(field), min, max);
  }

  public static MmuCondition nrange(
      final IntegerVariable variable, final BigInteger min, final BigInteger max) {
    return nrange(MmuExpression.var(variable), min, max);
  }

  public static MmuCondition neqReplaced(final MmuExpression expression) {
    return new MmuCondition(MmuConditionAtom.neqReplaced(expression));
  }

  public static MmuCondition neqReplaced(final IntegerField field) {
    return neqReplaced(MmuExpression.field(field));
  }

  public static MmuCondition neqReplaced(final IntegerVariable variable) {
    return neqReplaced(MmuExpression.var(variable));
  }

  //------------------------------------------------------------------------------------------------
  // Internals
  //------------------------------------------------------------------------------------------------

  private final Type type;

  private final List<MmuConditionAtom> atoms = new ArrayList<>();

  protected MmuCondition(final Type type, final List<MmuConditionAtom> atoms) {
    InvariantChecks.checkNotNull(type);
    InvariantChecks.checkNotNull(atoms);

    this.type = type;
    this.atoms.addAll(atoms);
  }

  protected MmuCondition(final MmuCondition condition) {
    InvariantChecks.checkNotNull(condition);

    this.type = condition.type;
    this.atoms.addAll(condition.getAtoms());
  }

  protected MmuCondition(final MmuConditionAtom atom) {
    InvariantChecks.checkNotNull(atom);

    this.type = Type.AND;
    this.atoms.add(atom);
  }

  public Type getType() {
    return type;
  }

  public List<MmuConditionAtom> getAtoms() {
    return atoms;
  }

  public MmuCondition getInstance(final int instanceId, final MemoryAccessContext context) {
    InvariantChecks.checkNotNull(context);

    if (context.isEmptyStack() && instanceId == 0) {
      return this;
    }

    final List<MmuConditionAtom> atomInstances = new ArrayList<>();

    for (final MmuConditionAtom atom : atoms) {
      atomInstances.add(atom.getInstance(instanceId, context));
    }

    return new MmuCondition(type, atomInstances);
  }
  
  @Override
  public String toString() {
    return String.format("%s %s", type, atoms);
  }
}
