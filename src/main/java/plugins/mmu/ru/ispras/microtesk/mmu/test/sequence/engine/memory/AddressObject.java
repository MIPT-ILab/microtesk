/*
 * Copyright 2006-2017 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.mmu.test.sequence.engine.memory;

import java.util.LinkedHashMap;
import java.util.Map;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.basis.solver.integer.IntegerVariable;
import ru.ispras.microtesk.mmu.MmuPlugin;
import ru.ispras.microtesk.mmu.basis.MemoryAccessType;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuAddressInstance;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuBuffer;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuBufferAccess;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuSubsystem;

/**
 * {@link AddressObject} represents test data for an individual {@link MemoryAccess}.
 * 
 * <p>Test data include addresses (virtual and physical ones), auxiliary attributes (cache policy,
 * control bits, etc.), sequences of addresses to be accessed to prepare hit/miss situations and
 * sets of entries to be written into the buffers.</p>
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class AddressObject {
  /**
   * Refers to the MMU specification.
   *
   * <p>It is used to initialize the auxiliary data structures and to build a string representation
   * of the test data (to print tags and indices for all relevant buffers).</p>
   */
  private final MmuSubsystem memory = MmuPlugin.getSpecification();

  /** Contains addresses (virtual, physical and intermediate addresses). */
  private final Map<MmuAddressInstance, Long> addresses = new LinkedHashMap<>();

  /** Contains attributes associated with the instruction call (cache policy, etc.). */
  private final Map<IntegerVariable, Long> attributes = new LinkedHashMap<>();

  /**
   * Contains entries to be written into the buffers.
   * 
   * <p>Typically, one memory access affects one entry of one buffer. It is assumed that each map
   * contains exactly one entry.</p>
   */
  private final Map<MmuBufferAccess, Map<Long, EntryObject>> entries = new LinkedHashMap<>();

  /** Refers to the memory access. */
  private MemoryAccess access;

  public AddressObject(final MemoryAccess access) {
    InvariantChecks.checkNotNull(memory);
    InvariantChecks.checkNotNull(access);

    this.access = access;
  }

  public MemoryAccessType getType() {
    return access.getType();
  }

  public MemoryAccessPath getPath() {
    return access.getPath();
  }

  public void setPath(final MemoryAccessPath path) {
    InvariantChecks.checkNotNull(path);

    final MemoryAccess access = new MemoryAccess(
        this.access.getType(), path, this.access.getRegion(), this.access.getSegment());

    this.access = access;
  }

  public Long getAttrValue(final IntegerVariable variable) {
    InvariantChecks.checkNotNull(variable);
    return attributes.get(variable);
  }

  public void setAttrValue(final IntegerVariable variable, final long value) {
    InvariantChecks.checkNotNull(variable);
    attributes.put(variable, value);
  }

  public void clearAttrs() {
    attributes.clear();
  }

  public long getAddress(final MmuAddressInstance addressType) {
    InvariantChecks.checkNotNull(addressType);
    return addresses.get(addressType); 
  }

  public long getAddress(final MmuBufferAccess bufferAccess) {
    InvariantChecks.checkNotNull(bufferAccess);
    return addresses.get(bufferAccess.getAddress()); 
  }

  public long getVirtualAddress() {
    return getAddress(memory.getVirtualAddress());
  }

  public long getPhysicalAddress() {
    return getAddress(memory.getPhysicalAddress());
  }

  public Map<MmuAddressInstance, Long> getAddresses() {
    return addresses; 
  }

  public void setAddress(final MmuAddressInstance addressType, final long value) {
    InvariantChecks.checkNotNull(addressType);
    addresses.put(addressType, value);
  }

  public void setAddress(final MmuBufferAccess bufferAccess, final long value) {
    InvariantChecks.checkNotNull(bufferAccess);
    addresses.put(bufferAccess.getAddress(), value);
  }

  public void setVirtualAddress(final long value) {
    setAddress(memory.getVirtualAddress(), value);
  }

  public void setPhysicalAddress(final long value) {
    setAddress(memory.getPhysicalAddress(), value);
  }

  public Map<MmuBufferAccess, Map<Long, EntryObject>> getEntries() {
    return entries;
  }

  /**
   * Returns the entries to be written to the given buffer.
   * 
   * @param bufferAccess the buffer access.
   * @return the entries to written.
   * @throws IllegalArgumentException if {@code buffer} is null.
   */
  public Map<Long, EntryObject> getEntries(final MmuBufferAccess bufferAccess) {
    InvariantChecks.checkNotNull(bufferAccess);
    return entries.get(bufferAccess);
  }

  /**
   * Sets the entries to be written to the given buffer.
   * 
   * @param bufferAccess the buffer access.
   * @param entries the entries to be written.
   * @throws IllegalArgumentException if some parameters are null.
   */
  public void setEntries(final MmuBufferAccess bufferAccess, final Map<Long, EntryObject> entries) {
    InvariantChecks.checkNotNull(bufferAccess);
    InvariantChecks.checkNotNull(entries);
    InvariantChecks.checkTrue(entries.size() == 1);

    for (final EntryObject entry : entries.values()) {
      entry.addAddrObject(this);
    }

    this.entries.put(bufferAccess, entries);
  }

  /**
   * Adds the entry to the set of entries to be written to the given buffer.
   * 
   * @param bufferAccess the buffer access.
   * @param entry the entry to be added.
   * @throws IllegalArgumentException if some parameters are null.
   */
  public void addEntry(final MmuBufferAccess bufferAccess, final EntryObject entry) {
    InvariantChecks.checkNotNull(bufferAccess);
    InvariantChecks.checkNotNull(entry);

    Map<Long, EntryObject> bufferEntries = entries.get(bufferAccess);

    if (bufferEntries == null) {
      entries.put(bufferAccess, bufferEntries = new LinkedHashMap<>());
    }

    entry.addAddrObject(this);
    bufferEntries.put(entry.getId(), entry);
  }

  @Override
  public String toString() {
    final String separator = ", ";
    final StringBuilder builder = new StringBuilder();

    boolean comma = false;

    for (final Map.Entry<MmuAddressInstance, Long> addrEntry : addresses.entrySet()) {
      final MmuAddressInstance addrType = addrEntry.getKey();
      final long addrValue = addrEntry.getValue();

      builder.append(comma ? separator : "");
      builder.append(String.format("%s=0x%x", addrType.getVariable().getName(), addrValue));
      comma = true;

      for (final MmuBuffer buffer : memory.getBuffers()) {
        if (buffer.getAddress() == addrType) {
          if (buffer.isReplaceable()) {
            builder.append(comma ? separator : "");
            builder.append(String.format("%s.tag=0x%x", buffer, buffer.getTag(addrValue)));
            comma = true;
          }

          if (buffer.getSets() > 1) {
            builder.append(comma ? separator : "");
            builder.append(String.format("%s.index=0x%x", buffer, buffer.getIndex(addrValue)));
            comma = true;
          }
        }
      }
    }

    return builder.toString();
  }
}
