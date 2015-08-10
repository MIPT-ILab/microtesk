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

package ru.ispras.microtesk.mmu.translator.ir;

import static ru.ispras.fortress.util.InvariantChecks.checkNotNull;

public final class StmtMark extends Stmt {
  private final String name;

  public StmtMark(final String name) {
    super(Kind.MARK);

    checkNotNull(name);
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return String.format("stmt mark[\"%s\"]", name);
  }
}
