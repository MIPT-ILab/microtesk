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

package ru.ispras.microtesk.translator.nml.ir.primitive;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.translator.nml.ir.expr.Expr;

import java.util.List;

public final class StatementCondition extends Statement {
  public static final class Block {
    private final Expr condition;
    private final List<Statement> statements;

    private Block(final Expr condition, final List<Statement> statements) {
      InvariantChecks.checkNotNull(statements);

      this.condition = condition;
      this.statements = statements;
    }

    public static Block newIfBlock(final Expr condition, final List<Statement> statements) {
      InvariantChecks.checkNotNull(condition);
      return new Block(condition, statements);
    }

    public static Block newElseBlock(final List<Statement> statements) {
      return new Block(null, statements);
    }

    public Expr getCondition() {
      return condition;
    }

    public boolean isElseBlock() {
      return null == condition;
    }

    public List<Statement> getStatements() {
      return statements;
    }
  }

  private final List<Block> blocks;

  StatementCondition(final List<Block> blocks) {
    super(Kind.COND);

    InvariantChecks.checkNotEmpty(blocks);
    this.blocks = blocks;
  }

  public int getBlockCount() {
    return blocks.size();
  }

  public Block getBlock(final int index) {
    InvariantChecks.checkBounds(index, getBlockCount());
    return blocks.get(index);
  }
}
