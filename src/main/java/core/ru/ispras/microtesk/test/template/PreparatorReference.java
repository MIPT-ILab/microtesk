/*
 * Copyright 2016-2018 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.test.template;

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.utils.SharedObject;

/**
 * The {@link PreparatorReference} class describes an invocation of a preparator
 * with a lazy value. Such an object is associated with a call which is created
 * when one preparator refers to another. The call will be replaced with a
 * sequence of calls when the value is known and a specific preparator is chosen.
 *
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class PreparatorReference {
  private final Primitive target;
  private final Value value;
  private final int valueBitSize;
  private final String preparatorName;
  private final String variantName;

  protected PreparatorReference(
      final Primitive target,
      final Value value,
      final int valueBitSize,
      final String preparatorName,
      final String variantName) {
    InvariantChecks.checkNotNull(target);
    InvariantChecks.checkNotNull(value);
    InvariantChecks.checkGreaterThanZero(valueBitSize);

    if (target.getKind() != Primitive.Kind.MODE) {
      throw new IllegalArgumentException(String.format(
          "Illegal preparator target kind: %s. An addressing mode is expected.", target.getKind()));
    }

    this.target = target;
    this.value = value;
    this.valueBitSize = valueBitSize;
    this.preparatorName = preparatorName;
    this.variantName = variantName;
  }

  protected PreparatorReference(final PreparatorReference other) {
    InvariantChecks.checkNotNull(other);

    this.target = other.target.newCopy();
    this.value = other.value instanceof SharedObject
        ? (Value)((SharedObject<?>) other.value).getCopy() : other.value.copy();

    this.valueBitSize = other.valueBitSize;
    this.preparatorName = other.preparatorName;
    this.variantName = other.variantName;
  }

  public Primitive getTarget() {
    return target;
  }

  public BitVector getValue() {
    return value instanceof LazyValue
        ? ((LazyValue) value).asBitVector()
        : BitVector.valueOf(value.getValue(), valueBitSize);
  }

  public String getPreparatorName() {
    return preparatorName;
  }

  public String getVariantName() {
    return variantName;
  }

  @Override
  public String toString() {
    return String.format(
        "%s[preparator=%s, variant=%s]",
        target.getName(),
        preparatorName,
        variantName
        );
  }
}
