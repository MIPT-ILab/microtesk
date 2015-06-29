/*
 * Copyright 2015 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.test.sequence.engine;

import java.util.Collections;
import java.util.List;

import ru.ispras.fortress.util.Result;
import ru.ispras.microtesk.test.sequence.iterator.Iterator;
import ru.ispras.microtesk.test.sequence.iterator.SingleValueIterator;

/**
 * {@link EngineResult} defines result of a {@link Engine}.
 * 
 * @author <a href="mailto:kotsynyak@ispras.ru">Artem Kotsynyak</a>
 */
public final class EngineResult<T> extends Result<EngineResult.Status, Iterator<T>> {
  public static enum Status {
    OK,
    ERROR
  }

  public EngineResult(
      final EngineResult.Status status,
      final Iterator<T> result,
      final List<String> errors) {
    super(status, result, errors);
  }

  public EngineResult(final Iterator<T> result) {
    super(Status.OK, result, Collections.<String>emptyList());
  }

  public EngineResult(final T result) {
    this(new SingleValueIterator<T>(result));
  }

  public EngineResult(final List<String> errors) {
    super(Status.ERROR, null, errors);
  }

  public EngineResult(final String error) {
    this(Collections.singletonList(error));
  }
}
