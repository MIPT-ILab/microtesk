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

package ru.ispras.microtesk.mmu.translator.ir.spec;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.basis.solver.integer.IntegerRange;
import ru.ispras.microtesk.mmu.basis.AddressView;
import ru.ispras.microtesk.utils.BigIntegerUtils;
import ru.ispras.microtesk.utils.Range;

/**
 * {@link MmuSegment} represents a virtual memory segment (address space).
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public class MmuSegment implements Range<Long> {
  private final String name;
  private final MmuAddressInstance vaType;
  private final MmuAddressInstance paType;
  private final long startAddress;
  private final long endAddress;
  private final boolean isMapped;
  private final MmuExpression paExpression;
  private final MmuExpression restExpression;
  private final AddressView<Long> addressView;
  private final IntegerRange range;

  public MmuSegment(
      final String name,
      final MmuAddressInstance vaType,
      final MmuAddressInstance paType,
      final long startAddress,
      final long endAddress,
      final boolean isMapped,
      final MmuExpression paExpression,
      final MmuExpression restExpression) {
    InvariantChecks.checkNotNull(name);
    InvariantChecks.checkTrue(isMapped || paExpression != null && restExpression != null);

    this.name = name;
    this.vaType = vaType;
    this.paType = paType;
    this.startAddress = startAddress;
    this.endAddress = endAddress;
    this.isMapped = isMapped;

    this.paExpression = paExpression;
    this.restExpression = restExpression;

    this.addressView =
        isMapped ? null : new MmuAddressViewBuilder(vaType, paExpression, restExpression).build();

    final BigInteger min = BigIntegerUtils.valueOfUnsignedLong(startAddress);
    final BigInteger max = BigIntegerUtils.valueOfUnsignedLong(endAddress);

    this.range = new IntegerRange(min, max);
  }

  public final String getName() {
    return name;
  }

  public final MmuAddressInstance getVaType() {
    return vaType;
  }

  public final MmuAddressInstance getPaType() {
    return paType;
  }

  public final long getStartAddress() {
    return startAddress;
  }

  public final long getEndAddress() {
    return endAddress;
  }

  public boolean isMapped() {
    return isMapped;
  }

  public final MmuExpression getPaExpression() {
    return paExpression;
  }

  public final MmuExpression getRestExpression() {
    return restExpression;
  }

  public final boolean checkVa(final long va) {
    return range.contains(BigIntegerUtils.valueOfUnsignedLong(va));
  }

  public final long getPa(long address) {
    return addressView.getField(address, 0);
  }

  public final long getRest(long address) {
    return addressView.getField(address, 1);
  }

  public final long getVa(final long pa, final long rest) {
    final List<Long> fields = new ArrayList<>();

    fields.add(pa);
    fields.add(rest);

    return addressView.getAddress(fields);
  }

  public final long getVa(final long pa) {
    final long startAddressRest = getRest(startAddress);

    return getVa(pa, startAddressRest);
  }

  @Override
  public final Long getMin() {
    return startAddress;
  }

  @Override
  public final Long getMax() {
    return endAddress;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if ((o == null) || !(o instanceof MmuSegment)) {
      return false;
    }

    final MmuSegment r = (MmuSegment) o;
    return name.equals(r.name);
  }
}
