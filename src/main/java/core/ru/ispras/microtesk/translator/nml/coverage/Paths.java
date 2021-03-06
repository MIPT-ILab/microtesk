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

package ru.ispras.microtesk.translator.nml.coverage;

import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.solver.constraint.Constraint;

import java.util.Iterator;

final class Paths implements Iterable<Constraint> {
  private final PathConstraintBuilder builder;
  private final Iterable<? extends Node> conditions;

  public Paths(final PathConstraintBuilder builder, final Iterable<? extends Node> conditions) {
    this.builder = builder;
    this.conditions = conditions;
  }

  @Override
  public Iterator<Constraint> iterator() {
    return new PathIterator(builder, conditions.iterator());
  }

  private static final class PathIterator implements Iterator<Constraint> {
    private final PathConstraintBuilder builder;
    private final Iterator<? extends Node> conditions;

    private PathIterator(
        final PathConstraintBuilder builder,
        final Iterator<? extends Node> conditions) {
      this.builder = builder;
      this.conditions = conditions;
    }

    @Override
    public boolean hasNext() {
      return conditions.hasNext();
    }

    @Override
    public Constraint next() {
      return builder.build(conditions.next());
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
