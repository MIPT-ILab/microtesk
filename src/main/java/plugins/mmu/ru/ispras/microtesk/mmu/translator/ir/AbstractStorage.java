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

package ru.ispras.microtesk.mmu.translator.ir;

import static ru.ispras.fortress.util.InvariantChecks.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public abstract class AbstractStorage {
  public static final String HIT_ATTR_NAME = "hit";
  public static final String READ_ATTR_NAME = "read";
  public static final String WRITE_ATTR_NAME = "write";
  public static final String ENTRY_NAME = "entry";

  private final String id;
  private final Address address;
  private final Var addressArg;
  private final Var dataArg;
  private final Map<String, Var> variables;
  private final Map<String, Attribute> attributes;

  protected AbstractStorage(
      final String id,
      final Address address,
      final Var addressArg,
      final Var dataArg,
      final Map<String, Var> variables,
      final Map<String, Attribute> attributes) {

    checkNotNull(id);
    checkNotNull(address);
    checkNotNull(addressArg);
    checkNotNull(variables);
    checkNotNull(attributes);

    this.id = id;
    this.address = address;
    this.addressArg = addressArg;
    this.dataArg = dataArg;
    this.variables = Collections.unmodifiableMap(variables);
    this.attributes = Collections.unmodifiableMap(attributes);
  }

  public final String getId() {
    return id;
  }

  public final Address getAddress() {
    return address;
  }

  public final Var getAddressArg() {
    return addressArg;
  }

  public final Var getDataArg() {
    return dataArg;
  }

  public final Collection<Attribute> getAttributes() {
    return attributes.values();
  }

  public final Attribute getAttribute(final String attrId) {
    return attributes.get(attrId);
  }

  public final Collection<Var> getVariables() {
    return variables.values();
  }

  public final Var getVariable(final String varId) {
    return variables.get(varId);
  }
}
