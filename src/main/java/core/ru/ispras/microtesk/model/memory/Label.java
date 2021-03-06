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

package ru.ispras.microtesk.model.memory;

import ru.ispras.fortress.util.InvariantChecks;

/**
 * The {@link Label} class labels some memory location with the provided identifier.
 *
 * <p>This is required to provide an external access to this location using
 * an architecture-independent identifier. For example, a register storing
 * program counter value is marked as PC to allow the external engines that
 * have no knowledge about the architecture to access it.
 *
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class Label {
  private final String name;
  private final Memory memory;
  private final int index;

  public Label(final String name, final Memory memory) {
    this(name, memory, 0);
  }

  public Label(final String name, final Memory memory, final int index) {
    InvariantChecks.checkNotNull(name);
    InvariantChecks.checkNotNull(memory);

    this.name = name;
    this.memory = memory;
    this.index = index;
  }

  public String getName() {
    return name;
  }

  public Location access() {
    return memory.access(index);
  }
}
