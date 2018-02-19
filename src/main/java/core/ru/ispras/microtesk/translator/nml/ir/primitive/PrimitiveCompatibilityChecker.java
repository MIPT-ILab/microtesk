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

package ru.ispras.microtesk.translator.nml.ir.primitive;

import ru.ispras.microtesk.translator.antlrex.SemanticException;
import ru.ispras.microtesk.translator.antlrex.symbols.Where;
import ru.ispras.microtesk.translator.nml.antlrex.WalkerContext;
import ru.ispras.microtesk.translator.nml.antlrex.WalkerFactoryBase;
import ru.ispras.microtesk.translator.nml.ir.shared.Type;

import java.util.Set;

final class PrimitiveCompatibilityChecker extends WalkerFactoryBase {
  private static final String COMMON_ERROR =
      "The %s primitive cannot be a part of the %s OR-rule. Reason: ";

  private static final String TYPE_MISMATCH_ERROR =
      COMMON_ERROR + "return type mismatch.";

  private static final String SIZE_MISMATCH_ERROR =
      COMMON_ERROR + "return type size mismatch.";

  private static final String ATTRIBUTE_MISMATCH_ERROR =
      COMMON_ERROR + "sets of attributes do not match (expected: %s, current: %s).";

  private final Where where;
  private final String name;
  private final Primitive current;
  private final Primitive expected;

  public PrimitiveCompatibilityChecker(
      final WalkerContext context,
      final Where where,
      final String name,
      final Primitive current,
      final Primitive expected) {
    super(context);

    this.where = where;
    this.name = name;
    this.current = current;
    this.expected = expected;
  }

  public void check() throws SemanticException {
    checkReturnTypes();
    checkAttributes();
  }

  private void checkReturnTypes() throws SemanticException {
    final Type currentType = current.getReturnType();
    final Type expectedType = expected.getReturnType();

    if (currentType == expectedType) {
      return;
    }

    checkType(currentType, expectedType);
    checkSize(currentType, expectedType);
  }

  private void checkType(final Type currentType, final Type expectedType) throws SemanticException {
    if ((null != expectedType) && (null != currentType)) {
      if (expectedType.getTypeId() == currentType.getTypeId()) {
        return;
      }

      if (currentType.getTypeId().isInteger() && expectedType.getTypeId().isInteger()) {
        return;
      }
    }

    raiseError(where, String.format(TYPE_MISMATCH_ERROR, current.getName(), name));
  }

  private void checkSize(final Type currentType, final Type expectedType) throws SemanticException {
    if ((null != expectedType) && (null != currentType)) {
      if (currentType.getBitSize() == expectedType.getBitSize()) {
        return;
      }
    }

    raiseError(where, String.format(SIZE_MISMATCH_ERROR, current.getName(), name));
  }

  private void checkAttributes() throws SemanticException {
    final Set<String> expectedAttrs = expected.getAttrNames();
    final Set<String> currentAttrs = current.getAttrNames();

    if (expectedAttrs == currentAttrs) {
      return;
    }

    if ((null != expectedAttrs) && (null != currentAttrs)) {
      if (expectedAttrs.equals(currentAttrs)) {
        return;
      }
    }

    raiseError(where, String.format(
        ATTRIBUTE_MISMATCH_ERROR, current.getName(), name, expectedAttrs, currentAttrs));
  }
}
