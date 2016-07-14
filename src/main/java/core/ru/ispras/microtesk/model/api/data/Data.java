/*
 * Copyright 2015-2016 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.model.api.data;

import java.math.BigInteger;

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.model.api.data.floatx.FloatX;

public final class Data implements Comparable<Data> {
  private final Type type;
  private final BitVector rawData;

  public Data(final BitVector rawData, final Type type) {
    this(type, rawData);
  }

  public Data(final Type type, final BitVector rawData) {
    InvariantChecks.checkNotNull(type);
    InvariantChecks.checkNotNull(rawData);
    InvariantChecks.checkTrue(rawData.getBitSize() == type.getBitSize());

    this.type = type;
    this.rawData = rawData;
  }

  public Data(final Data other) {
    InvariantChecks.checkNotNull(other);

    this.type = other.getType();
    this.rawData = other.getRawData().copy();
  }

  public Data(final Type type) {
    InvariantChecks.checkNotNull(type);

    this.type = type;
    this.rawData = BitVector.newEmpty(type.getBitSize());
  }

  public Type getType() {
    return type;
  }

  public boolean isType(final TypeId typeId) {
    return type.getTypeId() == typeId;
  }

  public int getBitSize() {
    return getRawData().getBitSize();
  }

  public BitVector getRawData() {
    return rawData;
  }

  public Data bitField(final int start, final int end) {
    InvariantChecks.checkBounds(start, getBitSize());
    InvariantChecks.checkBounds(end, getBitSize());

    if (start > end) {
      return bitField(end, start);
    }

    if ((start == 0) && (end == (getBitSize() - 1))) {
      return this;
    }

    final BitVector fieldRawData = rawData.field(start, end);
    final int fieldBitSize = fieldRawData.getBitSize();

    final Type fieldType = type.isInteger() ?
        type.resize(fieldBitSize) : Type.CARD(fieldBitSize);

    return new Data(fieldRawData, fieldType);
  }

  public Data bitField(final Data start, final Data end) {
    InvariantChecks.checkNotNull(start);
    InvariantChecks.checkTrue(start.getType().isInteger());
    InvariantChecks.checkTrue(start.getBitSize() <= Integer.SIZE);

    InvariantChecks.checkNotNull(end);
    InvariantChecks.checkTrue(end.getType().isInteger());
    InvariantChecks.checkTrue(end.getBitSize() <= Integer.SIZE);

    return bitField(start.getRawData().intValue(), end.getRawData().intValue());
  }

  public Data repeat(final int count) {
    final BitVector newRawData = rawData.repeat(count);
    return new Data(type.resize(newRawData.getBitSize()), newRawData);
  }

  public Data signExtendTo(final Type newType) {
    InvariantChecks.checkNotNull(newType);
    InvariantChecks.checkTrue(type.isInteger() && newType.isInteger());

    if (type.equals(newType)) {
      return this;
    }

    InvariantChecks.checkTrue(newType.getBitSize() >= type.getBitSize());
    final BitVector newRawData = rawData.resize(newType.getBitSize(), true);

    return new Data(newType, newRawData);
  }

  public Data zeroExtendTo(final Type newType) {
    InvariantChecks.checkNotNull(newType);
    InvariantChecks.checkTrue(type.isInteger() && newType.isInteger());

    if (type.equals(newType)) {
      return this;
    }

    InvariantChecks.checkTrue(newType.getBitSize() >= type.getBitSize());
    final BitVector newRawData = rawData.resize(newType.getBitSize(), false);

    return new Data(newType, newRawData);
  }

  public Data coerceTo(final Type newType) {
    InvariantChecks.checkNotNull(newType);
    InvariantChecks.checkTrue(type.isInteger() && newType.isInteger());

    if (type.equals(newType)) {
      return this;
    }

    // Sign extension applies only to signed integer values (TypeId.INT).
    final boolean signExt = isType(TypeId.INT);

    final BitVector newRawData = rawData.resize(newType.getBitSize(), signExt);
    return new Data(newType, newRawData);
  }

  public Data castTo(final Type newType) {
    InvariantChecks.checkNotNull(newType);

    if (type.equals(newType)) {
      return this;
    }

    InvariantChecks.checkTrue(type.getBitSize() == newType.getBitSize());
    return new Data(newType, rawData);
  }

  public Data negate() {
    return getOperations().negate(this);
  }

  public Data add(final Data other) {
    InvariantChecks.checkNotNull(other);
    InvariantChecks.checkTrue(this.type.equals(other.type));

    return getOperations().add(this, other);
  }

  public Data subtract(final Data other) {
    InvariantChecks.checkNotNull(other);
    InvariantChecks.checkTrue(this.type.equals(other.type));

    return getOperations().subtract(this, other);
  }

  public Data multiply(final Data other) {
    InvariantChecks.checkNotNull(other);
    InvariantChecks.checkTrue(this.type.equals(other.type));

    return getOperations().multiply(this, other);
  }

  public Data divide(final Data other) {
    InvariantChecks.checkNotNull(other);
    InvariantChecks.checkTrue(this.type.equals(other.type));

    return getOperations().divide(this, other);
  }

  public Data mod(final Data other) {
    InvariantChecks.checkNotNull(other);
    InvariantChecks.checkTrue(this.type.equals(other.type));

    return getOperations().mod(this, other);
  }

  public Data pow(final Data other) {
    InvariantChecks.checkNotNull(other);
    InvariantChecks.checkTrue(this.type.equals(other.type));

    return getOperations().pow(this, other);
  }

  public Data not() {
    return getOperations().not(this);
  }

  public Data and(final Data other) {
    InvariantChecks.checkNotNull(other);
    InvariantChecks.checkTrue(this.type.equals(other.type));

    return getOperations().and(this, other);
  }

  public Data or(final Data other) {
    InvariantChecks.checkNotNull(other);
    InvariantChecks.checkTrue(this.type.equals(other.type));

    return getOperations().or(this, other);
  }

  public Data xor(final Data other) {
    InvariantChecks.checkNotNull(other);
    InvariantChecks.checkTrue(this.type.equals(other.type));

    return getOperations().xor(this, other);
  }

  public Data shiftLeft(final Data amount) {
    InvariantChecks.checkNotNull(amount);
    InvariantChecks.checkTrue(amount.getType().isInteger());

    return getOperations().shiftLeft(this, amount);
  }

  public Data shiftRight(final Data amount) {
    InvariantChecks.checkNotNull(amount);
    InvariantChecks.checkTrue(amount.getType().isInteger());

    return getOperations().shiftRight(this, amount);
  }

  public Data rotateLeft(final Data amount) {
    InvariantChecks.checkNotNull(amount);
    InvariantChecks.checkTrue(amount.getType().isInteger());

    return getOperations().rotateLeft(this, amount);
  }

  public Data rotateRight(final Data amount) {
    InvariantChecks.checkNotNull(amount);
    InvariantChecks.checkTrue(amount.getType().isInteger());

    return getOperations().rotateRight(this, amount);
  }

  @Override
  public int compareTo(final Data other) {
    InvariantChecks.checkNotNull(other);
    InvariantChecks.checkTrue(this.type.equals(other.type));

    return getOperations().compare(this, other);
  }

  public Data sqrt() {
    final FloatX result = floatXValue().sqrt();
    return new Data(type, result.getData());
  }

  public boolean isNan() {
    return floatXValue().isNan();
  }

  public boolean isSignalingNan() {
    return floatXValue().isSignalingNan();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;

    result = prime * result + type.hashCode();
    result = prime * result + rawData.hashCode();

    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    final Data other = (Data) obj;
    if (!type.equals(other.type)) {
      return false;
    }

    return rawData.equals(other.rawData);
  }

  public BigInteger bigIntegerValue() {
    final boolean signed = type.getTypeId() == TypeId.INT;
    return bigIntegerValue(signed);
  }

  public BigInteger bigIntegerValue(final boolean signed) {
    return rawData.bigIntegerValue(signed);
  }

  public boolean booleanValue() {
    return !rawData.isAllReset();
  }

  public FloatX floatXValue() {
    InvariantChecks.checkTrue(type.getTypeId() == TypeId.FLOAT, "Not a float value!");
    return new FloatX(rawData, type.getFieldSize(0), type.getFieldSize(1));
  }

  @Override
  public String toString() {
    return getOperations().toString(this);
  }

  public String toHexString() {
    return getOperations().toHexString(this);
  }

  public String toBinString() {
    return rawData.toBinString();
  }

  private Operations getOperations() {
    return type.getTypeId().getOperations();
  }

  public static Data valueOf(final Type type, final BigInteger value) {
    InvariantChecks.checkNotNull(type);
    InvariantChecks.checkNotNull(value);
    return new Data(type, BitVector.valueOf(value, type.getBitSize()));
  }

  public static Data valueOf(final Type type, final long value) {
    InvariantChecks.checkNotNull(type);
    return new Data(type, BitVector.valueOf(value, type.getBitSize()));
  }

  public static Data valueOf(final Type type, final int value) {
    InvariantChecks.checkNotNull(type);
    return new Data(type, BitVector.valueOf(value, type.getBitSize()));
  }

  public static Data valueOf(final Type type, final boolean value) {
    InvariantChecks.checkNotNull(type);
    return new Data(type, BitVector.valueOf(value).resize(type.getBitSize(), false));
  }

  public static Data signExtend(final Type type, final Data value) {
    return value.signExtendTo(type);
  }

  public static Data zeroExtend(final Type type, final Data value) {
    return value.zeroExtendTo(type);
  }

  public static Data coerce(final Type type, final Data value) {
    return value.coerceTo(type);
  }

  public static Data cast(final Type type, final Data value) {
    return value.castTo(type);
  }

  public static Data intToFloat(final Type type, final Data value) {
    InvariantChecks.checkNotNull(type);
    InvariantChecks.checkNotNull(value);
    InvariantChecks.checkTrue(type.getTypeId() == TypeId.FLOAT);
    InvariantChecks.checkTrue(value.getType().getTypeId().isInteger());

    final BitVector source = value.getRawData();
    final FloatX target = FloatX.fromInteger(type.getFieldSize(0), type.getFieldSize(1), source);

    return new Data(type, target.getData());
  }

  public static Data floatToInt(final Type type, final Data value) {
    InvariantChecks.checkNotNull(type);
    InvariantChecks.checkNotNull(value);
    InvariantChecks.checkTrue(type.getTypeId().isInteger());
    InvariantChecks.checkTrue(value.isType(TypeId.FLOAT));

    final FloatX source = value.floatXValue();
    final BitVector target = source.toInteger(type.getBitSize());

    return new Data(type, target);
  }

  public static Data floatToFloat(final Type type, final Data value) {
    InvariantChecks.checkNotNull(type);
    InvariantChecks.checkNotNull(value);
    InvariantChecks.checkTrue(type.getTypeId() == TypeId.FLOAT);
    InvariantChecks.checkTrue(value.isType(TypeId.FLOAT));

    final FloatX source = value.floatXValue();
    final FloatX target = source.toFloat(type.getFieldSize(0), type.getFieldSize(1));

    return new Data(type, target.getData());
  }

  /**
   * Checks whether the significant bits are lost when the specified integer is converted to
   * the specified Model API type. This happens when the type is shorter than the value
   * and the truncated part goes beyond sign extension bits.
   * 
   * @param type Conversion target type.
   * @param value Value to be converted.
   * @return {@code true} if significant bits will be lost during the conversion
   * or {@code false} otherwise.
   */
  public static boolean isLossOfSignificantBits(final Type type, final BigInteger value) {
    InvariantChecks.checkNotNull(type);
    InvariantChecks.checkNotNull(value);

    final int valueBitSize = value.bitLength() + 1; // Minimal two's complement + sign bit
    if (type.getBitSize() >= valueBitSize) {
      return false;
    }

    final BitVector whole = BitVector.valueOf(value, valueBitSize);
    final BitVector truncated = whole.field(type.getBitSize(), whole.getBitSize() - 1);

    return !(truncated.isAllReset() ||
             whole.getBit(type.getBitSize() - 1) && truncated.isAllSet());
  }
}
