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

package ru.ispras.microtesk.mmu.model.sim;

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.fortress.util.Pair;
import ru.ispras.microtesk.model.memory.MemoryDevice;
import ru.ispras.microtesk.test.TestEngine;
import ru.ispras.microtesk.utils.SparseArray;

import java.math.BigInteger;

/**
 * The {@link RegisterMapping} class implements a register-mapped buffer.
 *
 * @param <D> the data type.
 * @param <A> the address type.
 *
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public abstract class RegisterMapping<D extends Data, A extends Address>
    implements Buffer<D, A>, BufferObserver {

  private final String name;

  private final int associativity;
  private final PolicyId policyId;

  private final Indexer<A> indexer;
  private final Matcher<D, A> matcher;

  private final SparseArray<Buffer<D, A>> sets;
  private BigInteger currentRegisterIndex;

  /**
   * The {@link RegisterMappedSet} class is an extension of the {@link Set} class
   * for register-mapped buffers.
   */
  private final class RegisterMappedSet extends Set<D, A> {
    public RegisterMappedSet() {
      super(associativity, policyId, matcher);
    }

    @Override
    protected Buffer<D, A> newLine(final Matcher<D, A> matcher) {
      return new RegisterMappedLine();
    }
  }

  /**
   * The {@link RegisterMappedLine} class is an implementation of a line
   * for register-mapped buffers.
   */
  private final class RegisterMappedLine implements Buffer<D, A> {
    private final BitVector registerIndex;

    private RegisterMappedLine() {
      final MemoryDevice storage = getRegisterDevice();
      this.registerIndex = BitVector.valueOf(currentRegisterIndex, storage.getAddressBitSize());
      currentRegisterIndex = currentRegisterIndex.add(BigInteger.ONE);
    }

    @Override
    public boolean isHit(final A address) {
      final MemoryDevice storage = getRegisterDevice();
      if (!storage.isInitialized(registerIndex)) {
        return false;
      }

      final BitVector rawData = storage.load(registerIndex);
      final D data = newData(rawData);

      return matcher.areMatching(data, address);
    }

    @Override
    public D getData(final A address) {
      final MemoryDevice storage = getRegisterDevice();
      final BitVector rawData = storage.load(registerIndex);
      return newData(rawData);
    }

    @Override
    public D setData(final A address, final D data) {
      final MemoryDevice storage = getRegisterDevice();
      storage.store(registerIndex, data.asBitVector());
      return null;
    }

    @Override
    public Pair<BitVector, BitVector> seeData(final BitVector index, final BitVector way) {
      final MemoryDevice storage = getRegisterDevice();
      return storage.isInitialized(registerIndex)
          ? new Pair<>(registerIndex, storage.load(registerIndex))
          : null
          ;
    }

    @Override
    public String toString() {
      final MemoryDevice storage = getRegisterDevice();
      final BitVector value = storage.load(registerIndex);
      return String.format("RegisterMappedLine [data=%s]", newData(value));
    }
  }

  /**
   * The {@link Proxy} class is used to simplify code of assignment expressions.
   */
  public final class Proxy {
    private final A address;

    private Proxy(final A address) {
      this.address = address;
    }

    public D assign(final D data) {
      return setData(address, data);
    }

    public D assign(final BitVector value) {
      final D data = newData(value);
      return setData(address, data);
    }
  }

  /**
   * Constructs a register-mapped buffer of the given length and associativity.
   *
   * @param name Name of the register file mapped to the buffer.
   * @param length the number of sets in the buffer.
   * @param associativity the number of lines in each set.
   * @param policyId the data replacement policy.
   * @param indexer the set indexer.
   * @param matcher the line matcher.
   */
  public RegisterMapping(
      final String name,
      final BigInteger length,
      final int associativity,
      final PolicyId policyId,
      final Indexer<A> indexer,
      final Matcher<D, A> matcher) {
    InvariantChecks.checkNotNull(name);
    InvariantChecks.checkNotNull(length);
    InvariantChecks.checkGreaterThan(length, BigInteger.ZERO);
    InvariantChecks.checkGreaterThanZero(associativity);
    InvariantChecks.checkNotNull(policyId);
    InvariantChecks.checkNotNull(indexer);
    InvariantChecks.checkNotNull(matcher);

    this.name = name;

    final MemoryDevice storage = getRegisterDevice();
    InvariantChecks.checkTrue(getDataBitSize() == storage.getDataBitSize());

    this.associativity = associativity;
    this.policyId = policyId;
    this.indexer = indexer;
    this.matcher = matcher;

    this.sets = new SparseArray<>(length);
    this.currentRegisterIndex = BigInteger.ZERO;

    for (BigInteger index = BigInteger.ZERO;
         index.compareTo(length) < 0;
         index = index.add(BigInteger.ONE)) {
      final Buffer<D, A> set = new RegisterMappedSet();
      final BitVector setIndex = BitVector.valueOf(index, storage.getAddressBitSize());
      sets.set(setIndex, set);
    }
  }

  private MemoryDevice getRegisterDevice() {
    return TestEngine.getInstance().getModel().getPE().getMemoryDevice(name);
  }

  @Override
  public final boolean isHit(final A address) {
    final Buffer<D, A> set = getSet(address);
    return null != set && set.isHit(address);
  }

  @Override
  public final boolean isHit(final BitVector value) {
    final A address = newAddress();
    address.getValue().assign(value);
    return isHit(address);
  }

  @Override
  public final D getData(final A address) {
    final Buffer<D, A> set = getSet(address);
    return set.getData(address);
  }

  @Override
  public final D setData(final A address, final D data) {
    final Buffer<D, A> set = getSet(address);
    return set.setData(address, data);
  }

  public final Proxy setData(final A address) {
    return new Proxy(address);
  }

  @Override
  public Pair<BitVector, BitVector> seeData(final BitVector index, final BitVector way) {
    final Buffer<D, A> set = sets.get(index);
    return null != set ? set.seeData(index, way) : null;
  }

  private Buffer<D, A> getSet(final A address) {
    final BitVector index = indexer.getIndex(address);
    return sets.get(index);
  }

  protected abstract A newAddress();

  protected abstract D newData(final BitVector value);

  protected abstract int getDataBitSize();

  @Override
  public String toString() {
    return String.format("%s %s", getClass().getSimpleName(), sets);
  }
}
