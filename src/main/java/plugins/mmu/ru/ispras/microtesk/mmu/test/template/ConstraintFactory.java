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

package ru.ispras.microtesk.mmu.test.template;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ru.ispras.fortress.randomizer.Variate;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.basis.solver.integer.IntegerConstraint;
import ru.ispras.microtesk.basis.solver.integer.IntegerDomainConstraint;
import ru.ispras.microtesk.basis.solver.integer.IntegerVariable;
import ru.ispras.microtesk.basis.solver.integer.IntegerField;
import ru.ispras.microtesk.mmu.MmuPlugin;
import ru.ispras.microtesk.mmu.basis.BufferAccessEvent;
import ru.ispras.microtesk.mmu.basis.BufferEventConstraint;
import ru.ispras.microtesk.mmu.basis.MemoryAccessConstraints;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuBuffer;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuSubsystem;
import ru.ispras.microtesk.test.GenerationAbortedException;

/**
 * The {@link ConstraintFactory} class is used by test templates to
 * create memory-related constraints from Ruby code.
 * 
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class ConstraintFactory {
  private static ConstraintFactory instance = null;

  private ConstraintFactory() {}

  public static ConstraintFactory get() {
    if (null == instance) {
      instance = new ConstraintFactory();
    }
    return instance;
  }

  public IntegerConstraint<IntegerField> newEqValue(
      final String variableName,
      final BigInteger value) {
    InvariantChecks.checkNotNull(value);

    final IntegerField variable = getVariable(variableName);
    return new IntegerDomainConstraint<>(variable, value);
  }

  public IntegerConstraint<IntegerField> newEqRange(
      final String variableName,
      final BigInteger min,
      final BigInteger max) {
    InvariantChecks.checkNotNull(min);
    InvariantChecks.checkNotNull(max);
    InvariantChecks.checkGreaterOrEq(max, min);

    final Set<BigInteger> values = new LinkedHashSet<>();
    for (BigInteger value = min;
         value.compareTo(max) <= 0;
         value = value.add(BigInteger.ONE)) {
      values.add(value);
    }

    final IntegerField variable = getVariable(variableName);
    return new IntegerDomainConstraint<>(variable, null, values);
  }

  public IntegerConstraint<IntegerField> newEqArray(
      final String variableName,
      final BigInteger[] values) {
    InvariantChecks.checkNotNull(values);

    final IntegerField variable = getVariable(variableName);
    return new IntegerDomainConstraint<>(variable, null, new LinkedHashSet<>(Arrays.asList(values)));
  }

  public IntegerConstraint<IntegerField> newEqDist(
      final String variableName,
      final Variate<?> distribution) {
    InvariantChecks.checkNotNull(distribution);

    final IntegerField variable = getVariable(variableName);
    final Set<BigInteger> values = extractValues(distribution);

    return new IntegerDomainConstraint<>(variable, null, values);
  }

  public BufferEventConstraint newHit(final String bufferName) {
    final MmuBuffer buffer = getBuffer(bufferName);
    return new BufferEventConstraint(buffer, BufferAccessEvent.HIT);
  }

  public BufferEventConstraint newMiss(final String bufferName) {
    final MmuBuffer buffer = getBuffer(bufferName);
    return new BufferEventConstraint(buffer, BufferAccessEvent.MISS);
  }

  public BufferEventConstraint newRead(final String bufferName) {
    final MmuBuffer buffer = getBuffer(bufferName);
    return new BufferEventConstraint(buffer, BufferAccessEvent.READ);
  }

  public BufferEventConstraint newWrite(final String bufferName) {
    final MmuBuffer buffer = getBuffer(bufferName);
    return new BufferEventConstraint(buffer, BufferAccessEvent.WRITE);
  }

  public BufferEventConstraint newEvent(
      final String bufferName,
      final int hitBias,
      final int missBias) {
    InvariantChecks.checkNotNull(bufferName);

    final List<BufferAccessEvent> events = new ArrayList<>(2);
    if (0 != hitBias) {
      events.add(BufferAccessEvent.HIT);
    }

    if (0 != missBias) {
      events.add(BufferAccessEvent.MISS);
    }

    final Set<BufferAccessEvent> eventSet = events.isEmpty() ?
        EnumSet.noneOf(BufferAccessEvent.class) : EnumSet.copyOf(events);

    final MmuBuffer buffer = getBuffer(bufferName);
    final BufferEventConstraint constraint =
        new BufferEventConstraint(buffer, eventSet);

    return constraint;
  }

  @SuppressWarnings("unchecked")
  public MemoryAccessConstraints newConstraints(final Object[] constraints) {
    InvariantChecks.checkNotNull(constraints);

    final MemoryAccessConstraints.Builder builder =
        new MemoryAccessConstraints.Builder();

    for (final Object constraint : constraints) {
      InvariantChecks.checkNotNull(constraint); 

      if (constraint instanceof IntegerConstraint<?>) {
        builder.addConstraint((IntegerConstraint<IntegerField>) constraint);
      } else if (constraint instanceof BufferEventConstraint) {
        builder.addConstraint((BufferEventConstraint) constraint);
      } else {
        throw new IllegalArgumentException(
            "Unsupported constraint class: " + constraint.getClass().getName());
      }
    }

    return builder.build();
  }

  private MmuSubsystem getSpecification() {
    return MmuPlugin.getSpecification();
  }

  private IntegerField getVariable(final String name) {
    InvariantChecks.checkNotNull(name);

    final MmuSubsystem spec = getSpecification();
    final IntegerVariable variable = spec.getVariable(name);
    if (null == variable) {
      throw new GenerationAbortedException(String.format(
          "Invalid test template: variable %s is not defined in the MMU model.", name));
    }

    return new IntegerField(variable);
  }

  private MmuBuffer getBuffer(final String name) {
    InvariantChecks.checkNotNull(name);

    final MmuSubsystem spec = getSpecification();
    final MmuBuffer buffer = spec.getBuffer(name);
    if (null == buffer) {
      throw new GenerationAbortedException(String.format(
          "Invalid test template: buffer %s is not defined in the MMU model.", name));
    }

    return buffer;
  }

  private Set<BigInteger> extractValues(final Variate<?> variate) {
    final ValueExtractor extractor = new ValueExtractor();
    extractor.visit(variate);
    return extractor.getValues();
  }
}
