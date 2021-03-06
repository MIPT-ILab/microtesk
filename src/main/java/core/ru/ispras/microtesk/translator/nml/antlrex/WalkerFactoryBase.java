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

package ru.ispras.microtesk.translator.nml.antlrex;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.translator.antlrex.ErrorReporter;
import ru.ispras.microtesk.translator.antlrex.ISemanticError;
import ru.ispras.microtesk.translator.antlrex.SemanticException;
import ru.ispras.microtesk.translator.antlrex.symbols.SymbolTable;
import ru.ispras.microtesk.translator.antlrex.symbols.Where;
import ru.ispras.microtesk.translator.nml.ir.Ir;
import ru.ispras.microtesk.translator.nml.ir.primitive.Primitive;

import java.util.Map;

public class WalkerFactoryBase implements WalkerContext {
  private final WalkerContext context;

  public WalkerFactoryBase(final WalkerContext context) {
    InvariantChecks.checkNotNull(context);
    this.context = context;
  }

  @Override
  public ErrorReporter getReporter() {
    return context.getReporter();
  }

  @Override
  public SymbolTable getSymbols() {
    return context.getSymbols();
  }

  @Override
  public Ir getIr() {
    return context.getIr();
  }

  @Override
  public Map<String, Primitive> getThisArgs() {
    return context.getThisArgs();
  }

  protected final void raiseError(
      final Where where, final String what) throws SemanticException {
    getReporter().raiseError(where, what);
  }

  protected final void raiseError(
      final Where where, final ISemanticError what) throws SemanticException {
    getReporter().raiseError(where, what);
  }
}
