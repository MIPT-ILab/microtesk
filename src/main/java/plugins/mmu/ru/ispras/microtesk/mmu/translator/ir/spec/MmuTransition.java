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

import java.util.Collection;
import java.util.LinkedHashSet;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.mmu.basis.MemoryAccessContext;

/**
 * {@link MmuTransition} represents a transition, which is a link between two {@link MmuAction}.
 * 
 * <p>The description includes the source and the target actions as well as the guard condition that
 * activates the transition.</p>
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class MmuTransition {
  /** Source action. */
  private final MmuAction source;
  /** Target action. */
  private final MmuAction target;
  /** Guard condition or {@code null} if the transition is interpreted as {@code goto}. */
  private final MmuGuard guard;

  public MmuTransition(final MmuAction source, final MmuAction target, final MmuGuard guard) {
    InvariantChecks.checkNotNull(source);
    InvariantChecks.checkNotNull(target);

    this.source = source;
    this.target = target;
    this.guard = guard;
  }

  public MmuTransition(final MmuAction source, final MmuAction target) {
    this(source, target, null);
  }

  public MmuAction getSource() {
    return source;
  }

  public MmuAction getTarget() {
    return target;
  }

  public MmuGuard getGuard() {
    return guard;
  }

  public Collection<MmuBufferAccess> getBufferAccesses(final MemoryAccessContext context) {
    final Collection<MmuBufferAccess> result = new LinkedHashSet<>();

    if (guard != null) {
      final MmuBufferAccess bufferAccess = guard.getBufferAccess(context);

      if (bufferAccess != null) {
        result.add(bufferAccess);
      }
    }

    if (target != null) {
      final MmuBufferAccess bufferAccess = target.getBufferAccess(context);

      if (bufferAccess != null) {
        result.add(bufferAccess);
      }
    }

    return result;
  }
  
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();

    builder.append("[");
    builder.append(source);
    builder.append("]");
    builder.append(" -> ");
    if (guard != null) {
      builder.append("[");
      builder.append(guard);
      builder.append("] -> ");
    }
    builder.append("[");
    builder.append(target);
    builder.append("]");

    return builder.toString();
  }
}
