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

package ru.ispras.microtesk.mmu.test.sequence.engine.memory;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import ru.ispras.fortress.util.BitUtils;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.basis.solver.integer.IntegerClause;
import ru.ispras.microtesk.basis.solver.integer.IntegerField;
import ru.ispras.microtesk.basis.solver.integer.IntegerFormula;
import ru.ispras.microtesk.basis.solver.integer.IntegerVariable;
import ru.ispras.microtesk.mmu.basis.MemoryAccessStack;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuAction;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuBinding;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuBufferAccess;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuCondition;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuConditionAtom;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuExpression;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuGuard;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuTransition;

/**
 * {@link MemorySymbolicExecutor} implements a simple symbolic executor of memory access structures.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class MemorySymbolicExecutor {
  /**
   * Result of a symbolic execution.
   */
  public static final class Result {
    private final Collection<IntegerVariable> variables;
    private final IntegerFormula.Builder<IntegerField> formula;

    /** Contains original variables. */
    private final Collection<IntegerVariable> originals;
    /** Maps a variable name to the instance number (it is increased after each assignment). */
    private final Map<String, Integer> instances;
    /** Maps a variable instance name to the corresponding variable. */
    private final Map<String, IntegerVariable> cache;

    private Result(
        final Collection<IntegerVariable> variables,
        final IntegerFormula.Builder<IntegerField> formula,
        final Collection<IntegerVariable> originals,
        final Map<String, Integer> instances,
        final Map<String, IntegerVariable> cache) {
      InvariantChecks.checkNotNull(variables);
      InvariantChecks.checkNotNull(formula);
      InvariantChecks.checkNotNull(originals);
      InvariantChecks.checkNotNull(instances);
      InvariantChecks.checkNotNull(cache);

      this.variables = variables;
      this.formula = formula;

      this.originals = originals;
      this.instances = instances;
      this.cache = cache;
    }

    public Result() {
      this(
          new LinkedHashSet<IntegerVariable>(),
          new IntegerFormula.Builder<IntegerField>(),
          new LinkedHashSet<IntegerVariable>(),
          new HashMap<String, Integer>(),
          new HashMap<String, IntegerVariable>());
    }

    public Result(final Result r) {
      this(
          new LinkedHashSet<>(r.variables),
          new IntegerFormula.Builder<>(r.formula),
          new LinkedHashSet<>(r.originals),
          new HashMap<>(r.instances),
          new HashMap<>(r.cache));
    }

    public Collection<IntegerVariable> getVariables() {
      return variables;
    }

    public IntegerFormula<IntegerField> getFormula() {
      return formula.build();
    }
  }

  private final MemoryAccessPath.Entry entry;
  private final MemoryAccessPath path;
  private final MemoryAccessStructure structure;

  private final boolean includeOriginalVariables;

  private final Result result;

  private final Map<Integer, MemoryAccessStack> stacks = new HashMap<>();

  public MemorySymbolicExecutor(
      final MemoryAccessPath.Entry entry,
      final Result result,
      final boolean includeOriginalVariables) {
    InvariantChecks.checkNotNull(entry);
    InvariantChecks.checkNotNull(result);

    this.entry = entry;
    this.path = null;
    this.structure = null;
    this.result = result;
    this.includeOriginalVariables = includeOriginalVariables;
  }

  public MemorySymbolicExecutor(
      final MemoryAccessPath.Entry entry,
      final boolean includeOriginalVariables) {
    InvariantChecks.checkNotNull(entry);

    this.entry = entry;
    this.path = null;
    this.structure = null;
    this.result = null;
    this.includeOriginalVariables = includeOriginalVariables;
  }

  public MemorySymbolicExecutor(
      final MemoryAccessPath path,
      final boolean includeOriginalVariables) {
    InvariantChecks.checkNotNull(path);

    this.entry = null;
    this.path = path;
    this.structure = null;
    this.result = null;
    this.includeOriginalVariables = includeOriginalVariables;
  }

  public MemorySymbolicExecutor(
      final MemoryAccessStructure structure,
      final boolean includeOriginalVariables) {
    InvariantChecks.checkNotNull(structure);

    this.entry = null;
    this.path = null;
    this.structure = structure;
    this.result = null;
    this.includeOriginalVariables = includeOriginalVariables;
  }

  public Result execute() {
    if (entry != null) {
      execute(entry);
    } else if (path != null) {
      execute(path);
    } else if (structure != null) {
      execute(structure);
    } else {
      InvariantChecks.checkTrue(false);
    }

    if (includeOriginalVariables) {
      // Add the constraints of the kind V = V(n), where n is the last instance number of V.
      result.variables.addAll(result.originals);

      for (final IntegerVariable original : result.originals) {
        final IntegerField lhs = new IntegerField(original);
        final IntegerField rhs = new IntegerField(getPathVarInstance(original.getName()));

        result.formula.addEquation(lhs, rhs, true);
      }
    }

    return result;
  }

  private void execute(final MemoryAccessStructure structure) {
    InvariantChecks.checkNotNull(structure);
    InvariantChecks.checkGreaterThanZero(structure.size());

    for (int j = 0; j < structure.size(); j++) {
      final MemoryAccessPath path2 = structure.getAccess(j).getPath();

      for (int i = 0; i < j; i++) {
        final MemoryAccessPath path1 = structure.getAccess(i).getPath();
        final MemoryDependency dependency = structure.getDependency(i, j);

        if (dependency != null) {
          // It does not execute the paths (only the dependency).
          execute(path1, i, path2, j, dependency);
        }
      }

      execute(path2, j);
    }
  }

  private void execute(final MemoryAccessPath path) {
    InvariantChecks.checkNotNull(path);
    execute(path, -1);
  }

  private void execute(final MemoryAccessPath.Entry entry) {
    InvariantChecks.checkNotNull(entry);
    execute(entry, -1);
  }

  private void execute(final MemoryAccessPath path, final int pathIndex) {
    InvariantChecks.checkNotNull(path);

    for (final MemoryAccessPath.Entry entry : path.getEntries()) {
      execute(entry, pathIndex);
    }
  }

  private void execute(
      final MemoryAccessPath path1,
      final int pathIndex1,
      final MemoryAccessPath path2,
      final int pathIndex2,
      final MemoryDependency dependency) {
    InvariantChecks.checkNotNull(path1);
    InvariantChecks.checkNotNull(path2);
    InvariantChecks.checkNotNull(dependency);

    for (final MemoryHazard hazard : dependency.getHazards()) {
      execute(hazard, pathIndex1, pathIndex2);
    }
  }

  private void execute(final MemoryHazard hazard, final int pathIndex1, final int pathIndex2) {
    InvariantChecks.checkNotNull(hazard);

    final MmuCondition condition = hazard.getCondition();
    if (condition != null) {
      execute(condition, pathIndex1, pathIndex2);
    }
  }

  private void execute(final MmuCondition condition, final int pathIndex1, final int pathIndex2) {
    InvariantChecks.checkNotNull(condition);

    final MemoryAccessStack stack1 = getStack(pathIndex1);
    final MemoryAccessStack stack2 = getStack(pathIndex2);

    final IntegerClause.Type clauseType =
        (condition.getType() == MmuCondition.Type.AND)
        ? IntegerClause.Type.AND
        : IntegerClause.Type.OR;

    final IntegerClause.Builder<IntegerField> clauseBuilder =
        new IntegerClause.Builder<>(clauseType);

    for (final MmuConditionAtom atom : condition.getAtoms()) {
      if (atom.getType() != MmuConditionAtom.Type.EQ_SAME_EXPR) {
        continue;
      }

      final MmuExpression expression = atom.getLhsExpr();

      for (final IntegerField term : expression.getTerms()) {
        final IntegerField term1 = stack1.getInstance(term);
        final IntegerField term2 = stack2.getInstance(term);
        
        final IntegerField field1 = getPathFieldInstance(term1, pathIndex1);
        final IntegerField field2 = getPathFieldInstance(term2, pathIndex2);

        clauseBuilder.addEquation(field1, field2, !atom.isNegated());

        result.variables.add(field1.getVariable());
        result.originals.add(getPathVar(term1.getVariable(), pathIndex1));

        result.variables.add(field2.getVariable());
        result.originals.add(getPathVar(term2.getVariable(), pathIndex2));
      }
    }

    result.formula.addClause(clauseBuilder.build());
  }

  private void execute(final MemoryAccessPath.Entry entry, final int pathIndex) {
    InvariantChecks.checkNotNull(entry);

    updateStack(entry, pathIndex);

    final MmuTransition transition = entry.getTransition();

    if (transition != null) {
      final MmuGuard guard = transition.getGuard();

      if (guard != null) {
        execute(guard, pathIndex);
      }

      final MmuAction action = transition.getTarget();

      if (action != null) {
        execute(action, pathIndex);
      }
    }
  }

  private void execute(final MmuGuard guard, final int pathIndex) {
    InvariantChecks.checkNotNull(guard);

    final MemoryAccessStack stack = getStack(pathIndex);

    final MmuBufferAccess bufferAccess = guard.getBufferAccess(stack);
    if (bufferAccess != null) {
      execute(bufferAccess, pathIndex);
    }

    final MmuCondition condition = guard.getCondition(stack);
    if (condition != null) {
      execute(condition, pathIndex);
    }
  }

  private void execute(final MmuAction action, final int pathIndex) {
    InvariantChecks.checkNotNull(action);

    final MemoryAccessStack stack = getStack(pathIndex);

    final Map<IntegerField, MmuBinding> assignments = action.getAction(stack);
    if (assignments != null) {
      execute(assignments.values(), pathIndex);
    }
  }

  private void execute(final MmuBufferAccess bufferAccess, final int pathIndex) {
    InvariantChecks.checkNotNull(bufferAccess);

    final MemoryAccessStack stack = getStack(pathIndex);

    execute(bufferAccess.getBuffer().getMatchBindings(stack), pathIndex);
  }

  private void execute(final MmuCondition condition, final int pathIndex) {
    InvariantChecks.checkNotNull(condition);

    final IntegerClause.Type definedType = condition.getType() == MmuCondition.Type.AND ?
        IntegerClause.Type.AND : IntegerClause.Type.OR;

    final IntegerClause.Builder<IntegerField> clauseBuilder =
        new IntegerClause.Builder<>(definedType);

    for (final MmuConditionAtom atom : condition.getAtoms()) {
      execute(clauseBuilder, atom, pathIndex);
    }

    result.formula.addClause(clauseBuilder.build());
  }

  private void execute(
      final IntegerClause.Builder<IntegerField> clauseBuilder,
      final MmuConditionAtom atom,
      final int pathIndex) {
    final MmuExpression lhsExpr = atom.getLhsExpr();

    switch(atom.getType()) {
      case EQ_EXPR_CONST:
        final BigInteger rhsConst = atom.getRhsConst();

        int offset = 0;
        for (final IntegerField term : lhsExpr.getTerms()) {
          final int lo = offset;
          final int hi = offset + (term.getWidth() - 1);

          final IntegerField field = getPathFieldInstance(term, pathIndex);
          final BigInteger value = BitUtils.getField(rhsConst, lo, hi);

          clauseBuilder.addEquation(field, value, !atom.isNegated());
          offset += term.getWidth();

          result.variables.add(field.getVariable());
          result.originals.add(getPathVar(term.getVariable(), pathIndex));
        }
        break;
      case EQ_EXPR_EXPR:
        final MmuExpression rhsExpr = atom.getRhsExpr();
        InvariantChecks.checkTrue(lhsExpr.size() == rhsExpr.size());

        for (int i = 0; i < lhsExpr.size(); i++) {
          final IntegerField lhsTerm = lhsExpr.getTerms().get(i);
          final IntegerField rhsTerm = rhsExpr.getTerms().get(i);
          InvariantChecks.checkTrue(lhsTerm.getWidth() == rhsTerm.getWidth());

          final IntegerField lhsField = getPathFieldInstance(lhsTerm, pathIndex);
          final IntegerField rhsField = getPathFieldInstance(rhsTerm, pathIndex);

          clauseBuilder.addEquation(lhsField, rhsField, !atom.isNegated());

          result.variables.add(lhsField.getVariable());
          result.variables.add(rhsField.getVariable());

          result.originals.add(getPathVar(lhsTerm.getVariable(), pathIndex));
          result.originals.add(getPathVar(rhsTerm.getVariable(), pathIndex));
        }
        break;
      default:
        InvariantChecks.checkTrue(false);
        break;
    }
  }

  private void execute(final Collection<MmuBinding> bindings, final int pathIndex) {
    InvariantChecks.checkNotNull(bindings);

    final IntegerClause.Builder<IntegerField> clauseBuilder =
        new IntegerClause.Builder<>(IntegerClause.Type.AND);

    for (final MmuBinding binding : bindings) {
      final IntegerField lhs = binding.getLhs();
      final MmuExpression rhs = binding.getRhs();

      final IntegerVariable prevLhsVar = getPathVarInstance(lhs.getVariable(), pathIndex);

      result.variables.add(prevLhsVar);
      result.originals.add(getPathVar(lhs.getVariable(), pathIndex));

      if (rhs != null) {
        final IntegerVariable nextLhsVar = getNextPathVarInstance(lhs.getVariable(), pathIndex);
        result.variables.add(nextLhsVar);

        if (lhs.getLoIndex() > 0) {
          clauseBuilder.addEquation(
              new IntegerField(nextLhsVar, 0, lhs.getLoIndex() - 1),
              new IntegerField(prevLhsVar, 0, lhs.getLoIndex() - 1), true);
        }

        if (lhs.getHiIndex() < lhs.getWidth() - 1) {
          clauseBuilder.addEquation(
              new IntegerField(nextLhsVar, lhs.getHiIndex() + 1, lhs.getWidth() - 1),
              new IntegerField(prevLhsVar, lhs.getHiIndex() + 1, lhs.getWidth() - 1), true);
        }

        int offset = lhs.getLoIndex();

        for (final IntegerField term : rhs.getTerms()) {
          final IntegerField field = getPathFieldInstance(term, pathIndex);

          final int lo = offset;
          final int hi = offset + (field.getWidth() - 1);

          clauseBuilder.addEquation(new IntegerField(nextLhsVar, lo, hi), field, true);
          offset += field.getWidth();

          result.variables.add(field.getVariable());
          result.originals.add(getPathVar(term.getVariable(), pathIndex));
        }

        if (offset <= lhs.getHiIndex()) {
          final int lo = offset;
          final int hi = lhs.getHiIndex();
  
          clauseBuilder.addEquation(new IntegerField(nextLhsVar, lo, hi), BigInteger.ZERO, true);
        }

        definePathVarInstance(lhs.getVariable(), pathIndex);
      } // if right-hand side exists.
    } // for each binding.

    if (clauseBuilder.size() != 0) {
      result.formula.addClause(clauseBuilder.build());
    }
  }

  private static String getPathVarName(final String varName, final int pathIndex) {
    InvariantChecks.checkNotNull(varName);
    return pathIndex == -1 ? varName : String.format("%s$%d", varName, pathIndex);
  }

  private static String getPathVarInstanceName(final String pathVarName, final int n) {
    return String.format("%s(%d)", pathVarName, n);
  }

  private int getPathVarNumber(final String pathVarName) {
    InvariantChecks.checkNotNull(pathVarName);
    return result.instances.containsKey(pathVarName) ? result.instances.get(pathVarName) : 0;
  }

  private IntegerVariable getPathVarInstance(final String pathVarName) {
    InvariantChecks.checkNotNull(pathVarName);

    final int n = getPathVarNumber(pathVarName);
    final String pathVarInstanceName = getPathVarInstanceName(pathVarName, n);

    return result.cache.get(pathVarInstanceName);
  }

  private void definePathVarInstance(final String pathVarName) {
    InvariantChecks.checkNotNull(pathVarName);

    final int n = getPathVarNumber(pathVarName);
    result.instances.put(pathVarName, n + 1);
  }

  private IntegerVariable getPathVar(final IntegerVariable var, final int pathIndex) {
    InvariantChecks.checkNotNull(var);

    final String pathVarName = getPathVarName(var.getName(), pathIndex);
    return getVariable(pathVarName, var.getWidth(), var.getValue());
  }

  private IntegerVariable getPathVarInstance(final IntegerVariable var, final int pathIndex) {
    InvariantChecks.checkNotNull(var);

    final String pathVarName = getPathVarName(var.getName(), pathIndex);
    final int n = getPathVarNumber(pathVarName);
    final String pathVarInstanceName = getPathVarInstanceName(pathVarName, n);

    return getVariable(pathVarInstanceName, var.getWidth(), var.getValue());
  }

  private IntegerVariable getNextPathVarInstance(final IntegerVariable var, final int pathIndex) {
    InvariantChecks.checkNotNull(var);

    final String pathVarName = getPathVarName(var.getName(), pathIndex);
    final int n = getPathVarNumber(pathVarName);
    final String pathVarInstanceName = getPathVarInstanceName(pathVarName, n + 1);

    return getVariable(pathVarInstanceName, var.getWidth(), var.getValue());
  }

  private IntegerField getPathFieldInstance(final IntegerField field, final int pathIndex) {
    InvariantChecks.checkNotNull(field);

    final IntegerVariable var = getPathVarInstance(field.getVariable(), pathIndex);

    final int lo = field.getLoIndex();
    final int hi = field.getHiIndex();

    return new IntegerField(var, lo, hi);
  }

  private void definePathVarInstance(final IntegerVariable var, final int pathIndex) {
    InvariantChecks.checkNotNull(var);

    final String pathVarName = getPathVarName(var.getName(), pathIndex);
    definePathVarInstance(pathVarName);
  }

  private IntegerVariable getVariable(
      final String varInstanceName, final int width, final BigInteger value) {
    InvariantChecks.checkNotNull(varInstanceName);

    IntegerVariable varInstance = result.cache.get(varInstanceName);
    if (varInstance == null) {
      result.cache.put(varInstanceName,
          varInstance = new IntegerVariable(varInstanceName, width, value));
    }

    return varInstance;
  }

  private MemoryAccessStack getStack(final int pathIndex) {
    MemoryAccessStack stack = stacks.get(pathIndex);

    if (stack == null) {
      final String id = pathIndex != -1 ? String.format("%d", pathIndex) : "";
      stacks.put(pathIndex, stack = new MemoryAccessStack(id));
    }

    return stack;
  }

  private void updateStack(final MemoryAccessPath.Entry entry, final int pathIndex) {
    InvariantChecks.checkNotNull(entry);

    final MemoryAccessStack stack = getStack(pathIndex);

    if (entry.isCall()) {
      stack.call(entry.getFrame());
    } else if (entry.isReturn()) {
      stack.ret();
    }
  }
}
