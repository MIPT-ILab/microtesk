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

package ru.ispras.microtesk.mmu.translator.codegen.spec;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.codegen.StringTemplateBuilder;
import ru.ispras.microtesk.mmu.translator.ir.AbstractStorage;
import ru.ispras.microtesk.mmu.translator.ir.Address;
import ru.ispras.microtesk.mmu.translator.ir.Attribute;
import ru.ispras.microtesk.mmu.translator.ir.Ir;
import ru.ispras.microtesk.mmu.translator.ir.Memory;
import ru.ispras.microtesk.mmu.translator.ir.Stmt;
import ru.ispras.microtesk.mmu.translator.ir.Type;
import ru.ispras.microtesk.mmu.translator.ir.Var;

import java.util.Collections;

final class StbMemory implements StringTemplateBuilder {
  public static final Class<?> SPEC_CLASS =
      ru.ispras.microtesk.mmu.translator.ir.spec.MmuSubsystem.class;

  public static final Class<?> OPERATION_CLASS =
      ru.ispras.microtesk.mmu.basis.MemoryOperation.class;

  private final String packageName;
  private final Ir ir;
  private final Memory memory;

  protected StbMemory(final String packageName, final Ir ir, final Memory memory) {
    InvariantChecks.checkNotNull(packageName);
    InvariantChecks.checkNotNull(memory);

    this.packageName = packageName;
    this.ir = ir;
    this.memory = memory;
  }

  @Override
  public ST build(final STGroup group) {
    final ST st = group.getInstanceOf("source_file");

    buildHeader(st);
    buildAddress(st, group);
    buildConstructor(st, group);

    return st;
  }

  private void buildHeader(final ST st) {
    st.add("name", memory.getId());
    st.add("pack", packageName);
    st.add("instance", false);

    st.add("imps", ru.ispras.fortress.data.Data.class.getName());
    st.add("imps", ru.ispras.fortress.data.DataType.class.getName());
    st.add("imps", ru.ispras.fortress.data.Variable.class.getName());
    st.add("imps", ru.ispras.fortress.data.types.bitvector.BitVector.class.getName());
    st.add("imps", ru.ispras.fortress.expression.Nodes.class.getName());
    st.add("imps", ru.ispras.fortress.expression.NodeValue.class.getName());
    st.add("imps", ru.ispras.fortress.expression.NodeVariable.class.getName());

    st.add("imps", OPERATION_CLASS.getName());
    st.add("imps", SPEC_CLASS.getName());
  }

  private void buildAddress(final ST st, final STGroup group) {
    StbStruct.buildFieldAlias(
        memory.getId(),
        memory.getAddressArg(),
        memory.getAddress(),
        true,
        st,
        group
    );

    // FIXME: All addresses are considered singletons. This assumption is not always correct.
    // There may be several local variables having type which corresponds to some address.
    // In this case they will refer to the same instance causing troubles.

    for (final Var variable : memory.getVariables()) {
      final Address address = ir.getAddresses().get(variable.getType().getId());
      if (null != address) {
        StbStruct.buildFieldAlias(
            memory.getId(),
            variable,
            address,
            false,
            st,
            group
        );
      }
    }

    st.add("members", "");
  }

  private void buildConstructor(final ST st, final STGroup group) {
    final ST stConstructor = group.getInstanceOf("constructor_memory");
    stConstructor.add("name", memory.getId());

    final String dataVariableName = getVariableName(memory.getDataArg().getName());
    StbStruct.buildFieldDecl(
        dataVariableName,
        memory.getDataArg().getType(),
        true,
        st,
        stConstructor,
        group
    );
    stConstructor.add("stmts", String.format("builder.setDataVariable(%s);%n", dataVariableName));

    for (final Var variable : memory.getVariables()) {
      final Type type = variable.getType();
      if (!ir.getAddresses().containsKey(type.getId())) {
        // FIXME: All addresses are considered singletons. This assumption is not always correct.
        // There may be several local variables having type which corresponds to some address.
        // In this case they will refer to the same instance causing troubles.

        final String name = getVariableName(variable.getName());
        StbStruct.buildFieldDecl(
            name,
            type,
            false,
            st,
            stConstructor,
            group
        );

        stConstructor.add("stmts", String.format("builder.registerVariable(%s);", name));
        stConstructor.add("stmts", "");
      }
    }

    final Attribute read = memory.getAttribute(AbstractStorage.READ_ATTR_NAME);
    final Attribute write = memory.getAttribute(AbstractStorage.WRITE_ATTR_NAME);

    if (null != read && null != write) {
      ControlFlowBuilder.buildImports(st, group);
    }

    final ControlFlowBuilder builder = new ControlFlowBuilder(
        ir,
        memory.getId(),
        st,
        group,
        stConstructor
        );

    builder.build(
        "START",
        "STOP",
        "IF_READ",
        read != null ? read.getStmts() : Collections.<Stmt>emptyList(),
        "IF_WRITE",
        write != null ? write.getStmts() : Collections.<Stmt>emptyList()
    );

    st.add("members", "");
    st.add("members", stConstructor);
  }

  private String getVariableName(final String prefixedName) {
    return Utils.getVariableName(memory.getId(), prefixedName);
  }
}
