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

package ru.ispras.microtesk.mmu.test.sequence.engine.iterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.mmu.MmuPlugin;
import ru.ispras.microtesk.mmu.basis.DataType;
import ru.ispras.microtesk.mmu.basis.MemoryAccessConstraints;
import ru.ispras.microtesk.mmu.basis.MemoryAccessType;
import ru.ispras.microtesk.mmu.basis.MemoryOperation;
import ru.ispras.microtesk.mmu.test.sequence.engine.MmuUnderTest;
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.MemoryAccessStructure;
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.MemoryDependency;
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.MemoryHazard;
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.iterator.MemoryAccessStructureIterator;
import ru.ispras.microtesk.mmu.translator.coverage.CoverageExtractor;
import ru.ispras.microtesk.mmu.translator.coverage.MemoryGraphAbstraction;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuAddressInstance;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuBuffer;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuSubsystem;

/**
 * Test for {@link MemoryAccessStructureMmuIterator}.
 * 
 * @author <a href="mailto:protsenko@ispras.ru">Alexander Protsenko</a>
 */
public final class MemoryAccessStructureIteratorTestCase {
  private static Collection<MmuBuffer> buffers = new LinkedHashSet<>();
  private static Collection<MmuAddressInstance> addresses = new LinkedHashSet<>();

  private static final boolean PRINT_LOGS = false;
  private final static int N = 2;

  private static int countPaVaL1Equal = 0;
  private static int countPaVaEqual = 0;

  @Test
  public void runTest() {
    final MmuSubsystem mmu = MmuUnderTest.get().mmu;
    MmuPlugin.setSpecification(mmu);

    final MmuBuffer jtlb = MmuUnderTest.get().jtlb;
    final MmuBuffer l2 = MmuUnderTest.get().l2;
    final MmuBuffer mem = MmuUnderTest.get().mem;

    buffers = mmu.getBuffers();
    addresses = mmu.getAddresses();

    final List<MemoryAccessType> accessTypes = new ArrayList<>();
    for (int i = 0; i < N; i++) {
      accessTypes.add(new MemoryAccessType(MemoryOperation.LOAD, DataType.BYTE));
    }

    final MemoryAccessStructureIterator mmuIterator =
        new MemoryAccessStructureIterator(
            MemoryGraphAbstraction.TRIVIAL,
            accessTypes,
            null,
            new MemoryAccessConstraints.Builder().build(),
            MemoryAccessStructureIterator.Mode.EXHAUSTIVE,
            -1);

    final Map<MemoryHazard.Type, Integer> hazardsType = new HashMap<>();
    for (final MemoryHazard.Type type : MemoryHazard.Type.values()) {
      hazardsType.put(type, 0);
    }

    final Map<MmuBuffer, Map<MemoryHazard.Type, Integer>> bufferHazards = new HashMap<>();
    for (final MmuBuffer buffer : buffers) {
      final Map<MemoryHazard.Type, Integer> hazards = new HashMap<>();
      for (final MemoryHazard hazard : CoverageExtractor.get().getHazards(buffer)) {
        hazards.put(hazard.getType(), 0);
      }

      bufferHazards.put(buffer, hazards);
    }

    final Map<MmuAddressInstance, Map<MemoryHazard.Type, Integer>> addressHazards = new HashMap<>();
    for (final MmuAddressInstance address : addresses) {
      final Map<MemoryHazard.Type, Integer> hazards = new HashMap<>();

      for (final MemoryHazard hazard : CoverageExtractor.get().getHazards(address)) {
        hazards.put(hazard.getType(), 0);
      }

      addressHazards.put(address, hazards);
    }

    int k = 0;
    for (mmuIterator.init(); mmuIterator.hasValue(); mmuIterator.next()) {
      k++;
      if (PRINT_LOGS) {
        System.out.println("");
      }
      if (PRINT_LOGS) {
        System.out.println("Template: " + k);
      }

      checkSituationsDependency((MemoryAccessStructure) mmuIterator.value(), hazardsType,
          bufferHazards, addressHazards);

      if (PRINT_LOGS) {
        System.out.println("");
      }

      boolean testEnd = true;
      for (final Map.Entry<MemoryHazard.Type, Integer> hazards : hazardsType.entrySet()) {
        if (hazards.getValue().equals(0)) {
          testEnd = false;
          break;
        }
      }
      if (testEnd) {
        // break;
      }
    }

    System.out.println("All: " + k);
    System.out.println(hazardsType);

    System.out.println(bufferHazards);
    System.out.println(addressHazards);

    if (countPaVaL1Equal == 0) {
      Assert.fail("Not found: PAEqual, VAEqual, L1TagEqual");
    } else {
      if (PRINT_LOGS)
        System.out.println("Found hazard: PAEqual, VAEqual, L1TagEqual: " + countPaVaL1Equal);
    }

    if (countPaVaEqual == 0) {
      Assert.fail("Not found: PAEqual, VAEqual");
    } else {
      if (PRINT_LOGS)
        System.out.println("Found: PAEqual, VAEqual: " + countPaVaEqual);
    }

    for (final Map.Entry<MemoryHazard.Type, Integer> hazards : hazardsType.entrySet()) {
      if (hazards.getValue() == 0) {
        Assert.fail("Not found: " + hazards.getKey());
      }
    }

    for (final Map.Entry<MmuBuffer, Map<MemoryHazard.Type, Integer>> bufferHazardCounts :
      bufferHazards.entrySet()) {
      final MmuBuffer buffer = bufferHazardCounts.getKey();

      if (buffer == jtlb || buffer == l2 || buffer == mem) {
        continue;
      }

      for (final Map.Entry<MemoryHazard.Type, Integer> hazards :
        bufferHazardCounts.getValue().entrySet()) {
        if (hazards.getValue() == 0) {
          Assert.fail(
              "Not found: " + hazards.getKey() + " of buffer " + bufferHazardCounts.getKey());
        }
      }
    }

    for (final Map.Entry<MmuAddressInstance, Map<MemoryHazard.Type, Integer>> addressHazardCounts :
      addressHazards.entrySet()) {
      for (final Map.Entry<MemoryHazard.Type, Integer> hazards :
        addressHazardCounts.getValue().entrySet()) {
        if (hazards.getValue() == 0) {
          Assert.fail(
              "Not found: " + hazards.getKey() + " of address " + addressHazardCounts.getKey());
        }
      }
    }

  }

  private static void checkSituationsDependency(final MemoryAccessStructure template,
      final Map<MemoryHazard.Type, Integer> hazardsType,
      final Map<MmuBuffer, Map<MemoryHazard.Type, Integer>> bufferHazards,
      final Map<MmuAddressInstance, Map<MemoryHazard.Type, Integer>> addressHazards) {
    InvariantChecks.checkNotNull(template);
    InvariantChecks.checkNotNull(hazardsType);
    InvariantChecks.checkNotNull(bufferHazards);

    for (int i = 0; i < N; i++) {
      for (int j = i + 1; j < N; j++) {
        final MemoryDependency dependency = template.getDependency(i, j);

        if (dependency != null) {
          boolean addressConflict = false;
          boolean paEqual = false;
          boolean vaEqual = false;
          boolean l1TagEqual = false;
          boolean memTagNotEqual = false;

          for (final MemoryHazard hazard : dependency.getHazards()) {
            final MmuAddressInstance address = hazard.getAddress();
            final MmuBuffer buffer = hazard.getDevice();
            final MemoryHazard.Type type = hazard.getType();

            if (buffer != null) {
              final Map<MemoryHazard.Type, Integer> hazards = bufferHazards.get(buffer);
              final int numberOfConflicts = hazards.get(type);
              hazards.put(type, numberOfConflicts + 1);
            }

            if (address != null) {
              final Map<MemoryHazard.Type, Integer> hazards = addressHazards.get(address);
              final int numberOfConflicts = hazards.get(type);
              hazards.put(type, numberOfConflicts + 1);
              addressConflict = true;
              if (MmuUnderTest.get().pa.equals(address.getVariable())) {
                addressConflict = true;
              }
              if (MmuUnderTest.get().va.equals(address.getVariable())) {
                addressConflict = true;
              }
            }

            if (PRINT_LOGS) {
              System.out.println("hazard name: " + hazard.getFullName());
            }

            hazardsType.put(type, hazardsType.get(type) + 1);

            if (address != null
                && MmuUnderTest.get().pa.equals(address.getVariable())
                && MemoryHazard.Type.ADDR_EQUAL.equals(type)) {
              paEqual = true;
            }

            if (address != null
                && MmuUnderTest.get().va.equals(address.getVariable())
                && MemoryHazard.Type.ADDR_EQUAL.equals(type)) {
              vaEqual = true;
            }

            if (buffer != null && MmuUnderTest.get().l1.equals(buffer)
                && MemoryHazard.Type.TAG_EQUAL.equals(type)) {
              l1TagEqual = true;
            }

            if (buffer != null && MmuUnderTest.get().mem.equals(buffer)
                && MemoryHazard.Type.TAG_NOT_EQUAL.equals(type)) {
              memTagNotEqual = true;
            }

            if (paEqual && vaEqual) {
              countPaVaEqual++;
            }

            if (paEqual && memTagNotEqual) {
              Assert.fail("Found: PAEqual, MemTagNotEqual");
            }

            if (paEqual && vaEqual && l1TagEqual) {
              countPaVaL1Equal++;
            }
          }
          if (!addressConflict) {
            Assert.fail("Not found: Address Equal/NotEqual.");
          }
        }
      }
    }
  }
}
