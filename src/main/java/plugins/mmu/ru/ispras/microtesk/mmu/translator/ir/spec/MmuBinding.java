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

import ru.ispras.fortress.util.BitUtils;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.basis.solver.integer.IntegerField;
import ru.ispras.microtesk.basis.solver.integer.IntegerVariable;
import ru.ispras.microtesk.mmu.basis.MemoryAccessContext;

/**
 * {@link MmuBinding} describes an assignment, i.e. a pair of the kind {@code lhs = rhs},
 * where {@code lhs} is an {@link IntegerField} and {@code rhs} is an {@link MmuExpression}.
 * 
 * <p>The right-hand side of the assignment is allowed to be {@code null}. It means that the
 * expression can be derived from the context.</p>
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class MmuBinding {
  /** Left-hand side. */
  private final IntegerField lhs;
  /** Right-hand side or {@code null}. */
  private final MmuExpression rhs;

  public MmuBinding(final IntegerField lhs, final MmuExpression rhs) {
    InvariantChecks.checkNotNull(lhs);
    InvariantChecks.checkNotNull(rhs);

    this.lhs = lhs;
    this.rhs = rhs;
  }

  public MmuBinding(final IntegerField lhs, final IntegerField rhs) {
    this(lhs, MmuExpression.field(rhs));
  }

  public MmuBinding(final IntegerField lhs, final IntegerVariable rhs) {
    this(lhs, MmuExpression.var(rhs));
  }

  public MmuBinding(final IntegerField lhs, final BigInteger rhs) {
    this(
        lhs,
        MmuExpression.val(
            BitUtils.getField(rhs, 0, lhs != null ? lhs.getWidth() - 1 : 0),
            lhs != null ? lhs.getWidth() : 0
        )
    );
  }

  public MmuBinding(final IntegerVariable lhs, final MmuExpression rhs) {
    this(new IntegerField(lhs), rhs);
  }

  public MmuBinding(final IntegerVariable lhs, final IntegerField rhs) {
    this(lhs, MmuExpression.field(rhs));
  }

  public MmuBinding(final IntegerVariable lhs, final IntegerVariable rhs) {
    this(lhs, MmuExpression.var(rhs));
  }

  public MmuBinding(final IntegerVariable lhs, final BigInteger rhs) {
    this(new IntegerField(lhs), rhs);
  }

  public MmuBinding(final IntegerField lhs) {
    InvariantChecks.checkNotNull(lhs);

    this.lhs = lhs;
    this.rhs = null;
  }

  public MmuBinding(final IntegerVariable lhs) {
    this(new IntegerField(lhs));
  }

  public IntegerField getLhs() {
    return lhs;
  }

  public MmuExpression getRhs() {
    return rhs;
  }

  public MmuBinding getInstance(
      final String lhsInstanceId,
      final String rhsInstanceId,
      final MemoryAccessContext context) {
    InvariantChecks.checkNotNull(context);

    return new MmuBinding(
        context.getInstance(lhsInstanceId, lhs),
        rhs != null ? rhs.getInstance(rhsInstanceId, context) : null);
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder(lhs.toString());

    if (rhs != null) {
      builder.append("=");
      builder.append(rhs);
    }

    return builder.toString();
  }
}
