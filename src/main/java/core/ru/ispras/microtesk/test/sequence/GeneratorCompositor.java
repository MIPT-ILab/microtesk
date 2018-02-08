/*
 * Copyright 2013-2018 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.test.sequence;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.test.sequence.combinator.Combinator;
import ru.ispras.microtesk.test.sequence.compositor.Compositor;
import ru.ispras.testbase.knowledge.iterator.CollectionIterator;
import ru.ispras.testbase.knowledge.iterator.Iterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link GeneratorCompositor} implements the test sequence generator.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class GeneratorCompositor<T> implements Generator<T> {
  /** Produces different combinations of sequences. */
  private final Combinator<List<T>> combinator;
  /** Merges several sequences into one. */
  private final Compositor<T> compositor;

  /** The list of iterators. */
  private final List<Iterator<List<T>>> iterators;

  public GeneratorCompositor(
      final Combinator<List<T>> combinator,
      final Compositor<T> compositor,
      final List<Iterator<List<T>>> iterators) {
    InvariantChecks.checkNotNull(combinator);
    InvariantChecks.checkNotNull(compositor);
    InvariantChecks.checkNotNull(iterators);

    this.combinator = combinator;
    this.compositor = compositor;
    this.iterators = iterators;
  }

  @Override
  public void init() {
    combinator.initialize(iterators);
    combinator.init();
  }

  @Override
  public boolean hasValue() {
    return combinator.hasValue();
  }

  @Override
  public List<T> value() {
    final List<List<T>> combination = combinator.value();
    if (combination.isEmpty()) {
      return Collections.emptyList();
    }

    final List<Iterator<T>> iterators = new ArrayList<>();
    for (final List<T> sequence : combination) {
      iterators.add(new CollectionIterator<T>(sequence));
    }

    compositor.initialize(iterators);

    final List<T> sequence = new ArrayList<T>();
    for (compositor.init(); compositor.hasValue(); compositor.next()) {
      sequence.add(compositor.value());
    }

    return sequence;
  }

  @Override
  public void next() {
    combinator.next();
  }

  @Override
  public void stop() {
    combinator.stop();
  }

  @Override
  public Iterator<List<T>> clone() {
    throw new UnsupportedOperationException();
  }
}
