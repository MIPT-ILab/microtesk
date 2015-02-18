/*
 * Copyright 2014-2015 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.test.template;

import static ru.ispras.fortress.util.InvariantChecks.checkNotNull;

import java.util.Collections;
import java.util.Map;

import ru.ispras.microtesk.test.sequence.Sequence;
import ru.ispras.microtesk.test.sequence.iterator.IIterator;

public final class Block {
  private final BlockId blockId;
  private final IIterator<Sequence<Call>> iterator;
  private final Map<String, Object> attributes;

  Block(BlockId blockId, IIterator<Sequence<Call>> iterator, Map<String, Object> attributes) {
    checkNotNull(blockId);
    checkNotNull(iterator);
    checkNotNull(attributes);

    this.blockId = blockId;
    this.iterator = iterator;
    this.attributes = attributes;
  }

  Block(BlockId blockId, IIterator<Sequence<Call>> iterator) {
    this(blockId, iterator, Collections.<String, Object>emptyMap());
  }

  public IIterator<Sequence<Call>> getIterator() {
    return iterator;
  }

  public Object getAttribute(String name) {
    return attributes.get(name);
  }

  public BlockId getBlockId() {
    return blockId;
  }
}
