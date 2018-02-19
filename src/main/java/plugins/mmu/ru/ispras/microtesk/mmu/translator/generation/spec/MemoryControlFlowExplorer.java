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

package ru.ispras.microtesk.mmu.translator.generation.spec;

import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeOperation;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.expression.StandardOperation;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.fortress.util.Pair;
import ru.ispras.microtesk.mmu.translator.ir.AbstractStorage;
import ru.ispras.microtesk.mmu.translator.ir.Attribute;
import ru.ispras.microtesk.mmu.translator.ir.AttributeRef;
import ru.ispras.microtesk.mmu.translator.ir.Buffer;
import ru.ispras.microtesk.mmu.translator.ir.Callable;
import ru.ispras.microtesk.mmu.translator.ir.Memory;
import ru.ispras.microtesk.mmu.translator.ir.Segment;
import ru.ispras.microtesk.mmu.translator.ir.Stmt;
import ru.ispras.microtesk.mmu.translator.ir.StmtAssign;
import ru.ispras.microtesk.mmu.translator.ir.StmtCall;
import ru.ispras.microtesk.mmu.translator.ir.StmtIf;
import ru.ispras.microtesk.mmu.translator.ir.StmtReturn;

import java.util.List;
import java.util.ListIterator;

public final class MemoryControlFlowExplorer {
  private final Buffer target;

  public MemoryControlFlowExplorer(final Memory memory) {
    InvariantChecks.checkNotNull(memory);

    final Attribute read = memory.getAttribute(AbstractStorage.READ_ATTR_NAME);
    InvariantChecks.checkNotNull(read, "Undefined attribute: " + AbstractStorage.READ_ATTR_NAME);

    final Buffer readTarget = visitStmts(read.getStmts(), AbstractStorage.READ_ATTR_NAME);
    InvariantChecks.checkNotNull(readTarget,
        "Failed to determine target buffer for attribute: " + AbstractStorage.READ_ATTR_NAME);

    final Attribute write = memory.getAttribute(AbstractStorage.WRITE_ATTR_NAME);
    InvariantChecks.checkNotNull(write, "Undefined attribute: " + AbstractStorage.WRITE_ATTR_NAME);

    final Buffer writeTarget = visitStmts(write.getStmts(), AbstractStorage.WRITE_ATTR_NAME);
    InvariantChecks.checkNotNull(writeTarget,
        "Failed to determine target buffer for attribute: " + AbstractStorage.WRITE_ATTR_NAME);

    if (readTarget != writeTarget) {
      throw new IllegalStateException(String.format(
          "Invariant is violated: control flow of read and write attributes of %s "
              + "must use the same buffer for the last access. This buffer will be "
              + "recognized as target buffer. Now %s (for read) vs. %s (for write).",
          memory.getId(),
          readTarget.getId(),
          writeTarget.getId())
          );
    }

    this.target = readTarget;
  }

  public Buffer getTargetBuffer() {
    return target;
  }

  private static Buffer visitStmts(final List<Stmt> stmts, final String attrId) {
    InvariantChecks.checkNotNull(stmts);

    Buffer latestAccess = null;
    for (final Stmt stmt : stmts) {
      Buffer currentAccess = null;
      switch (stmt.getKind()) {
        case IF:
          currentAccess = visitStmtIf((StmtIf) stmt, attrId);
          break;

        case ASSIGN:
          currentAccess = visitStmtAssign((StmtAssign) stmt, attrId);
          break;

        case EXCEPT: // Other statements cannot be visited after exception
          return latestAccess;

        case MARK: // Ignored
          break;

        case TRACE: // Ignored
          break;

        case CALL:
          final StmtCall call = (StmtCall) stmt;
          latestAccess = extractBufferAccess(call.getCallee(), call.getArguments(), attrId);
          break;

        case RETURN:
          final StmtReturn ret = (StmtReturn) stmt;
          if (ret.getExpr() != null) {
            latestAccess = extractBufferAccess(ret.getExpr(), attrId);
          }
          return latestAccess;

        case ASSERT:
          // Ignored. Should not affect path to target buffer.
          break;

        default:
          throw new IllegalStateException("Unknown statement: " + stmt.getKind());
      }

      if (null != currentAccess) {
        latestAccess = currentAccess;
      }
    }

    return latestAccess;
  }

  private static Buffer visitStmtIf(final StmtIf stmt, final String attrId) {
    InvariantChecks.checkNotNull(stmt);
    Buffer latestAccess = null;

    // Flag that states that the most recent if condition was BUFFER.MISS
    boolean isIfMiss = false;

    for (final Pair<Node, List<Stmt>> block : stmt.getIfBlocks()) {
      final Node cond = block.first;
      final List<Stmt> stmts = block.second;

      isIfMiss = isBufferMiss(cond);
      if (!isIfMiss && isBufferHit(cond)) {
        // Blocks with BUFFER.HIT condition are not visited.
        continue;
      }

      final Buffer currentAccess = visitStmts(stmts, attrId);
      if (null != currentAccess) {
        latestAccess = currentAccess;
      }
    }

    // Else block is not visited if the previous condition was BUFFER.MISS
    if (isIfMiss) {
      return latestAccess;
    }

    return visitStmts(stmt.getElseBlock(), attrId);
  }

  private static boolean isBufferHit(final Node expr) {
    InvariantChecks.checkNotNull(expr);

    if (expr.getKind() != Node.Kind.VARIABLE
        && expr.getKind() != Node.Kind.OPERATION) {
      return false;
    }

    final Condition cond = Condition.extract(expr);
    for (final Node atom : cond.getAtoms()) {
      if (atom.getKind() == Node.Kind.VARIABLE && isBufferHit((NodeVariable) atom)) {
        return true;
      }
    }

    return false;
  }

  private static boolean isBufferHit(final NodeVariable var) {
    InvariantChecks.checkNotNull(var);

    if (var.getUserData() instanceof AttributeRef) {
      final AttributeRef attrRef = (AttributeRef) var.getUserData();
      if (attrRef.getTarget() instanceof Buffer
          && attrRef.getAttribute().getId().equals(AbstractStorage.HIT_ATTR_NAME)) {
        return true;
      }
    }

    return false;
  }

  private static boolean isBufferMiss(final Node expr) {
    InvariantChecks.checkNotNull(expr);

    if (expr.getKind() != Node.Kind.VARIABLE
        && expr.getKind() != Node.Kind.OPERATION) {
      return false;
    }

    final Condition cond = Condition.extract(expr);
    for (final Node atom : cond.getAtoms()) {
      if (atom.getKind() != Node.Kind.OPERATION) {
        continue;
      }

      final NodeOperation op = (NodeOperation) atom;
      if (op.getOperationId() != StandardOperation.NOT) {
        continue;
      }

      final Node notAtom = op.getOperand(0);
      if (notAtom.getKind() == Node.Kind.VARIABLE && isBufferHit((NodeVariable) notAtom)) {
        return true;
      }
    }

    return false;
  }

  private static Buffer visitStmtAssign(final StmtAssign stmt, final String attrId) {
    InvariantChecks.checkNotNull(stmt);

    final Node left = stmt.getLeft();
    final Buffer leftAccess = extractBufferAccess(left, attrId);

    final Node right = stmt.getRight();
    final Buffer rightAccess = extractBufferAccess(right, attrId);

    return rightAccess != null ? rightAccess : leftAccess;
  }

  private static Buffer extractBufferAccess(final Node expr, final String attrId) {
    InvariantChecks.checkNotNull(expr);

    if (expr.getUserData() instanceof AttributeRef) {
      final AttributeRef ref = (AttributeRef) expr.getUserData();
      if (ref.getTarget() instanceof Buffer
          && ref.getAttribute().getId().equals(attrId)) {
        return (Buffer) ref.getTarget();
      } else if (ref.getTarget() instanceof Segment) {
        final Attribute attribute = ref.getAttribute();
        return visitStmts(attribute.getStmts(), attrId);
      }
    }
    if (expr.getUserData() instanceof Callable) {
      return extractBufferAccess(
          (Callable) expr.getUserData(),
          ((NodeOperation) expr).getOperands(),
          attrId);
    }

    return null;
  }

  /** Extract latest buffer access from function call. Evaluation order is
   *  considered eager: first evaluate arguments in left-to-right order,
   *  then function body.
   */

  private static Buffer extractBufferAccess(
      final Callable callee,
      final List<Node> args,
      final String attrId) {
    final Buffer funcAccess = visitStmts(callee.getBody(), attrId);
    if (funcAccess != null) {
      return funcAccess;
    }
    final ListIterator<Node> it = args.listIterator(args.size());
    while (it.hasPrevious()) {
      final Buffer argAccess = extractBufferAccess(it.previous(), attrId);
      if (argAccess != null) {
        return argAccess;
      }
    }
    return null;
  }
}
