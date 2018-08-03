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

package ru.ispras.microtesk.mmu.translator.codegen.sim;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.codegen.StringTemplateBuilder;
import ru.ispras.microtesk.mmu.translator.ir.Operation;

import java.math.BigInteger;

final class StbOperation extends StbCommon implements StringTemplateBuilder {
  public static final Class<?> OPERATION_CLASS =
      ru.ispras.microtesk.mmu.model.sim.Operation.class;

  private final Operation operation;

  public StbOperation(
      final String packageName,
      final Operation operation) {
    super(packageName);

    InvariantChecks.checkNotNull(operation);
    this.operation = operation;
  }

  @Override
  public ST build(final STGroup group) {
    final ST st = group.getInstanceOf("source_file");

    buildHeader(st);
    buildBody(st, group);

    return st;
  }

  protected final void buildHeader(final ST st) {
    final String baseText = String.format(
        "%s<%s>",
        OPERATION_CLASS.getName(),
        operation.getAddress().getId()
        );

    st.add("name", operation.getId());
    st.add("pack", packageName);
    st.add("ext", baseText);

    st.add("imps", BigInteger.class.getName());
    st.add("imps", String.format("%s.*", StbCommon.BIT_VECTOR_CLASS.getPackage().getName()));
  }

  private void buildBody(final ST st, final STGroup group) {
    final ST stBody = group.getInstanceOf("operation_body");
    stBody.add("name", getId());
    stBody.add("addr", operation.getAddress().getId());

    final String addrName = removePrefix(operation.getAddressArg().getName());
    stBody.add("addr_name", addrName);

    ExprPrinter.get().pushVariableScope();
    ExprPrinter.get().addVariableMappings(operation.getAddressArg(), addrName);

    buildStmts(stBody, group, operation.getStmts());
    ExprPrinter.get().popVariableScope();

    st.add("members", stBody);
  }

  @Override
  protected String getId() {
    return operation.getId();
  }
}
