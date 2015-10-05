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

package ru.ispras.microtesk.model.api.data.fp;

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.softfloat.JSoftFloat;

final class Float64Operations implements Operations {
  private static Operations instance = null;

  public static Operations get() {
    if (null == instance) {
      instance = new Float64Operations();
    }
    return instance;
  }

  private Float64Operations() {}

  @Override
  public FloatX add(final FloatX lhs, final FloatX rhs) {
    final double value = JSoftFloat.float64_add(lhs.doubleValue(), rhs.doubleValue());
    return newFloat64(value);
  }

  @Override
  public FloatX sub(final FloatX lhs, final FloatX rhs) {
    final double value = JSoftFloat.float64_sub(lhs.doubleValue(), rhs.doubleValue());
    return newFloat64(value);
  }

  @Override
  public FloatX mul(final FloatX lhs, final FloatX rhs) {
    final double value = JSoftFloat.float64_mul(lhs.doubleValue(), rhs.doubleValue());
    return newFloat64(value);
  }

  @Override
  public FloatX div(final FloatX lhs, final FloatX rhs) {
    final double value = JSoftFloat.float64_div(lhs.doubleValue(), rhs.doubleValue());
    return newFloat64(value);
  }

  @Override
  public FloatX rem(FloatX lhs, FloatX rhs) {
    final double value = JSoftFloat.float64_rem(lhs.doubleValue(), rhs.doubleValue());
    return newFloat64(value);
  }

  @Override
  public FloatX sqrt(final FloatX arg) {
    final double value = JSoftFloat.float64_sqrt(arg.doubleValue());
    return newFloat64(value);
  }

  private static FloatX newFloat64(final double doubleData) {
    return new FloatX(
        BitVector.valueOf(Double.doubleToLongBits(doubleData), Double.SIZE),
        Precision.FLOAT64
    );
  }
}
