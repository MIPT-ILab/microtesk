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
  private static MmuAddressInstance newAddress(final String name, int width) {
    final Type type = new Type(name, Collections.singletonMap("value", new Type(width)));

    return new MmuAddressInstance(
        name,
        new Variable(name, type),
        new IntegerVariable(name + ".value", width));
  }

  public static final MmuAddressInstance VA_ADDR = newAddress("VA", 64);
  public static final MmuAddressInstance PA_ADDR = newAddress("PA", 36);

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
      false, null
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
      true, JTLB
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

  public static final AddressView<BigInteger> DTLB_ADDR_VIEW = new AddressView<BigInteger>(
      new Function<BigInteger, List<BigInteger>>() {
        @Override
        public List<BigInteger> apply(final BigInteger address) {
          final List<BigInteger> fields = new ArrayList<>();
          // Tag = VPN2.
          fields.add(BigInteger.valueOf((address.longValue() >>> 13) & 0x7FFffffL));
          // Index = 0.
          fields.add(BigInteger.ZERO);
          // Offset = Select | Offset.
          fields.add(BigInteger.valueOf(address.longValue() & 0x1fffL));
          return fields;
        }
      },
      new Function<List<BigInteger>, BigInteger>() {
        @Override
        public BigInteger apply(final List<BigInteger> fields) {
          final long tag = fields.get(0).longValue() & 0x7FFffffL;
          final long offset = fields.get(2).longValue() & 0x1fffL;

          return BigInteger.valueOf((tag << 13) | offset);
        }
      });

  public static final MmuBuffer L1 = new MmuBuffer(
      "L1", MmuBuffer.Kind.UNMAPPED, 4, 128, PA_ADDR,
      MmuExpression.var(PA, 12, 35), // Tag
      MmuExpression.var(PA, 5, 11), // Index
      MmuExpression.var(PA, 0, 4), // Offset
      Collections.singleton(
          new MmuBinding(new IntegerField(L1_TAG), MmuExpression.var(PA, 12, 35))),
      true, null
      );

  static {
    L1.addField(L1_TAG);
    L1.addField(L1_DATA);
  }

  public static final AddressView<BigInteger> L1_ADDR_VIEW = new AddressView<BigInteger>(
      new Function<BigInteger, List<BigInteger>>() {
        @Override
        public List<BigInteger> apply(final BigInteger address) {
          final List<BigInteger> fields = new ArrayList<>();
          fields.add(BigInteger.valueOf(((address.longValue() >>> 12) & 0xFFffffL)));
          fields.add(BigInteger.valueOf((address.longValue() >>> 5) & 0x7fL));
          fields.add(BigInteger.valueOf(address.longValue() & 0x1fL));
          return fields;
        }
      },
      new Function<List<BigInteger>, BigInteger>() {
        @Override
        public BigInteger apply(final List<BigInteger> fields) {
          final long tag = fields.get(0).longValue() & 0xFFffffL;
          final long index = fields.get(1).longValue() & 0x7fL;
          final long offset = fields.get(2).longValue() & 0x1fL;

          return BigInteger.valueOf((tag << 12) | (index << 5) | offset);
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
      true, null
      );

  static {
    L2.addField(L2_TAG);
    L2.addField(L2_DATA);
  }

  public static final AddressView<BigInteger> L2_ADDR_VIEW = new AddressView<BigInteger>(
      new Function<BigInteger, List<BigInteger>>() {
        @Override
        public List<BigInteger> apply(final BigInteger address) {
          final List<BigInteger> fields = new ArrayList<BigInteger>();
          fields.add(BigInteger.valueOf((address.longValue() >>> 17) & 0x7ffffL));
          fields.add(BigInteger.valueOf((address.longValue() >>> 5) & 0xfffL));
          fields.add(BigInteger.valueOf(address.longValue() & 0x1fL));
          return fields;
        }
      },
      new Function<List<BigInteger>, BigInteger>() {
        @Override
        public BigInteger apply(final List<BigInteger> fields) {
          final long tag = fields.get(0).longValue() & 0x7ffffL;
          final long index = fields.get(1).longValue() & 0xfffL;
          final long offset = fields.get(2).longValue() & 0x1fL;

          return BigInteger.valueOf((tag << 17) | (index << 5) | offset);
        }
      });

  public static final MmuBuffer MEM = new MmuBuffer(
      "MMU", MmuBuffer.Kind.UNMAPPED, 1, (1L << 36) / 32, PA_ADDR,
      MmuExpression.empty(),        // Tag
      MmuExpression.var(PA, 5, 35), // Index
      MmuExpression.var(PA, 0, 4),  // Offset
      Collections.<MmuBinding>emptySet(),
      false, null
      );

  static {
    MEM.addField(DATA);
  }

  private void runTest(
      final MmuBuffer device, final AddressView<BigInteger> addressView, final BigInteger address) {
    System.out.format("Test: %s, %x\n", device.getName(), address);

    final BigInteger tagA = addressView.getTag(address);
    final BigInteger indexA = addressView.getIndex(address);
    final BigInteger offsetA = addressView.getOffset(address);

    final BigInteger tagD = device.getTag(address);
    final BigInteger indexD = device.getIndex(address);
    final BigInteger offsetD = device.getOffset(address);

    System.out.format("Spec: tag=%x, index=%x, offset=%x%n", tagA, indexA, offsetA);
    System.out.format("Impl: tag=%x, index=%x, offset=%x%n", tagD, indexD, offsetD);

    Assert.assertEquals(tagA.toString(16), tagD.toString(16));
    Assert.assertEquals(indexA.toString(16), indexD.toString(16));
    Assert.assertEquals(offsetA.toString(16), offsetD.toString(16));

    final BigInteger addressA = addressView.getAddress(tagA, indexA, offsetA);
    final BigInteger addressD = device.getAddress(tagD, indexD, offsetD);

    System.out.format("Spec: address=%x%n", addressA);
    System.out.format("Impl: address=%x%n", addressD);

    Assert.assertEquals(addressA.toString(16), addressD.toString(16));
  }

  @Test
  public void runTest() {
    final int testCount = 1000;
    for (int i = 0; i < testCount; i++) {
      runTest(DTLB, DTLB_ADDR_VIEW, BigInteger.valueOf(Randomizer.get().nextLong()));
      runTest(L1, L1_ADDR_VIEW, BigInteger.valueOf(Randomizer.get().nextLong()));
      runTest(L2, L2_ADDR_VIEW, BigInteger.valueOf(Randomizer.get().nextLong()));
    }
  }
}
