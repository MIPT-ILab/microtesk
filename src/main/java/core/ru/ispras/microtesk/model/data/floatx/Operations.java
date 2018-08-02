/*
 * Copyright 2015-2018 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.model.data.floatx;

import ru.ispras.fortress.data.types.bitvector.BitVector;

/**
 * The {@link Operations} interface defines operations to be supported
 * for all floating-point types.
 *
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
interface Operations {
  FloatX add(final FloatX lhs, final FloatX rhs);

  FloatX sub(final FloatX lhs, final FloatX rhs);

  FloatX mul(final FloatX lhs, final FloatX rhs);

  FloatX div(final FloatX lhs, final FloatX rhs);

  FloatX rem(final FloatX lhs, final FloatX rhs);

  FloatX sqrt(final FloatX arg);

  boolean equals(final FloatX first, final FloatX second);

  int compare(final FloatX first, final FloatX second);

  boolean isNan(final FloatX arg);

  boolean isSignalingNan(final FloatX arg);

  FloatX round(final FloatX value);

  FloatX toFloat(final FloatX value, final Precision precision);

  BitVector toInteger(final FloatX value, final int size);

  FloatX fromInteger(final BitVector value);

  String toString(final FloatX arg);

  String toHexString(final FloatX arg);
}
