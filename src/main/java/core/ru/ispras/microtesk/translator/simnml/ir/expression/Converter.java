/*
 * Copyright 2014 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.translator.simnml.ir.expression;

import java.math.BigInteger;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import ru.ispras.fortress.data.Data;
import ru.ispras.fortress.data.DataType;
import ru.ispras.fortress.expression.StandardOperation;
import ru.ispras.microtesk.model.api.type.TypeId;
import ru.ispras.microtesk.translator.simnml.ir.expression.Operator;
import ru.ispras.microtesk.translator.simnml.ir.valueinfo.ValueInfo;
import ru.ispras.microtesk.translator.simnml.ir.shared.Type;

/**
 * Contains methods that perform conversion from MicroTESK data types and operators to Fortress data
 * types and operators.
 * 
 * @author Andrei Tatarnikov
 */

public final class Converter {
  private Converter() {}

  private static final class OperatorInfo {
    private final Enum<?> nativeOp;
    private final Enum<?> modelOp;

    OperatorInfo(Enum<?> nativeOp, Enum<?> modelOp) {
      this.nativeOp = nativeOp;
      this.modelOp = modelOp;
    }

    Enum<?> getNative() {
      return nativeOp;
    }

    Enum<?> getModel() {
      return modelOp;
    }
  }

  private static final Map<Operator, OperatorInfo> operators = createOperators();

  private static Map<Operator, OperatorInfo> createOperators() {
    final Map<Operator, OperatorInfo> result = new EnumMap<Operator, OperatorInfo>(Operator.class);

    result.put(Operator.OR, new OperatorInfo(StandardOperation.OR, StandardOperation.BVOR));
    result.put(Operator.AND, new OperatorInfo(StandardOperation.AND, StandardOperation.BVAND));

    result.put(Operator.BIT_OR, new OperatorInfo(StandardOperation.OR, StandardOperation.BVOR));
    result.put(Operator.BIT_XOR, new OperatorInfo(StandardOperation.XOR, StandardOperation.BVXOR));
    result.put(Operator.BIT_AND, new OperatorInfo(StandardOperation.AND, StandardOperation.BVAND));

    result.put(Operator.EQ, new OperatorInfo(StandardOperation.EQ, StandardOperation.EQ));
    result.put(Operator.NOT_EQ, new OperatorInfo(StandardOperation.NOTEQ, StandardOperation.NOTEQ));

    result.put(Operator.LEQ, new OperatorInfo(StandardOperation.LESSEQ, StandardOperation.BVULE));
    result.put(Operator.GEQ, new OperatorInfo(StandardOperation.GREATEREQ, StandardOperation.BVUGE));
    result.put(Operator.LESS, new OperatorInfo(StandardOperation.LESS, StandardOperation.BVULT));
    result.put(Operator.GREATER, new OperatorInfo(StandardOperation.GREATER, StandardOperation.BVUGT));

    result.put(Operator.L_SHIFT, new OperatorInfo(StandardOperation.BVLSHL, StandardOperation.BVLSHL));
    result.put(Operator.R_SHIFT, new OperatorInfo(StandardOperation.BVLSHR, StandardOperation.BVLSHR));
    result.put(Operator.L_ROTATE, new OperatorInfo(StandardOperation.BVROL, StandardOperation.BVROL));
    result.put(Operator.R_ROTATE, new OperatorInfo(StandardOperation.BVROL, StandardOperation.BVROR));

    result.put(Operator.PLUS, new OperatorInfo(StandardOperation.ADD, StandardOperation.BVADD));
    result.put(Operator.MINUS, new OperatorInfo(StandardOperation.SUB, StandardOperation.BVSUB));

    result.put(Operator.MUL, new OperatorInfo(StandardOperation.MUL, StandardOperation.BVMUL));
    result.put(Operator.DIV, new OperatorInfo(StandardOperation.DIV, StandardOperation.DIV));
    result.put(Operator.MOD, new OperatorInfo(StandardOperation.MOD, StandardOperation.BVSMOD));

    result.put(Operator.POW, new OperatorInfo(StandardOperation.POWER, null));

    result.put(Operator.UPLUS, new OperatorInfo(StandardOperation.PLUS, null));
    result.put(Operator.UMINUS, new OperatorInfo(StandardOperation.MINUS, StandardOperation.BVNEG));
    result.put(Operator.BIT_NOT, new OperatorInfo(null, StandardOperation.BVNOT));
    result.put(Operator.NOT, new OperatorInfo(StandardOperation.NOT, null));

    result.put(Operator.ITE, new OperatorInfo(null, null));

    return Collections.unmodifiableMap(result);
  }

  /**
   * Creates a Fortress data object basing a value information object.
   * 
   * @param valueInfo Source value information object.
   * @return Fortress data object.
   * 
   * @throws NullPointerException if the parameter is null.
   * @throws IllegalArgumentException if the conversion is not supported to the given data type.
   */

  public static Data toFortressData(ValueInfo valueInfo) {
    checkValueInfo(valueInfo);

    if (valueInfo.isModel()) {
      return dataFromModel(valueInfo.getModelType());
    }

    return valueInfo.isConstant() ?
      dataFromNative(valueInfo.getNativeValue()) :
      dataFromNativeType(valueInfo.getNativeType());
  }

  /**
   * Returns a Fortress operator that corresponds to the specified operator applied to the specified
   * value information object.
   * 
   * @param operator Operator identifier.
   * @param valueInfo Value information object that describes the type of the operator's operands.
   * @return Fortress operator identifier.
   * 
   * @throws NullPointerException if any of the parameters is null.
   * @throws IllegalArgumentException if conversion is not supported.
   */

  static Enum<?> toFortressOperator(Operator operator, ValueInfo valueInfo) {
    checkNotNull(operator);
    checkValueInfo(valueInfo);

    final OperatorInfo oi = operators.get(operator);

    if (null == oi) {
      throw new IllegalArgumentException(String.format(ERR_UNSUPPORTED_OP, operator));
    }

    final Enum<?> result = valueInfo.isModel() ? oi.getModel() : oi.getNative();
    if (null == result) {
      throw new IllegalArgumentException(String.format(
        ERR_UNSUPPORTED_FOR, operator, valueInfo.getTypeName()));
    }

    return result;
  }

  private static Data dataFromModel(Type type) {
    checkNotNull(type);

    final DataType dataType = getDataTypeForModel(type);
    return dataType.valueUninitialized();
  }

  private static Data dataFromNative(Object value) {
    checkNotNull(value);

    // FIXME: TEMPORARY FIX. MAY CAUSE INCORRECT WORK
    // BECAUSE OF VALUE TRUNCATION. LONG IS NOT CURRENTY
    // SUPPORTED BY FORTRESS.
    
    if (Integer.class == value.getClass()) {
      return Data.newInteger((Integer) value);
    }

    if (Long.class == value.getClass()) {
      return Data.newInteger((Long) value);
    }

    final DataType dataType = getDataTypeForNative(value.getClass());
    return new Data(dataType, value);
  }

  private static Data dataFromNativeType(Class<?> type) {
    checkNotNull(type);

    final DataType dataType = getDataTypeForNative(type);
    return dataType.valueUninitialized();
  }

  public static DataType getDataTypeForModel(Type type) {
    final TypeId typeId = type.getTypeId();

    final Set<TypeId> supportedTypes = EnumSet.of(TypeId.INT, TypeId.CARD, TypeId.FLOAT);

    if (!supportedTypes.contains(typeId)) {
      throw new IllegalArgumentException(String.format(ERR_UNSUPPORTED_TYPE, typeId));
    }

    final int bitSize = type.getBitSize();
    return DataType.BIT_VECTOR(bitSize);
  }

  private static DataType getDataTypeForNative(Class<?> type) {
    if (Integer.class == type || Long.class == type || BigInteger.class == type) {
      return DataType.INTEGER;
    }

    if (Boolean.class == type) {
      return DataType.BOOLEAN;
    }

    throw new IllegalArgumentException(String.format(ERR_UNSUPPORTED_TYPE, type.getSimpleName()));
  }

  private static void checkNotNull(Object o) {
    if (null == o) {
      throw new NullPointerException();
    }
  }

  private static void checkValueInfo(ValueInfo valueInfo) {
    checkNotNull(valueInfo);

    if (valueInfo.isModel() || valueInfo.isNative()) {
      return;
    }

    throw new IllegalArgumentException(String.format(ERR_UNKNOWN_KIND, valueInfo.getValueKind()));
  }

  private static final String ERR_UNKNOWN_KIND = "Unknown kind: %s.";
  private static final String ERR_UNSUPPORTED_TYPE = "Unsupported type: %s.";
  private static final String ERR_UNSUPPORTED_OP = "Unsupported operator: %s.";
  private static final String ERR_UNSUPPORTED_FOR = "The %s operator is not supported for %s.";
}
