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
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.MemoryAccess;
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.MemoryAccessPath;
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.BufferHazard;
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.BufferUnitedHazard;
import ru.ispras.microtesk.mmu.translator.ir.spec.MmuBufferAccess;
import ru.ispras.microtesk.utils.function.BiPredicate;

/**
 * Filters off test templates, where there is a hit or a replace in a child device (e.g. DTLB) and
 * a miss in the parent device (e.g. JTLB).
 * 
 * <p>NOTE: Such test templates are unsatisfiable.</p>
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class FilterParentMissChildHitOrReplace
    implements BiPredicate<MemoryAccess, BufferUnitedHazard> {

  @Override
  public boolean test(final MemoryAccess access, final BufferUnitedHazard hazard) {
    final MmuBufferAccess viewAccess = hazard.getBufferAccess();

    if (viewAccess != null && viewAccess.getBuffer().isView()) {
      final MmuBufferAccess parentAccess = viewAccess.getParentAccess();
      final MemoryAccessPath path = access.getPath();

      final boolean isViewAccessed = path.getEvent(viewAccess) == BufferAccessEvent.HIT
          || !hazard.getRelation(BufferHazard.Type.TAG_REPLACED).isEmpty();

      if (path.getEvent(parentAccess) == BufferAccessEvent.MISS && isViewAccessed) {
        // Filter off.
        return false;
      }
    }

    return true;
  }
}

