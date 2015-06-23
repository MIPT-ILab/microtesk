/*
 * Copyright 2009-2015 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.test.sequence.branch.internal;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.test.sequence.iterator.Iterator;

/**
 * {@link BranchStructureExecutionIterator} implements a composite iterator of branch structures and
 * execution traces.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class BranchStructureExecutionIterator implements Iterator<BranchStructure> {
  private final int maxBranchExecution;
  private final Iterator<BranchStructure> branchStructureIterator;
  private BranchTraceIterator branchTraceIterator;
  private boolean hasValue;

  public BranchStructureExecutionIterator(
      final Iterator<BranchStructure> branchStructureIterator,
      final int maxBranchExecution) {
    InvariantChecks.checkNotNull(branchStructureIterator);
    InvariantChecks.checkTrue(maxBranchExecution >= 0);

    this.branchStructureIterator = branchStructureIterator;
    this.maxBranchExecution = maxBranchExecution;

    init();
  }

  private boolean initBranchStructureIterator() {
    branchStructureIterator.init();

    while (branchStructureIterator.hasValue()) {
      if (initBranchTraceIterator()) {
        return true;
      }

      branchStructureIterator.next();
    }

    return branchStructureIterator.hasValue();
  }

  private boolean initBranchTraceIterator() {
    branchTraceIterator = new BranchTraceIterator(branchStructureIterator.value(), maxBranchExecution);
    branchTraceIterator.init();

    return branchTraceIterator.hasValue();
  }

  @Override
  public void init() {
    hasValue = true;

    if (!initBranchStructureIterator()) {
      stop();
      return;
    }
    if (!initBranchTraceIterator()) {
      stop();
      return;
    }
  }

  @Override
  public boolean hasValue() {
    return hasValue;
  }

  @Override
  public BranchStructure value() {
    return branchTraceIterator.value();
  }

  private boolean nextBranchStructureIterator() {
    if (branchStructureIterator.hasValue()) {
      branchStructureIterator.next();

      while (branchStructureIterator.hasValue()) {
        if (initBranchTraceIterator()) {
          break;
        }

        branchStructureIterator.next();
      }
    }

    return branchStructureIterator.hasValue();
  }

  private boolean nextBranchTraceIterator() {
    if (branchTraceIterator.hasValue()) {
      branchTraceIterator.next();
    }

    return branchTraceIterator.hasValue();
  }

  @Override
  public void next() {
    if (!hasValue()) {
      return;
    }
    if (nextBranchTraceIterator()) {
      return;
    }
    if (nextBranchStructureIterator()) {
      return;
    }

    stop();
  }

  public void stop() {
    hasValue = false;
  }
}
