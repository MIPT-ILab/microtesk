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

package ru.ispras.microtesk.translator.nml.coverage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import ru.ispras.fortress.expression.ExprUtils;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeOperation;
import ru.ispras.fortress.expression.Nodes;
import ru.ispras.fortress.expression.StandardOperation;

final class PathFilter {
  private static final class OpHeader {
    public final Enum<?> opId;
    public int numOperands;

    public OpHeader(final Enum<?> opId) {
      this.opId = opId;
      this.numOperands = 0;
    }
  }

  private final Deque<OpHeader> headers;
  private final List<Node> operands;

  private PathFilter() {
    this.headers = new ArrayDeque<>();
    this.operands = new ArrayList<>();
  }

  private void processNode(final Node node) {
    if (ExprUtils.isOperation(node, StandardOperation.AND)) {
      processConjunction(node);
    } else if (ExprUtils.isOperation(node, StandardOperation.OR)) {
      processDisjunction(node);
    }
  }

  private void processConjunction(final Node node) {
    final NodeOperation op = (NodeOperation) node;
    push(StandardOperation.AND);

    for (final Node child : op.getOperands()) {
      processNode(child);
    }

    pop();
  }

  private void processDisjunction(final Node node) {
    final NodeOperation op = (NodeOperation) node;
    push(StandardOperation.OR);

    for (final Node child : op.getOperands()) {
      final NodeOperation branch = (NodeOperation) child;
      push(StandardOperation.AND);

      addOperand(branch.getOperand(0));
      for (int i = 1; i < branch.getOperandCount(); ++i) {
        processNode(branch.getOperand(i));
      }

      pop();
    }

    pop();
  }

  private void push(final Enum<?> op) {
    this.headers.push(new OpHeader(op));
  }

  private void addOperand(final Node operand) {
    this.operands.add(operand);
    this.headers.peek().numOperands++;
  }

  private void pop() {
    final OpHeader header = headers.pop();
    if (header.numOperands == 1) {
      headers.peek().numOperands++;
    } else if (header.numOperands > 1) {
      final List<Node> operands =
          this.operands.subList(this.operands.size() - header.numOperands,
                                this.operands.size());
      final Node op = new NodeOperation(header.opId, operands);
      operands.clear();
      addOperand(op);
    }
  }

  public static Node filter(final Node node) {
    final PathFilter filter = new PathFilter();

    filter.push(StandardOperation.AND);
    filter.processNode(node);

    return filter.operands.isEmpty() ?
        Nodes.TRUE : filter.operands.get(0);
  }
}
