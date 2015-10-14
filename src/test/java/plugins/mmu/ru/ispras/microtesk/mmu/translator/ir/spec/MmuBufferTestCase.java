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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.ispras.fortress.randomizer.Randomizer;
import ru.ispras.microtesk.basis.solver.integer.IntegerField;
import ru.ispras.microtesk.basis.solver.integer.IntegerVariable;
import ru.ispras.microtesk.mmu.basis.AddressView;
import ru.ispras.microtesk.mmu.translator.ir.Type;
import ru.ispras.microtesk.mmu.translator.ir.Variable;
import ru.ispras.microtesk.utils.function.Function;

/**
 * Test for {@link MmuBuffer}.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public class MmuBufferTestCase {
  private static MmuAddressType newAddress(final String name, int width) {
    final Type type = new Type(name, Collections.singletonMap("value", new Type(width)));
    return new MmuAddressType(new Variable(name, type),
                              new IntegerVariable(name + ".value", width));
  }

  public static final MmuAddressType VA_ADDR = newAddress("VA", 64);
  public static final MmuAddressType PA_ADDR = newAddress("PA", 36);

  public static final IntegerVariable VA = VA_ADDR.getVariable();
  public static final IntegerVariable PA = PA_ADDR.getVariable();

  public static final IntegerVariable isMapped = new IntegerVariable("isMapped", 1);
  public static final IntegerVariable isCached = new IntegerVariable("isCached", 1);
  public static final IntegerVariable VPN2 = new IntegerVariable("VPN2", 27);
  public static final IntegerVariable V0 = new IntegerVariable("V0", 1);
  public static final IntegerVariable D0 = new IntegerVariable("D0", 1);
  public static final IntegerVariable G0 = new IntegerVariable("G0", 1);
  public static final IntegerVariable C0 = new IntegerVariable("C0", 3);
  public static final IntegerVariable PFN0 = new IntegerVariable("PFN0", 24);
  public static final IntegerVariable V1 = new IntegerVariable("V1", 1);
  public static final IntegerVariable D1 = new IntegerVariable("D1", 1);
  public static final IntegerVariable G1 = new IntegerVariable("G1", 1);
  public static final IntegerVariable C1 = new IntegerVariable("C1", 3);
  public static final IntegerVariable PFN1 = new IntegerVariable("PFN1", 24);
  public static final IntegerVariable V = new IntegerVariable("V", 1);
  public static final IntegerVariable D = new IntegerVariable("D", 1);
  public static final IntegerVariable G = new IntegerVariable("G", 1);
  public static final IntegerVariable C = new IntegerVariable("C", 3);
  public static final IntegerVariable PFN = new IntegerVariable("PFN", 24);
  public static final IntegerVariable L1_TAG = new IntegerVariable("TAG1", 24);
  public static final IntegerVariable L2_TAG = new IntegerVariable("TAG2", 24);
  public static final IntegerVariable L1_DATA = new IntegerVariable("DATA1", 8 * 32);
  public static final IntegerVariable L2_DATA = new IntegerVariable("DATA2", 8 * 32);
  public static final IntegerVariable DATA = new IntegerVariable("DATA", 8 * 32);

  public static final MmuBuffer JTLB = new MmuBuffer(
      "JTLB", MmuBuffer.Kind.UNMAPPED, 64, 1, VA_ADDR,
      MmuExpression.var(VA, 13, 39), // Tag
      MmuExpression.empty(),         // Index
      MmuExpression.var(VA, 0, 12),  // Offset
      Collections.singleton(new MmuBinding(new IntegerField(VPN2), MmuExpression.var(VA, 13, 39))),
      null, null, false, null
      );

  static {
    JTLB.addField(VPN2);

    JTLB.addField(V0);
    JTLB.addField(D0);
    JTLB.addField(G0);
    JTLB.addField(C0);
    JTLB.addField(PFN0);

    JTLB.addField(V1);
    JTLB.addField(D1);
    JTLB.addField(G1);
    JTLB.addField(C1);
    JTLB.addField(PFN1);
  }

  public static final MmuBuffer DTLB = new MmuBuffer(
      "DTLB", MmuBuffer.Kind.UNMAPPED, 4, 1, VA_ADDR,
      MmuExpression.var(VA, 13, 39), // Tag
      MmuExpression.empty(),         // Index
      MmuExpression.var(VA, 0, 12),  // Offset
      Collections.singleton(new MmuBinding(new IntegerField(VPN2), MmuExpression.var(VA, 13, 39))),
      null, null, true, JTLB
      );

  static {
    DTLB.addField(VPN2);

    DTLB.addField(V0);
    DTLB.addField(D0);
    DTLB.addField(G0);
    DTLB.addField(C0);
    DTLB.addField(PFN0);

    DTLB.addField(V1);
    DTLB.addField(D1);
    DTLB.addField(G1);
    DTLB.addField(C1);
    DTLB.addField(PFN1);
  }

  public static final AddressView<Long> DTLB_ADDR_VIEW = new AddressView<Long>(
      new Function<Long, List<Long>>() {
        @Override
        public List<Long> apply(final Long address) {
          final List<Long> fields = new ArrayList<>();
          // Tag = VPN2.
          fields.add((address >>> 13) & 0x7FFffffL);
          // Index = 0.
          fields.add(0L);
          // Offset = Select | Offset.
          fields.add(address & 0x1fffL);
          return fields;
        }
      }, new Function<List<Long>, Long>() {
        @Override
        public Long apply(final List<Long> fields) {
          final long tag = fields.get(0) & 0x7FFffffL;
          final long offset = fields.get(2) & 0x1fffL;

          return (tag << 13) | offset;
        }
      });

  public static final MmuBuffer L1 = new MmuBuffer(
      "L1", MmuBuffer.Kind.UNMAPPED, 4, 128, PA_ADDR,
      MmuExpression.var(PA, 12, 35), // Tag
      MmuExpression.var(PA, 5, 11), // Index
      MmuExpression.var(PA, 0, 4), // Offset
      Collections.singleton(
          new MmuBinding(new IntegerField(L1_TAG), MmuExpression.var(PA, 12, 35))),
      null, null, true, null
      );

  static {
    L1.addField(L1_TAG);
    L1.addField(L1_DATA);
  }

  public static final AddressView<Long> L1_ADDR_VIEW = new AddressView<Long>(
      new Function<Long, List<Long>>() {
        @Override
        public List<Long> apply(final Long address) {
          final List<Long> fields = new ArrayList<Long>();
          fields.add(((address >>> 12) & 0xFFffffL));
          fields.add((address >>> 5) & 0x7fL);
          fields.add(address & 0x1fL);
          return fields;
        }
      }, new Function<List<Long>, Long>() {
        @Override
        public Long apply(final List<Long> fields) {
          final long tag = fields.get(0) & 0xFFffffL;
          final long index = fields.get(1) & 0x7fL;
          final long offset = fields.get(2) & 0x1fL;

          return (tag << 12) | (index << 5) | offset;
        }
      });

  // -----------------------------------------------------------------------------------------------
  public static final MmuBuffer L2 = new MmuBuffer(
      "L2", MmuBuffer.Kind.UNMAPPED, 4, 4096, PA_ADDR,
      MmuExpression.var(PA, 17, 35), // Tag
      MmuExpression.var(PA, 5, 16), // Index
      MmuExpression.var(PA, 0, 4), // Offset
      Collections.singleton(
          new MmuBinding(new IntegerField(L2_TAG), MmuExpression.var(PA, 17, 35))),
      null, null, true, null
      );

  static {
    L2.addField(L2_TAG);
    L2.addField(L2_DATA);
  }

  public static final AddressView<Long> L2_ADDR_VIEW = new AddressView<Long>(
      new Function<Long, List<Long>>() {
        @Override
        public List<Long> apply(final Long address) {
          final List<Long> fields = new ArrayList<Long>();
          fields.add((address >>> 17) & 0x7ffffL);
          fields.add((address >>> 5) & 0xfffL);
          fields.add(address & 0x1fL);
          return fields;
        }
      }, new Function<List<Long>, Long>() {
        @Override
        public Long apply(final List<Long> fields) {
          final long tag = fields.get(0) & 0x7ffffL;
          final long index = fields.get(1) & 0xfffL;
          final long offset = fields.get(2) & 0x1fL;

          return (tag << 17) | (index << 5) | offset;
        }
      });

  public static final MmuBuffer MEM = new MmuBuffer(
      "MMU", MmuBuffer.Kind.UNMAPPED, 1, (1L << 36) / 32, PA_ADDR,
      MmuExpression.empty(),        // Tag
      MmuExpression.var(PA, 5, 35), // Index
      MmuExpression.var(PA, 0, 4),  // Offset
      Collections.<MmuBinding>emptySet(),
      null, null, false, null
      );

  static {
    MEM.addField(DATA);
  }

  private void runTest(
      final MmuBuffer device, final AddressView<Long> addressView, final long address) {
    System.out.format("Test: %s, %x\n", device.getName(), address);

    final long tagA = addressView.getTag(address);
    final long indexA = addressView.getIndex(address);
    final long offsetA = addressView.getOffset(address);

    final long tagD = device.getTag(address);
    final long indexD = device.getIndex(address);
    final long offsetD = device.getOffset(address);

    System.out.format("Spec: tag=%x, index=%x, offset=%x%n", tagA, indexA, offsetA);
    System.out.format("Impl: tag=%x, index=%x, offset=%x%n", tagD, indexD, offsetD);

    Assert.assertEquals(Long.toHexString(tagA), Long.toHexString(tagD));
    Assert.assertEquals(Long.toHexString(indexA), Long.toHexString(indexD));
    Assert.assertEquals(Long.toHexString(offsetA), Long.toHexString(offsetD));

    final long addressA = addressView.getAddress(tagA, indexA, offsetA);
    final long addressD = device.getAddress(tagD, indexD, offsetD);

    System.out.format("Spec: address=%x%n", addressA);
    System.out.format("Impl: address=%x%n", addressD);

    Assert.assertEquals(Long.toHexString(addressA), Long.toHexString(addressD));
  }

  @Test
  public void runTest() {
    final int testCount = 1000;
    for (int i = 0; i < testCount; i++) {
      runTest(DTLB, DTLB_ADDR_VIEW, Randomizer.get().nextLong());
      runTest(L1, L1_ADDR_VIEW, Randomizer.get().nextLong());
      runTest(L2, L2_ADDR_VIEW, Randomizer.get().nextLong());
    }
  }
}
