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

import java.math.BigInteger;

/**
 * The {@link MmuMapping} class describes a buffer mapped to memory.
 * An access to such a buffer causes a access to memory by virtual
 * address using MMU (address translation, caches, physical memory).
 *
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 *
 * @param <D> Data type.
 * @param <A> Address type.
 */
public abstract class MmuMapping<D extends Data, A extends Address & Data>
    implements Buffer<D, A>, BufferObserver {

  private final BigInteger length;
  private final int associativity;
  private final PolicyId policyId;

  private final Indexer<A> indexer;
  private final Matcher<D, A> matcher;

  /**
   * Constructs a memory-mapped buffer of the given length and associativity.
   *
   * @param length the number of sets in the buffer.
   * @param associativity the number of lines in each set.
   * @param policyId the data replacement policy.
   * @param indexer the set indexer.
   * @param matcher the line matcher.
   */
  public MmuMapping(
      final BigInteger length,
      final int associativity,
      final PolicyId policyId,
      final Indexer<A> indexer,
      final Matcher<D, A> matcher) {
    InvariantChecks.checkNotNull(length);
    InvariantChecks.checkGreaterThan(length, BigInteger.ZERO);
    InvariantChecks.checkGreaterThanZero(associativity);
    InvariantChecks.checkNotNull(policyId);
    InvariantChecks.checkNotNull(indexer);
    InvariantChecks.checkNotNull(matcher);

    this.length = length;
    this.associativity = associativity;
    this.policyId = policyId;
    this.indexer = indexer;
    this.matcher = matcher;
  }

  @Override
  public boolean isHit(final A address) {
    // TODO
    return getMmu().isHit(address);
  }

  @Override
  public boolean isHit(final BitVector value) {
    final A address = newAddress();
    address.getValue().assign(value);
    return isHit(address);
  }

  @Override
  public D getData(final A address) {
    final BitVector value = getMmu().getData(address);
    InvariantChecks.checkTrue(value.getBitSize() == getDataBitSize());
    return newData(value);
  }

  @Override
  public D setData(final A address, final D data) {
    final BitVector value = data.asBitVector();
    InvariantChecks.checkTrue(value.getBitSize() == getDataBitSize());
    getMmu().setData(address, value);
    return null;
  }

  @Override
  public Pair<BitVector, BitVector> seeData(BitVector index, BitVector way) {
    // NOT SUPPORTED
    throw new UnsupportedOperationException();
  }

  protected abstract Mmu<A> getMmu();

  protected abstract A newAddress();

  protected abstract D newData(final BitVector value);

  protected abstract int getDataBitSize();
}
