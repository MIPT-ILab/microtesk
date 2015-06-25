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

package ru.ispras.microtesk.test.template;

import java.util.Collections;
import java.util.List;
import ru.ispras.fortress.util.InvariantChecks;

public final class StreamPreparator {
  private final List<Call> init;
  private final List<Call> read;
  private final List<Call> write;

  private final LazyPrimitive data;
  private final LazyPrimitive index;
  private final LazyLabel startLabel;

  protected StreamPreparator(
      final List<Call> init,
      final List<Call> read,
      final List<Call> write,
      final LazyPrimitive data,
      final LazyPrimitive index,
      final LazyLabel startLabel) {
    InvariantChecks.checkNotNull(init);
    InvariantChecks.checkNotNull(read);
    InvariantChecks.checkNotNull(write);

    InvariantChecks.checkNotNull(data);
    InvariantChecks.checkNotNull(index);
    InvariantChecks.checkNotNull(startLabel);

    this.init = Collections.unmodifiableList(init);
    this.read = Collections.unmodifiableList(read);
    this.write = Collections.unmodifiableList(write);

    this.data = data;
    this.index = index;
    this.startLabel = startLabel;
  }

  public Stream newStream(
      final Primitive dataSource,
      final Primitive indexSource,
      final String startLabel,
      final int length) {
    InvariantChecks.checkNotNull(dataSource);
    InvariantChecks.checkNotNull(indexSource);
    InvariantChecks.checkNotNull(startLabel);
    InvariantChecks.checkGreaterThanZero(length);

    this.data.setSource(dataSource);
    this.index.setSource(indexSource);
    this.startLabel.setSource(startLabel);

    // TODO: CLONE calls in init, read, write
    return new Stream(init, read, write, length);
  }

  @Override
  public String toString() {
    return String.format("StreamPreparator [data_source=%s, index_source=%s]",
        data.getName(), index.getName());
  }
}
