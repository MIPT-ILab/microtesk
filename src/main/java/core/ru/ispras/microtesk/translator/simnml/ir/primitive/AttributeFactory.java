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

package ru.ispras.microtesk.translator.simnml.ir.primitive;

import java.util.Collections;
import java.util.List;

import ru.ispras.microtesk.translator.simnml.antlrex.WalkerContext;
import ru.ispras.microtesk.translator.simnml.antlrex.WalkerFactoryBase;

public final class AttributeFactory extends WalkerFactoryBase {
  public AttributeFactory(WalkerContext context) {
    super(context);
  }

  public Attribute createAction(String name, List<Statement> stmts) {
    return new Attribute(name, Attribute.Kind.ACTION, stmts);
  }

  public Attribute createExpression(String name, Statement stmt) {
    return new Attribute(name, Attribute.Kind.EXPRESSION, Collections.singletonList(stmt));
  }
}
