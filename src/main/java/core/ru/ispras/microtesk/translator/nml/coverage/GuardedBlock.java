/*
 * Copyright 2014-2017 ISP RAS (http://www.ispras.ru)
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
import ru.ispras.fortress.util.InvariantChecks;

final class GuardedBlock {
  public final String name;
  public final Node guard;
  public final Block block;

  GuardedBlock(final String name, final Node guard, final Block block) {
    InvariantChecks.checkNotNull(name);
    InvariantChecks.checkNotNull(guard);
    InvariantChecks.checkNotNull(block);

    this.name = name;
    this.guard = guard;
    this.block = block;
  }
}
