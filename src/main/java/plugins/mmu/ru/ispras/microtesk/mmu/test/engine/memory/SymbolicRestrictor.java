/*
 * Copyright 2017 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.mmu.test.engine.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.basis.solver.bitvector.BitVectorConstraint;
import ru.ispras.microtesk.basis.solver.bitvector.BitVectorRange;
import ru.ispras.microtesk.basis.solver.bitvector.BitVectorRangeConstraint;
import ru.ispras.microtesk.mmu.MmuPlugin;
import ru.ispras.microtesk.mmu.basis.MemoryAccessContext;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuAddressInstance;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuBuffer;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuBufferAccess;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuProgram;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuSubsystem;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuTransition;
import ru.ispras.microtesk.settings.GeneratorSettings;
import ru.ispras.microtesk.settings.RegionSettings;

/**
 * {@link SymbolicRestrictor} produces a constraint for a given memory access.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class SymbolicRestrictor {
  private final MmuSubsystem memory = MmuPlugin.getSpecification();

  private final RegionSettings region;

  public SymbolicRestrictor(final RegionSettings region) {
    // Region can be null.
    this.region = region;
  }

  public Collection<BitVectorConstraint> getConstraints(
      final MmuBufferAccess bufferAccess) {
    final MmuAddressInstance physAddrType = memory.getPhysicalAddress();
    final MmuAddressInstance addrType = bufferAccess.getAddress();
    final MmuBuffer buffer = bufferAccess.getBuffer();
    final MemoryAccessContext context = bufferAccess.getContext();

    // Restrict the physical memory access.
    final boolean isTopLevel = context.getMemoryAccessStack().isEmpty();
    final boolean isPhysAddr = addrType.getName().equals(physAddrType.getName());

    if (region != null && isTopLevel && isPhysAddr) {
      return getConstraints(addrType, region);
    }

    // Restrict the memory-mapped buffer access.
    if (buffer.getKind() == MmuBuffer.Kind.MEMORY) {
      final GeneratorSettings settings = GeneratorSettings.get();
      InvariantChecks.checkNotNull(settings);

      return getConstraints(
          bufferAccess.getAddress(),
          settings.getMemory().getRegion(buffer.getName()));
    }

    return Collections.<BitVectorConstraint>emptyList();
  }

  public Collection<BitVectorConstraint> getConstraints() {
    final GeneratorSettings settings = GeneratorSettings.get();
    InvariantChecks.checkNotNull(settings);

    return getConstraints(
        memory.getVirtualAddress(),
        settings.getMemory().getRegion(memory.getName()));
  }

  private Collection<BitVectorConstraint> getConstraints(
      final MmuAddressInstance addrType,
      final RegionSettings region) {
    InvariantChecks.checkNotNull(addrType);
    InvariantChecks.checkNotNull(region);

    final BitVectorRange range = new BitVectorRange(
        BitVector.valueOf(region.getMin(), addrType.getWidth()),
        BitVector.valueOf(region.getMax(), addrType.getWidth()));
    Logger.debug("Range constraint: %s in %s", addrType, range);

    return Collections.<BitVectorConstraint>singleton(
        new BitVectorRangeConstraint(addrType.getVariable().getVariable(), range));
  }

  public Collection<BitVectorConstraint> getConstraints(
      final boolean isStart,
      final MmuTransition transition,
      final MemoryAccessContext context) {
    final Collection<BitVectorConstraint> constraints = new ArrayList<>();

    if (isStart) {
      constraints.addAll(getConstraints());
    }

    for (final MmuBufferAccess bufferAccess : transition.getBufferAccesses(context)) {
      constraints.addAll(getConstraints(bufferAccess));
    }

    return constraints;
  }

  public Collection<BitVectorConstraint> getConstraints(
      final boolean isStart,
      final MmuProgram program,
      final MemoryAccessContext context) {
    final Collection<BitVectorConstraint> constraints = new ArrayList<>();

    if (isStart) {
      constraints.addAll(getConstraints());
    }

    for (final MmuTransition transition : program.getTransitions()) {
      for (final MmuBufferAccess bufferAccess : transition.getBufferAccesses(context)) {
        constraints.addAll(getConstraints(bufferAccess));
      }
    }

    return constraints;
  }
}
