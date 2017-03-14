/*
 * Copyright 2015-2017 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.mmu.test.sequence.engine.memory.filter;

import ru.ispras.microtesk.mmu.basis.BufferAccessEvent;
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.BufferHazard;
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.BufferUnitedHazard;
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.MemoryAccess;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuBufferAccess;
import ru.ispras.microtesk.utils.function.BiPredicate;

/**
 * Filters off test templates, where {@code TAG_REPLACED} and {@code HIT} are simultaneously set for
 * the same device.
 * 
 * <p>NOTE: In a more general case, replace-use chains should not include reloads.</p>
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class FilterHitAndTagReplaced
    implements BiPredicate<MemoryAccess, BufferUnitedHazard> {

  @Override
  public boolean test(final MemoryAccess access, final BufferUnitedHazard hazard) {
    final MmuBufferAccess bufferAccess = hazard.getBufferAccess();

    if (bufferAccess != null) {
      if (bufferAccess.getEvent() == BufferAccessEvent.HIT) {
        if (!hazard.getRelation(BufferHazard.Type.TAG_REPLACED).isEmpty()) {
          // Filter off.
          return false;
        }
      }
    }

    return true;
  }
}
