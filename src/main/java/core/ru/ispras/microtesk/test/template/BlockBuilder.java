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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import ru.ispras.microtesk.test.sequence.Generator;
import ru.ispras.microtesk.test.sequence.GeneratorBuilder;
import ru.ispras.microtesk.test.sequence.Sequence;
import ru.ispras.microtesk.test.sequence.iterator.IIterator;
import ru.ispras.microtesk.test.sequence.iterator.SingleValueIterator;

public final class BlockBuilder {
  private final BlockId blockId;

  private List<Block> nestedBlocks;
  private Map<String, Object> attributes;

  private String compositorName;
  private String combinatorName;

  private boolean isAtomic;

  BlockBuilder() {
    this(new BlockId());
  }

  BlockBuilder(BlockBuilder parent) {
    this(parent.getBlockId().nextChildId());
  }

  private BlockBuilder(BlockId blockId) {
    this.blockId = blockId;

    this.nestedBlocks = new ArrayList<Block>();
    this.attributes = new HashMap<String, Object>();

    this.compositorName = null;
    this.combinatorName = null;
    
    this.isAtomic = false;
  }

  public BlockId getBlockId() {
    return blockId;
  }

  public boolean isEmpty() {
    // A block is empty if it contains no nested blocks or calls.
    // Attributes affect only the block itself and are not important 
    // from an external point of view.

    return nestedBlocks.isEmpty();  
  }

  public void setCompositor(String name) {
    assert null == compositorName;
    compositorName = name;
  }

  public void setCombinator(String name) {
    assert null == combinatorName;
    combinatorName = name;
  }
  
  public void setAtomic(boolean value) {
    this.isAtomic = value;
  }

  public void setAttribute(String name, Object value) {
    assert !attributes.containsKey(name);
    attributes.put(name, value);
  }

  public void addBlock(Block block) {
    checkNotNull(block);
    nestedBlocks.add(block);
  }

  public void addCall(Call call) {
    checkNotNull(call);

    if (call.isEmpty()) {
      return;
    }

    final Sequence<Call> sequence = new Sequence<Call>();
    sequence.add(call);

    final IIterator<Sequence<Call>> iterator = new SingleValueIterator<Sequence<Call>>(sequence);
    nestedBlocks.add(new Block(blockId, iterator));
  }

  public Block build() {
    final GeneratorBuilder<Call> generatorBuilder = new GeneratorBuilder<Call>();
    generatorBuilder.setSingle(isAtomic);

    if (null != combinatorName) {
      generatorBuilder.setCombinator(combinatorName);
    }

    if (null != compositorName) {
      generatorBuilder.setCompositor(compositorName);
    }

    for (Block block : nestedBlocks) {
      generatorBuilder.addIterator(block.getIterator());
    }

    final Generator<Call> generator = generatorBuilder.getGenerator();
    return new Block(blockId, generator, attributes);
  }
}
