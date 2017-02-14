/*
 * Copyright 2015-2017 ISP RAS (http://www.ispras.ru)
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

import java.util.HashMap;
import java.util.Map;

import ru.ispras.fortress.util.InvariantChecks;

/**
 * The {@link BufferPreparatorStore} class stores a collection of buffer preparators.
 * 
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class BufferPreparatorStore {
  private final Map<String, BufferPreparator> preparators;

  public BufferPreparatorStore() {
    this.preparators = new HashMap<>();
  }

  public void addPreparator(final BufferPreparator preparator) {
    InvariantChecks.checkNotNull(preparator);
    preparators.put(preparator.getBufferId(), preparator);
  }

  public BufferPreparator getPreparatorFor(final String bufferId) {
    InvariantChecks.checkNotNull(bufferId);
    return preparators.get(bufferId);
  }
}
