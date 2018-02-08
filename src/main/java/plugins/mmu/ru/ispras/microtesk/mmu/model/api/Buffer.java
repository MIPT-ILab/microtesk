/*
 * Copyright 2012-2015 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.mmu.model.api;

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.util.Pair;

/**
 * This is a generic interface of a buffer (i.e., a component that stores addressable data).
 *
 * @param <D> the data type.
 * @param <A> the address type.
 *
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public interface Buffer<D, A> {
  /**
   * Checks whether the given address causes a hit.
   * 
   * @param address the data address.
   * @return {@code true} if the address causes a hit; {@code false} otherwise.
   */
  boolean isHit(final A address);

  /**
   * Returns the data associated with the given address.
   *
   * @param address the data address.
   * @return the data object if the address causes a hit; {@code null} otherwise.
   */
  D getData(final A address);

  /**
   * Updates the data associated with the given address.
   *
   * @param address the data address.
   * @param data the new data.
   *
   * @return the old data if they exist; {@code null} otherwise.
   */
  D setData(final A address, final D data);

  /**
   * Returns data and associated address without changing the state.
   *
   * @param index Set index.
   * @param way Line index.
   * @return Pair(Address, Data) or {@code null} if it is not found.
   */
  Pair<BitVector, BitVector> seeData(final BitVector index, final BitVector way);
}
