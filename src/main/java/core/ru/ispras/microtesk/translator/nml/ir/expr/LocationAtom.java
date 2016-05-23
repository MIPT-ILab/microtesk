/*
 * Copyright 2013-2014 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.translator.nml.ir.expr;

import static ru.ispras.fortress.util.InvariantChecks.checkNotNull;
import ru.ispras.microtesk.translator.nml.ir.expr.Expr;
import ru.ispras.microtesk.translator.nml.ir.primitive.Primitive;
import ru.ispras.microtesk.translator.nml.ir.shared.MemoryExpr;
import ru.ispras.microtesk.translator.nml.ir.shared.Type;

public final class LocationAtom implements Location {

  public static final class Bitfield {
    private final Expr from;
    private final Expr to;
    private final Type type;

    private Bitfield(Expr from, Expr to, Type type) {
      checkNotNull(from);
      checkNotNull(to);
      checkNotNull(type);

      this.from = from;
      this.to = to;
      this.type = type;
    }

    public Expr getFrom() {
      return from;
    }

    public Expr getTo() {
      return to;
    }

    public Type getType() {
      return type;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      final Bitfield other = (Bitfield) obj;
      if (!type.equals(other.getType())) {
        return false;
      }

      return from.equals(other.from) && to.equals(other.to);
    }
  }

  private final String name;
  private final LocationSource source;
  private final Type type;
  private final Expr index;
  private final Bitfield bitfield;

  private LocationAtom(
      final String name,
      final LocationSource source,
      final Expr index,
      final Bitfield bitfield) {
    checkNotNull(name);
    checkNotNull(source);

    this.name = name;
    this.source = source;
    this.index = index;
    this.bitfield = bitfield;

    final Type sourceType = null != bitfield ? bitfield.getType() : source.getType();
    this.type = sourceType;
  }

  public static LocationAtom createMemoryBased(String name, MemoryExpr memory, Expr index) {
    return new LocationAtom(
      name,
      new LocationSourceMemory(memory),
      index,
      null
    );
  }

  public static LocationAtom createPrimitiveBased(String name, Primitive primitive) {
    return new LocationAtom(
      name,
      new LocationSourcePrimitive(primitive),
      null,
      null
    );
  }

  public static LocationAtom createBitfield(LocationAtom location, Expr from, Expr to, Type type) {
    return new LocationAtom(
      location.getName(),
      location.getSource(),
      location.getIndex(),
      new Bitfield(from, to, type)
    );
  }

  public String getName() {
    return name;
  }

  public LocationSource getSource() {
    return source;
  }

  @Override
  public Type getType() {
    return type;
  }

  public Expr getIndex() {
    return index;
  }

  public Bitfield getBitfield() {
    return bitfield;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    final LocationAtom other = (LocationAtom) obj;
    if (!name.equals(other.getName())) {
      return false;
    }

    if (!source.equals(other.source)) {
      return false;
    }

    if (null != index && !index.equals(other.index)) {
      return false;
    }

    if (null != bitfield && !bitfield.equals(other.bitfield)) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(name);

    if (index != null) {
      sb.append(String.format("[%s]", index.getNode()));
    }

    if (bitfield != null) {
      sb.append(String.format("<%s..%s>", bitfield.getFrom().getNode(), bitfield.getTo().getNode()));
    }

    return sb.toString();
  }
}
