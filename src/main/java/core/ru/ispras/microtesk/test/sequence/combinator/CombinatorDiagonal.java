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

package ru.ispras.microtesk.test.sequence.combinator;

import ru.ispras.testbase.knowledge.iterator.Iterator;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link CombinatorDiagonal} implements the diagonal combinator of iterators.
 *
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class CombinatorDiagonal<T> extends CombinatorBase<T> {
  /** The set of exhausted iterators. */
  private final Set<Integer> exhausted = new HashSet<Integer>();

  @Override
  public void onInit() {
    exhausted.clear();
  }

  @Override
  public T getValue(final int i) {
    final Iterator<T> iterator = iterators.get(i);

    return iterator.hasValue() ? iterator.value() : null;
  }

  @Override
  public boolean doNext() {
    for (int i = 0; i < iterators.size(); i++) {
      Iterator<T> iterator = iterators.get(i);

      iterator.next();

      if (!iterator.hasValue()) {
        exhausted.add(i);

        if (exhausted.size() < iterators.size()) {
          iterator.init();
        } else {
          return false;
        }
      }
    }

    return true;
  }
}
