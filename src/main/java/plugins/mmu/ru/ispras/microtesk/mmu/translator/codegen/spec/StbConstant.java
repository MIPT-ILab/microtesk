/*
 * Copyright 2015-2017 ISP RAS (http://www.ispras.ru)
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

import ru.ispras.castle.codegen.StringTemplateBuilder;
import ru.ispras.fortress.data.DataType;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.mmu.translator.ir.Constant;

public class StbConstant implements StringTemplateBuilder {
  public static final Class<?> CONSTANT_CLASS =
      ru.ispras.microtesk.mmu.model.spec.MmuDynamicConst.class;

  private final String packageName;
  private final String simulatorPackageName;
  private final Constant constant;

  public StbConstant(
      final String packageName,
      final Constant constant) {
    InvariantChecks.checkNotNull(packageName);
    InvariantChecks.checkNotNull(constant);

    this.packageName = packageName;
    this.constant = constant;
    this.simulatorPackageName = packageName.substring(0, packageName.lastIndexOf('.')) + ".sim";
  }

  @Override
  public ST build(final STGroup group) {
    final ST st = group.getInstanceOf("source_file");

    buildHeader(st);
    buildBody(st, group);

    return st;
  }

  private void buildHeader(final ST st) {
    st.add("name", constant.getId());
    st.add("pack", packageName);
    st.add("imps", java.util.Map.class.getName());
    st.add("imps", java.util.HashMap.class.getName());
    st.add("imps", ru.ispras.fortress.data.DataType.class.getName());
    st.add("imps", ru.ispras.fortress.expression.NodeVariable.class.getName());
    st.add("imps", CONSTANT_CLASS.getName());
  }

  private void buildBody(final ST st, final STGroup group) {
    final ST stBody = group.getInstanceOf("constant_body");

    final DataType type = constant.getVariable().getDataType();
    stBody.add("name",  constant.getId());
    stBody.add("width", type.getTypeId().isLogic() ? "DataType.LOGIC_TYPE_SIZE" : type.getSize());
    stBody.add("value", String.format("%s.%s.get()", simulatorPackageName, constant.getId()));

    st.add("members", stBody);
  }
}
