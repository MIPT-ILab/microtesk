/*
 * Copyright 2016 ISP RAS (http://www.ispras.ru)
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

import java.util.ArrayList;
import java.util.List;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.testbase.knowledge.iterator.Iterator;

public final class GeneratorUtils {
  private GeneratorUtils() {}

  public static <T> List<T> expand(final Iterator<List<T>> iterator) {
    InvariantChecks.checkNotNull(iterator);

    final List<T> result = new ArrayList<>();

    iterator.init();
    while (iterator.hasValue()) {
      result.addAll(iterator.value());
      iterator.next();
    }

    return result;
  }

  public static <T> List<T> expandAll(final List<Iterator<List<T>>> iterators) {
    InvariantChecks.checkNotNull(iterators);

    final List<T> result = new ArrayList<>();

    for (final Iterator<List<T>> sequenceIterator : iterators) {
      final List<T> sequence = GeneratorUtils.expand(sequenceIterator);
      result.addAll(sequence);
    }

    return result;
  }
}
