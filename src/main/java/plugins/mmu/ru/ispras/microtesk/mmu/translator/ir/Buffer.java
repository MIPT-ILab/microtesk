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
import static ru.ispras.fortress.util.InvariantChecks.checkTrue;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import ru.ispras.fortress.data.DataType;
import ru.ispras.fortress.expression.Node;
import ru.ispras.microtesk.mmu.model.api.PolicyId;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuBuffer;

public final class Buffer extends AbstractStorage {
  private final MmuBuffer.Kind kind;
  private final BigInteger ways;
  private final BigInteger sets;
  private final Node index;
  private final Node match;
  private final Node guard;
  private final PolicyId policy;
  private final Buffer parent;

  public Buffer(
      final String id,
      final MmuBuffer.Kind kind,
      final Address address,
      final Variable addressArg,
      final Variable dataArg,
      final BigInteger ways,
      final BigInteger sets,
      final Node index,
      final Node match,
      final Node guard,
      final PolicyId policy,
      final Buffer parent) {

    super(
        id,
        address,
        addressArg,
        dataArg,
        Collections.<String, Variable>emptyMap(),
        createAttributes(addressArg, dataArg)
    );

    checkNotNull(kind);
    checkTrue(ways.compareTo(BigInteger.ZERO) > 0);
    checkTrue(sets.compareTo(BigInteger.ZERO) > 0);
    checkNotNull(index);
    checkNotNull(match);
    checkNotNull(policy);

    checkTrue((guard == null) == (parent == null));

    this.kind = kind;
    this.ways = ways;
    this.sets = sets;
    this.index = index;
    this.match = match;
    this.guard = guard;
    this.policy = policy;
    this.parent = parent;
  }

  private static Map<String, Attribute> createAttributes(
      final Variable addressArg,
      final Variable dataArg) {

    checkNotNull(addressArg);
    checkNotNull(dataArg);

    final Attribute[] attrs = new Attribute[] { 
        new Attribute(HIT_ATTR_NAME, DataType.BOOLEAN),
        new Attribute(READ_ATTR_NAME, dataArg.getDataType()),
        new Attribute(WRITE_ATTR_NAME, dataArg.getDataType())
    };

    final Map<String, Attribute> result = new LinkedHashMap<>();
    for (Attribute attr : attrs) {
      result.put(attr.getId(), attr);
    }

    return Collections.unmodifiableMap(result);
  }

  public MmuBuffer.Kind getKind() {
    return kind;
  }

  public BigInteger getWays() {
    return ways;
  }

  public BigInteger getSets() {
    return sets;
  }

  public Type getEntry() {
    return getDataArg().getType();
  }

  public Node getIndex() {
    return index;
  }

  public Node getMatch() {
    return match;
  }

  public Node getGuard() {
    return guard;
  }

  public PolicyId getPolicy() {
    return policy;
  }

  public Buffer getParent() {
    return parent;
  }

  @Override
  public String toString() {
    return String.format(
        "%sbuffer %s(%s) = {ways=%d, sets=%d, entry=%s, index=%s, match=%s, policy=%s, guard=%s, parent=%s}",
        kind.getText(),
        getId(),
        getAddressArg(),
        ways,
        sets,
        getEntry(),
        index,
        match,
        policy,
        guard,
        parent != null ? parent.getId() : null
        );
  }
}
