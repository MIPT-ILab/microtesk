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

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.codegen.StringTemplateBuilder;
import ru.ispras.microtesk.mmu.translator.ir.Var;

public class StbExtern implements StringTemplateBuilder {
  public static final Class<?> CONSTANT_CLASS =
      ru.ispras.microtesk.mmu.translator.ir.spec.MmuDynamicConst.class;

  private final String packageName;
  private final String simulatorPackageName;
  private final Var extern;

  public StbExtern(
      final String packageName,
      final Var extern) {
    InvariantChecks.checkNotNull(packageName);
    InvariantChecks.checkNotNull(extern);

    this.packageName = packageName;
    this.extern = extern;
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
    st.add("name", extern.getName());
    st.add("pack", packageName);
    st.add("imps", java.util.Map.class.getName());
    st.add("imps", java.util.HashMap.class.getName());
    st.add("imps", ru.ispras.fortress.expression.NodeVariable.class.getName());
    st.add("imps", CONSTANT_CLASS.getName());
  }

  private void buildBody(final ST st, final STGroup group) {
    final ST stBody = group.getInstanceOf("constant_body");

    stBody.add("name", extern.getName());
    stBody.add("width", extern.getBitSize());
    stBody.add("value",
        String.format("%s.Extern.get().%s", simulatorPackageName, extern.getName()));

    st.add("members", stBody);
  }
}
