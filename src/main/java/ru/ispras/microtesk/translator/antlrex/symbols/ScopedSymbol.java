/*
 * Copyright (c) 2012 ISPRAS (www.ispras.ru)
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * ScopedSymbol.java, Dec 10, 2012 6:37:38 PM Andrei Tatarnikov
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

package ru.ispras.microtesk.translator.antlrex.symbols;

import org.antlr.runtime.Token;

public final class ScopedSymbol<Kind extends Enum<Kind>> extends Symbol<Kind> {
  private final IScope<Kind> innerScope;

  public ScopedSymbol(Token token, Kind kind, IScope<Kind> scope) {
    super(token, kind, scope);
    this.innerScope = new Scope<Kind>(scope, this);
  }

  @Override
  public final IScope<Kind> getInnerScope() {
    return innerScope;
  }
}
