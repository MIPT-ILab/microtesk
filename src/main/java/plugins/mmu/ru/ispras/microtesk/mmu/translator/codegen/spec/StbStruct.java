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
import ru.ispras.microtesk.mmu.translator.ir.Address;
import ru.ispras.microtesk.mmu.translator.ir.Type;
import ru.ispras.microtesk.mmu.translator.ir.Var;

import java.util.Map;

final class StbStruct implements StringTemplateBuilder {
  public static final Class<?> STRUCT_CLASS =
      ru.ispras.microtesk.mmu.translator.ir.spec.MmuStruct.class;

  public static final Class<?> DATA_TYPE_CLASS =
      ru.ispras.fortress.data.DataType.class;

  public static final Class<?> VARIABLE_CLASS =
      ru.ispras.fortress.expression.NodeVariable.class;

  private final String packageName;
  private final Type type;

  protected StbStruct(final String packageName, final Type type) {
    InvariantChecks.checkNotNull(packageName);
    InvariantChecks.checkNotNull(type);

    this.packageName = packageName;
    this.type = type;
  }

  @Override
  public ST build(final STGroup group) {
    final ST st = group.getInstanceOf("source_file");

    buildHeader(st);
    buildFields(st, group);

    return st;
  }

  private void buildHeader(final ST st) {
    st.add("name", type.getId());
    st.add("pack", packageName);
    st.add("ext",  STRUCT_CLASS.getSimpleName());

    st.add("imps", DATA_TYPE_CLASS.getName());
    st.add("imps", VARIABLE_CLASS.getName());
    st.add("imps", STRUCT_CLASS.getName());
  }

  private void buildFields(final ST st, final STGroup group) {
    final ST stConstructor = group.getInstanceOf("struct_constructor");

    stConstructor.add("name", type.getId());
    buildFieldDecls(type, st, stConstructor, group);

    stConstructor.add("stmts", "");
    buildAddField(type, stConstructor, group);

    st.add("members", "");
    st.add("members", stConstructor);
  }

  protected static void buildFieldDecls(
      final Type structType,
      final ST st,
      final ST stConstructor,
      final STGroup group) {
    for (final Map.Entry<String, Type> field : structType.getFields().entrySet()) {
      final String name = field.getKey();
      final Type type = field.getValue();
      buildFieldDecl(name, type, true, st, stConstructor, group);
    }
  }

  protected static void buildFieldDecl(
      final String name,
      final Type type,
      final boolean isPublic,
      final ST st,
      final ST stConstructor,
      final STGroup group) {
    st.add("members", getFieldDecl(name, type, isPublic, group));
    stConstructor.add("stmts", getFieldDef(name, type, group));
  }

  protected static ST getFieldDecl(
      final String name,
      final Type type,
      final boolean isPublic,
      final STGroup group) {
    final ST fieldDecl = group.getInstanceOf("field_decl");

    fieldDecl.add("name", name);
    fieldDecl.add("is_public", isPublic);
    if (type.isStruct()) {
      fieldDecl.add("type", type.getId());
    } else {
      fieldDecl.add("type", VARIABLE_CLASS.getSimpleName());
    }
    return fieldDecl;
  }

  protected static ST getFieldDef(final String name, final Type type, final STGroup group) {
    final ST fieldDef;
    if (type.isStruct()) {
      fieldDef = group.getInstanceOf("field_def_struct");
      fieldDef.add("type", type.getId());
    } else {
      fieldDef = group.getInstanceOf("field_def_var");
      fieldDef.add("type", String.format(
          "%s.bitVector(%d)", DATA_TYPE_CLASS.getSimpleName(), type.getBitSize()));
    }

    fieldDef.add("name", name);
    return fieldDef;
  }

  protected static void buildAddField(
      final Type structType,
      final ST stConstructor,
      final STGroup group) {
    for (final String fieldName : structType.getFields().keySet()) {
      final ST addField = group.getInstanceOf("add_field");
      addField.add("name", fieldName);
      stConstructor.add("stmts", addField);
    }
  }

  protected static void buildFieldAlias(
      final String context,
      final Var variable,
      final Address address,
      final boolean isPublic,
      final ST st,
      final STGroup group) {
    InvariantChecks.checkNotNull(context);
    InvariantChecks.checkNotNull(variable);
    InvariantChecks.checkNotNull(address);
    InvariantChecks.checkNotNull(st);
    InvariantChecks.checkNotNull(group);

    final ST stAddress = group.getInstanceOf("field_alias");
    stAddress.add("name", Utils.getVariableName(context, variable.getName()));
    stAddress.add("type", address.getId());
    stAddress.add("is_public", isPublic);
    st.add("members", stAddress);
  }
}
