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

package ru.ispras.microtesk.translator.mmu.ir;

import static ru.ispras.fortress.util.InvariantChecks.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public final class Memory extends AbstractStorage {
  private final Map<String, Variable> variables;

  public Memory(
      String id,
      Address address,
      Variable addressArg,
      Variable dataArg,
      Map<String, Variable> variables,
      Map<String, Attribute> attributes) {

    super(id, address, addressArg, dataArg, attributes);

    checkNotNull(variables);
    this.variables = Collections.unmodifiableMap(variables);
  }

  public Collection<Variable> getVaraibles() {
    return variables.values();
  }

  public Variable getVariable(String id) {
    return variables.get(id);
  }

  @Override
  public String toString() {
    return String.format("mmu %s(%s)=(%s) [vars=%s, attributes=%s]",
        getId(), getAddressArg(), getDataArg(), variables, getAttributes());
  }
}
