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

package ru.ispras.microtesk.test.sequence.rearranger;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.testbase.knowledge.iterator.Iterator;

/**
 * The {@link RearrangerExpand} rearranger concatenates the sequences
 * returned by the original iterator into a single sequence.
 * 
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 *
 * @param <T> Sequence item type.
 */
public final class RearrangerExpand<T> implements Rearranger<T> {
  private List<T> sequence = null;
  private boolean hasValue = false;

  @Override
  public void initialize(final Iterator<List<T>> original) {
    InvariantChecks.checkNotNull(original);
    this.sequence = expand(original);
  }

  private static <T> List<T> expand(final Iterator<List<T>> iterator) {
    final List<T> result = new ArrayList<T>();

    iterator.init();
    while (iterator.hasValue()) {
      result.addAll(iterator.value());
      iterator.next();
    }

    return result;
  }

  @Override
  public void init() {
    hasValue = true;
  }

  @Override
  public boolean hasValue() {
    return hasValue;
  }

  @Override
  public List<T> value() {
    if (!hasValue) {
      throw new NoSuchElementException();
    }

    InvariantChecks.checkNotNull(sequence);
    return sequence;
  }

  @Override
  public void next() {
    hasValue = false;
  }

  @Override
  public void stop() {
    hasValue = false;
  }

  @Override
  public Iterator<List<T>> clone() {
    throw new UnsupportedOperationException();
  }
}