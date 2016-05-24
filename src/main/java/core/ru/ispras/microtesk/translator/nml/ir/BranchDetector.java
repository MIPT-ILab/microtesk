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

package ru.ispras.microtesk.translator.nml.ir;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import ru.ispras.fortress.expression.ExprTreeVisitorDefault;
import ru.ispras.fortress.expression.ExprTreeWalker;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.fortress.util.Pair;
import ru.ispras.microtesk.model.api.state.Status;
import ru.ispras.microtesk.translator.nml.NmlSymbolKind;
import ru.ispras.microtesk.translator.nml.ir.expr.Expr;
import ru.ispras.microtesk.translator.nml.ir.expr.Location;
import ru.ispras.microtesk.translator.nml.ir.expr.LocationAtom;
import ru.ispras.microtesk.translator.nml.ir.expr.LocationConcat;
import ru.ispras.microtesk.translator.nml.ir.expr.NodeInfo;
import ru.ispras.microtesk.translator.nml.ir.primitive.Attribute;
import ru.ispras.microtesk.translator.nml.ir.primitive.Primitive;
import ru.ispras.microtesk.translator.nml.ir.primitive.PrimitiveAND;
import ru.ispras.microtesk.translator.nml.ir.primitive.Statement;
import ru.ispras.microtesk.translator.nml.ir.primitive.StatementAssignment;
import ru.ispras.microtesk.translator.nml.ir.primitive.StatementAttributeCall;
import ru.ispras.microtesk.translator.nml.ir.primitive.StatementCondition;
import ru.ispras.microtesk.translator.nml.ir.primitive.StatementStatus;

public final class BranchDetector {
  private final Ir ir;
  private final IrInquirer inquirer;
  private final Deque<PrimitiveAND> primitives;
  private final Deque<Pair<StatementCondition, Integer>> conditions; // Condition : Case Index

  public BranchDetector(final Ir ir) {
    InvariantChecks.checkNotNull(ir);

    this.ir = ir;
    this.inquirer = new IrInquirer(ir);
    this.primitives = new ArrayDeque<>();
    this.conditions = new ArrayDeque<>();
  }

  public void start() {
    for (final Primitive item : ir.getOps().values()) {
      if (!item.isOrRule()) {
        final PrimitiveAND primitive = (PrimitiveAND) item; 
        final Attribute actionAttribute = primitive.getAttributes().get(Attribute.ACTION_NAME);
        traverse(primitive, actionAttribute.getStatements());
      }
    }
  }

  private void traverse(final PrimitiveAND primitive, final List<Statement> stmts) {
    primitives.push(primitive);
    for (final Statement stmt : stmts) {
      onStatement(stmt);
    }
    primitives.pop();
  }

  private void onStatement(final Statement stmt) {
    switch(stmt.getKind()) {
      case STATUS:
        onStatus((StatementStatus) stmt);
        break;

      case COND:
        onCondition((StatementCondition) stmt);
        break;

      case CALL:
        onAttributeCall((StatementAttributeCall) stmt);
        break;

      case ASSIGN:
        onAssignment((StatementAssignment) stmt);
        break;

      case FUNCALL: // Ignored
        break;

      case FORMAT: // Ignored
        break;

      default:
        throw new IllegalArgumentException(
            "Unknown statement kind: " + stmt.getKind());
    }
  }

  private void onAssignment(final StatementAssignment stmt) {
    final Location left = stmt.getLeft();
    final Expr right = stmt.getRight();

    if (isPCAssignment(left, right)) {
      final boolean isConditional = !conditions.isEmpty();
      final PrimitiveAND primitive =  primitives.getLast();

      if (isConditional) {
        primitive.setConditionalBranch(true);
      } else {
        primitive.setBranch(true);
      }

      //System.out.printf("[Assign] %s - %sbranch%n",
      //     primitive.getName(), isConditional ? "conditional " : "");
    }
  }

  private void onStatus(final StatementStatus stmt) {
    /*
    if (stmt.getStatus() == Status.CTRL_TRANSFER) {
      final boolean isConditional = !conditions.isEmpty();
      final PrimitiveAND primitive =  primitives.getLast();

      if (isConditional) {
        primitive.setConditionalBranch(true);
      } else {
        primitive.setBranch(true);
      }

      //System.out.printf("[Status] %s - %sbranch%n",
      //    primitive.getName(), isConditional ? "conditional " : "");
    }
    */
  }

  private void onAttributeCall(final StatementAttributeCall stmt) {
    if (stmt.getCalleeInstance() != null) {
      final PrimitiveAND primitive = stmt.getCalleeInstance().getPrimitive();
      final Attribute attribute = primitive.getAttributes().get(stmt.getAttributeName());
      traverse(primitive, attribute.getStatements());
    } else if (stmt.getCalleeName() != null) {
      final PrimitiveAND primitive = primitives.peek();
      final Primitive callee = primitive.getArguments().get(stmt.getCalleeName());
      if (!callee.isOrRule()) {
        final PrimitiveAND calleePrimitive = (PrimitiveAND) callee;
        final Attribute calleeAttribute = calleePrimitive.getAttributes().get(stmt.getAttributeName());
        traverse(calleePrimitive, calleeAttribute.getStatements());
      }
    } else {
      final PrimitiveAND primitive = primitives.peek();
      final Attribute attribute = primitive.getAttributes().get(stmt.getAttributeName());

      for (final Statement attrStmt : attribute.getStatements()) {
        onStatement(attrStmt);
      }
    }
  }

  private void onCondition(final StatementCondition stmt) {
    for (int index = 0; index < stmt.getBlockCount(); index++) {
      conditions.push(new Pair<>(stmt, index));

      final StatementCondition.Block block = stmt.getBlock(index);
      for (final Statement blockStmt : block.getStatements()) {
        onStatement(blockStmt);
      }

      conditions.pop();
    }
  }

  private boolean isPCAssignment(final Location left, final Expr right) {
    if (!inquirer.isPC(left)) {
      return false;
    }

    final PCSourceExplorer visitor = new PCSourceExplorer();
    final ExprTreeWalker walker = new ExprTreeWalker(visitor);

    walker.visit(right.getNode());
    return visitor.isBasedOnExternalParameters();
  }

  private final class PCSourceExplorer extends ExprTreeVisitorDefault {
    private boolean basedOnExternalParameters = false;

    public boolean isBasedOnExternalParameters() {
      return basedOnExternalParameters;
    }

    @Override
    public void onVariable(final NodeVariable variable) {
      final Expr node = new Expr(variable);
      final NodeInfo nodeInfo = node.getNodeInfo();

      InvariantChecks.checkNotNull(nodeInfo.isLocation());
      final Location location = (Location) nodeInfo.getSource();

      final List<LocationAtom> locations = location instanceof LocationAtom ?
          Collections.singletonList((LocationAtom) location) :
          ((LocationConcat) location).getLocations();

      for (final LocationAtom atom : locations) {
        if (!inquirer.isPC(atom)) {
          basedOnExternalParameters = true;
          setStatus(Status.ABORT);
        }
      }
    }
  }
}
