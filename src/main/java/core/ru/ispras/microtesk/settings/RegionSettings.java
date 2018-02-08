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

package ru.ispras.microtesk.settings;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.model.memory.MemoryAccessMode;
import ru.ispras.microtesk.utils.Range;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

/**
 * {@link RegionSettings} represents a configuration of a single memory region.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class RegionSettings extends AbstractSettings implements Range<BigInteger> {
  public static final String TAG = "region";

  public static enum Type {
    TEXT,
    DATA,
    TABLE
  }

  private final String name;
  private final Type type;
  private final BigInteger startAddress;
  private final BigInteger endAddress;
  private final MemoryAccessMode mode;
  private final MemoryAccessMode others;

  private final Collection<AccessSettings> accesses = new ArrayList<>();

  public RegionSettings(
      final String name,
      final Type type,
      final BigInteger startAddress,
      final BigInteger endAddress,
      final MemoryAccessMode mode,
      final MemoryAccessMode others) {
    super(TAG);

    InvariantChecks.checkNotNull(name);
    InvariantChecks.checkNotNull(type);
    InvariantChecks.checkNotNull(startAddress);
    InvariantChecks.checkNotNull(endAddress);
    InvariantChecks.checkNotNull(mode);
    InvariantChecks.checkNotNull(others);

    this.name = name;
    this.type = type;
    this.startAddress = startAddress;
    this.endAddress = endAddress;
    this.mode = mode;
    this.others = others;
  }

  public String getName() {
    return name;
  }

  public Type getType() {
    return type;
  }

  public BigInteger getStartAddress() {
    return startAddress;
  }

  public BigInteger getEndAddress() {
    return endAddress;
  }

  public boolean canRead() {
    return mode.r;
  }

  public boolean canWrite() {
    return mode.w;
  }

  public boolean canExecute() {
    return mode.x;
  }

  public boolean isEnabled() {
    return mode.r || mode.w || mode.x;
  }

  public boolean isVolatile() {
    return others.w;
  }

  public boolean checkAddress(final BigInteger address) {
    return (startAddress.compareTo(address) <= 0 && endAddress.compareTo(address) >= 0);
  }

  public Collection<AccessSettings> getAccesses() {
    return accesses;
  }

  @Override
  public BigInteger getMin() {
    return startAddress;
  }

  @Override
  public BigInteger getMax() {
    return endAddress;
  }

  @Override
  public Collection<AbstractSettings> get(final String tag) {
    InvariantChecks.checkTrue(AccessSettings.TAG.equals(tag));

    final Collection<AbstractSettings> result = new ArrayList<>(accesses.size());
    result.addAll(getAccesses());

    return result;
  }

  @Override
  public void add(final AbstractSettings section) {
    InvariantChecks.checkTrue(section instanceof AccessSettings);
    accesses.add((AccessSettings) section);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }

    if (o == null || !(o instanceof RegionSettings)) {
      return false;
    }

    final RegionSettings r = (RegionSettings) o;
    return name.equals(r.name);
  }

  @Override
  public String toString() {
    return String.format("%s={name=%s, type=%s, start=%x, end=%x, mode=%s%s}",
        TAG, name, type.name(), startAddress, endAddress, mode, others);
  }
}
