/*
 * Copyright 2015-2018 ISP RAS (http://www.ispras.ru)
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

import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.util.InvariantChecks;

public final class StmtAssign extends Stmt {
  private final Node left;
  private final Node right;

  public StmtAssign(final Node left, final Node right) {
    super(Kind.ASSIGN);

    InvariantChecks.checkNotNull(left);
    InvariantChecks.checkNotNull(right);

    this.left = left;
    this.right = right;
  }

  public Node getLeft() {
    return left;
  }

  public Node getRight() {
    return right;
  }

  @Override
  public String toString() {
    return String.format("stmt assign[%s = %s]", left, right);
  }
}
