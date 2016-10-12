/*
 * Copyright 2016 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.model.api;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.model.api.memory.Label;
import ru.ispras.microtesk.model.api.memory.Memory;
import ru.ispras.microtesk.model.api.memory.MemoryAllocator;
import ru.ispras.microtesk.model.api.memory.MemoryDevice;
import ru.ispras.microtesk.model.api.state.LocationAccessor;

public abstract class Core {
  private final Map<String, Memory> storageMap = new HashMap<>();
  private final Map<String, Label> labelMap = new HashMap<>();
  private final List<Memory> variables = new ArrayList<>();

  protected final void addStorage(final Memory storage) {
    InvariantChecks.checkNotNull(storage);
    this.storageMap.put(storage.getName(), storage);

    if (Memory.Kind.VAR == storage.getKind()) {
      variables.add(storage);
    }
  }

  protected final void addLabel(final Label label) {
    InvariantChecks.checkNotNull(label);
    this.labelMap.put(label.getName(), label);
  }

  public final LocationAccessor accessLocation(final String name) {
    return accessLocation(name, BigInteger.ZERO);
  }

  public final LocationAccessor accessLocation(final String name, final BigInteger index) {
    InvariantChecks.checkNotNull(name);

    final Label label = labelMap.get(name);
    if (null != label) {
      if (null != index && !index.equals(BigInteger.ZERO)) {
        throw new IllegalArgumentException(
            String.format("The %d index is invalid for the %s storage.", index, name));
      }

      return label.access();
    }

    final Memory storage = storageMap.get(name);
    if (null == storage) {
      throw new IllegalArgumentException(
          String.format("The %s storage is not defined in the model.", name));
    }

    return storage.access(index);
  }

  public final void resetVariables() {
    for (final Memory variable : variables) {
      variable.reset();
    }
  }

  public final MemoryAllocator newAllocator(
      final String storageId,
      final int addressableUnitBitSize,
      final BigInteger baseAddress) {
    final Memory storage = getStorage(storageId);
    return storage.newAllocator(addressableUnitBitSize, baseAddress);
  }

  public final MemoryDevice setHandler(
      final String storageId,
      final MemoryDevice handler) {
    final Memory storage = getStorage(storageId);
    return storage.setHandler(handler);
  }

  private Memory getStorage(final String id) {
    final Memory storage = storageMap.get(id);
    if (null == storage) {
      throw new IllegalArgumentException(
          String.format("The %s storage is not defined in the model.", id));
    }
    return storage;
  }
}
