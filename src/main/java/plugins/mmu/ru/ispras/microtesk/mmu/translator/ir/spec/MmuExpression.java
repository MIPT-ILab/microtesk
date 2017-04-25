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
import java.util.Collection;
import java.util.List;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.basis.solver.integer.IntegerField;
import ru.ispras.microtesk.basis.solver.integer.IntegerVariable;
import ru.ispras.microtesk.mmu.basis.MemoryAccessContext;

/**
 * {@link MmuExpression} represents an expression, which is a sequence of {@link IntegerField}.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 * @author <a href="mailto:protsenko@ispras.ru">Alexander Protsenko</a>
 */
public final class MmuExpression {
  //------------------------------------------------------------------------------------------------
  // Composed Expressions
  //------------------------------------------------------------------------------------------------

  /**
   * Creates a concatenation of the fields.
   * 
   * @param fields the fields to be concatenated.
   * @return the expression.
   */
  public static MmuExpression cat(final List<IntegerField> fields) {
    return new MmuExpression(fields);
  }

  public static MmuExpression cat(final IntegerField... fields) {
    return rcat(Arrays.<IntegerField>asList(fields));
  }

  public static MmuExpression catVars(final List<IntegerVariable> variables) {
    final List<IntegerField> fields = new ArrayList<>(variables.size());

    for (final IntegerVariable variable : variables) {
      fields.add(new IntegerField(variable));
    }

    return new MmuExpression(fields);
  }

  public static MmuExpression catVars(final IntegerVariable... variables) {
    return catVars(Arrays.<IntegerVariable>asList(variables));
  }

  /**
   * Creates a reversed concatenation of the fields.
   * 
   * @param fields the fields to be concatenated.
   * @return the expression.
   */
  public static MmuExpression rcat(final List<IntegerField> fields) {
    final List<IntegerField> reversedAtoms = new ArrayList<>(fields.size());

    for (final IntegerField field : fields) {
      reversedAtoms.add(0, field);
    }

    return new MmuExpression(reversedAtoms);
  }

  public static MmuExpression rcat(final IntegerField... fields) {
    return rcat(Arrays.<IntegerField>asList(fields));
  }

  public static MmuExpression rcatVars(final Collection<IntegerVariable> variables) {
    final List<IntegerField> reversedAtoms = new ArrayList<>(variables.size());

    for (final IntegerVariable variable : variables) {
      reversedAtoms.add(0, new IntegerField(variable));
    }

    return new MmuExpression(reversedAtoms);
  }

  public static MmuExpression rcatVars(final IntegerVariable... variables) {
    return rcatVars(Arrays.<IntegerVariable>asList(variables));
  }

  //------------------------------------------------------------------------------------------------
  // Atomic Expressions
  //------------------------------------------------------------------------------------------------

  public static MmuExpression empty() {
    return new MmuExpression();
  }

  public static MmuExpression field(final IntegerField field) {
    return new MmuExpression(field);
  }

  public static MmuExpression var(final IntegerVariable variable) {
    return field(new IntegerField(variable));
  }

  public static MmuExpression var(final IntegerVariable variable, final int lo, final int hi) {
    return field(new IntegerField(variable, lo, hi));
  }

  public static MmuExpression val(final BigInteger value, int width) {
    return var(new IntegerVariable(String.format("literal_%s:%d", value, width), width, value));
  }

  //------------------------------------------------------------------------------------------------
  // Internals
  //------------------------------------------------------------------------------------------------

  private final List<IntegerField> terms = new ArrayList<>();

  private MmuExpression() {}

  private MmuExpression(final List<IntegerField> terms) {
    InvariantChecks.checkNotNull(terms);
    this.terms.addAll(terms);
  }

  private MmuExpression(final IntegerField term) {
    InvariantChecks.checkNotNull(term);
    this.terms.add(term);
  }

  public int size() {
    return terms.size();
  }

  public List<IntegerField> getTerms() {
    return terms;
  }

  public int getWidth() {
    int width = 0;

    for (final IntegerField field : terms) {
      width += field.getWidth();
    }

    return width;
  }

  public int getLoIndex(final IntegerVariable variable) {
    InvariantChecks.checkNotNull(variable);

    int lo = -1;
    for (final IntegerField term : terms) {
      if (term.getVariable() == variable) {
        if (term.getLoIndex() < lo || lo == -1) {
          lo = term.getLoIndex();
        }
      }
    }

    InvariantChecks.checkTrue(lo >= 0);
    return lo;
  }

  public int getHiIndex(final IntegerVariable variable) {
    InvariantChecks.checkNotNull(variable);

    int hi = -1;
    for (final IntegerField term : terms) {
      if (term.getVariable() == variable) {
        if (term.getHiIndex() > hi) {
          hi = term.getHiIndex();
        }
      }
    }

    InvariantChecks.checkTrue(hi >= 0);
    return hi;
  }

  public MmuExpression getInstance(final String instanceId, final MemoryAccessContext context) {
    InvariantChecks.checkNotNull(context);

    if (context.isEmptyStack() && instanceId == null) {
      return this;
    }

    final List<IntegerField> termInstances = new ArrayList<>();

    for (final IntegerField term : terms) {
      termInstances.add(context.getInstance(instanceId, term));
    }

    return new MmuExpression(termInstances);
  }

  @Override
  public String toString() {
    if (terms.isEmpty()) {
      return "empty";
    }

    if (terms.size() == 1) {
      return terms.get(0).toString();
    }

    final String separator = ", ";
    final StringBuilder builder = new StringBuilder();

    boolean comma = false;

    builder.append("{");
    for (final IntegerField field : terms) {
      if (comma) {
        builder.append(separator);
      }
      builder.append(field);
      comma = true;
    }
    builder.append("}");

    return builder.toString();
  }
}
