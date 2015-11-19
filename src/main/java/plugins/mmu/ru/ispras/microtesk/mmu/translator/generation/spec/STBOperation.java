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

package ru.ispras.microtesk.mmu.translator.generation.spec;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.mmu.translator.ir.Operation;
import ru.ispras.microtesk.translator.generation.STBuilder;

public final class STBOperation implements STBuilder {
  public static final Class<?> OPERATION_CLASS =
      ru.ispras.microtesk.mmu.translator.ir.spec.MmuOperation.class;

  private final String packageName;
  private final Operation operation;

  protected STBOperation(
      final String packageName,
      final Operation operation) {
    InvariantChecks.checkNotNull(packageName);
    InvariantChecks.checkNotNull(operation);

    this.packageName = packageName;
    this.operation = operation;
  }

  @Override
  public ST build(final STGroup group) {
    final ST st = group.getInstanceOf("source_file");

    buildHeader(st);
    buildBody(st, group);

    return st;
  }

  private void buildHeader(final ST st) {
    st.add("name", operation.getId()); 
    st.add("pack", packageName);
    st.add("imps", OPERATION_CLASS.getName());
  }

  private void buildBody(final ST st, final STGroup group) {
    final ST stBody = group.getInstanceOf("operation_body");

    stBody.add("name", operation.getId());
    stBody.add("addr", operation.getAddress().getId());

    st.add("members", stBody);
  }
}
