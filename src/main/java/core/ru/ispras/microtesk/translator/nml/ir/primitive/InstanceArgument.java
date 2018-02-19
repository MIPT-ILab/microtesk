/*
 * Copyright 2014-2015 ISP RAS (http://www.ispras.ru)
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

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.translator.nml.ir.expr.Expr;

public final class InstanceArgument {

  public static enum Kind {
    INSTANCE (Instance.class),
    EXPR (Expr.class),
    PRIMITIVE (Primitive.class);

    private final Class<?> valueClass;
    private Kind(final Class<?> valueClass) {
      this.valueClass = valueClass;
    }
  }

  public static InstanceArgument newInstance(final Instance instance) {
    return new InstanceArgument(Kind.INSTANCE, instance, null);
  }

  public static InstanceArgument newExpr(final Expr expr) {
    return new InstanceArgument(Kind.EXPR, expr, null);
  }

  public static InstanceArgument newPrimitive(final String name, final Primitive type) {
    return new InstanceArgument(Kind.PRIMITIVE, type, name);
  }

  private final Kind kind;
  private final Object value;
  private final String name;

  private InstanceArgument(final Kind kind, final Object value, final String name) {
    InvariantChecks.checkNotNull(kind);
    InvariantChecks.checkNotNull(value);

    if (!kind.valueClass.isAssignableFrom(value.getClass())) {
      throw new IllegalArgumentException();
    }

    this.kind = kind;
    this.value = value;
    this.name = name;
  }

  public Kind getKind() {
    return kind;
  }

  public String getTypeName() {
    switch (kind) {
      case EXPR:
        return getExpr().getNodeInfo().getType().getJavaText();
      case PRIMITIVE:
        return getPrimitive().getName();
      case INSTANCE:
        return getInstance().getPrimitive().getName();
    }
    InvariantChecks.checkTrue(false);
    return null;
  }

  public Expr getExpr() {
    return (Expr) getValueIfAssignable(Expr.class);
  }

  public Instance getInstance() {
    return (Instance) getValueIfAssignable(Instance.class);
  }

  public Primitive getPrimitive() {
    return (Primitive) getValueIfAssignable(Primitive.class);
  }

  public String getName() {
    return name;
  }

  private Object getValueIfAssignable(final Class<?> targetClass) {
    if (!targetClass.isAssignableFrom(value.getClass())) {
      throw new IllegalStateException(value.getClass().getName());
    }

    return value;
  }
}
