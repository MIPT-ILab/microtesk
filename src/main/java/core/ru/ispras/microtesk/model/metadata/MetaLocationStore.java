/*
 * Copyright 2012-2018 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.model.metadata;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.model.data.Type;

import java.math.BigInteger;

/**
 * The MetaLocationStore class describes memory resources of the processor (as registers and memory
 * store locations).
 * 
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class MetaLocationStore implements MetaData {
  private final String name;
  private final Type dataType;
  private final BigInteger count;

  public MetaLocationStore(
      final String name,
      final Type dataType, 
      final BigInteger count) {
    InvariantChecks.checkNotNull(name);
    InvariantChecks.checkNotNull(count);
    InvariantChecks.checkGreaterThan(count, BigInteger.ZERO);

    this.name = name;
    this.dataType = dataType;
    this.count = count;
  }

  /**
   * Returns the name of the resource.
   * 
   * @return Memory resource name.
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Returns the type of items stored in the memory store.
   * 
   * @return Item data type.
   */
  @Override
  public Type getDataType() {
    return dataType;
  }

  /**
   * Returns the count of items in the memory store.
   * 
   * @return Memory store item count.
   */
  public BigInteger getCount() {
    return count;
  }
}
