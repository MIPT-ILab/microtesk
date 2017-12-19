/*
 * Copyright 2014-2016 ISP RAS (http://www.ispras.ru)
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

import java.math.BigInteger;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.microtesk.utils.SharedObject;

public class LazyValue extends SharedObject<LazyValue> implements Value {
  private static final LazyData DATA = new LazyData();
  static {
    DATA.setValue(BitVector.valueOf(0, 1));
  }

  private static class LazyAddress extends LazyValue {
    @Override
    public LazyValue newCopy() {
      publishSharedCopy(this, this);
      return this;
    }

    @Override
    public Value copy() {
      return newCopy();
    }

    protected LazyAddress() {
      super(DATA);
    }
  }

  public static final LazyValue ADDRESS = new LazyAddress();

  private final LazyData data;
  private final int start;
  private final int size;

  protected LazyValue(final LazyData data) {
    InvariantChecks.checkNotNull(data);

    this.data = data;
    this.start = 0;
    this.size = 0; // This means that we will use all data.
  }

  protected LazyValue(final LazyData data, final int start, final int end) {
    InvariantChecks.checkNotNull(data);

    if ((start < 0) || (end < 0)) {
      throw new IllegalArgumentException();
    }

    this.data = data;
    this.start = Math.min(start, end);
    this.size = Math.abs(end - start) + 1;
  }

  protected LazyValue(final LazyValue other) {
    super(other);

    this.data = new LazyData(other.data);
    this.start = other.start;
    this.size = other.size;
  }

  @Override
  public LazyValue newCopy() {
    return new LazyValue(this);
  }

  @Override
  public Value copy() {
    return newCopy();
  }

  public BitVector asBitVector() {
    final BitVector value = data.getValue();

    if (null == value) {
      throw new IllegalStateException("LazyData does not have a value.");
    }

    if (start == 0 && size == 0) {
      return value; // All data is used.
    }

    final BitVector mapping = BitVector.newMapping(value, start, size);
    return mapping;
  }

  @Override
  public BigInteger getValue() {
    return asBitVector().bigIntegerValue(false);
  }

  @Override
  public String toString() {
    return null != data.getValue() ? getValue().toString() : "LazyValue";
  }
}
