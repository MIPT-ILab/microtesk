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

package ru.ispras.microtesk.mmu.test.sequence.engine.memory.filter;

import java.util.Collection;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.MemoryAccess;
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.MemoryAccessStructure;
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.MemoryDependency;
import ru.ispras.microtesk.mmu.test.sequence.engine.memory.MemoryUnitedDependency;
import ru.ispras.microtesk.utils.function.BiPredicate;
import ru.ispras.microtesk.utils.function.Predicate;
import ru.ispras.microtesk.utils.function.TriPredicate;

/**
 * {@link FilterStructure} composes execution- and dependency-level filters into a template-level
 * filter.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class FilterStructure implements Predicate<MemoryAccessStructure> {
  private final Collection<TriPredicate<MemoryAccess, MemoryAccess, MemoryDependency>> dependencyFilters;
  private final Collection<BiPredicate<MemoryAccess, MemoryUnitedDependency>> unitedDependencyFilters;

  /**
   * Constructs a template-level filter from dependency-level filters.
   * 
   * @param dependencyFilters the collection of dependency-level filters.
   * @param unitedDependencyFilters the collection of united-dependency-level filters.
   * @throws IllegalArgumentException if some parameters are null.
   */
  public FilterStructure(
      final Collection<TriPredicate<MemoryAccess, MemoryAccess, MemoryDependency>> dependencyFilters,
      final Collection<BiPredicate<MemoryAccess, MemoryUnitedDependency>> unitedDependencyFilters) {
    InvariantChecks.checkNotNull(dependencyFilters);
    InvariantChecks.checkNotNull(unitedDependencyFilters);

    this.dependencyFilters = dependencyFilters;
    this.unitedDependencyFilters = unitedDependencyFilters;
  }

  @Override
  public boolean test(final MemoryAccessStructure template) {
    for (int i = 0; i < template.size(); i++) {
      final MemoryAccess execution1 = template.getAccess(i);

      for (int j = i + 1; j < template.size(); j++) {
        final MemoryAccess execution2 = template.getAccess(j);
        final MemoryDependency dependency = template.getDependency(i, j);

        if (dependency == null) {
          continue;
        }

        // Apply the dependency-level filters.
        for (final TriPredicate<MemoryAccess, MemoryAccess, MemoryDependency> filter : dependencyFilters) {
          if (!filter.test(execution1, execution2, dependency)) {
            // Filter off.
            return false;
          }
        }
      }

      final MemoryUnitedDependency unitedDependency = template.getUnitedDependency(i);

      // Apply the united-dependency-level filters.
      for (final BiPredicate<MemoryAccess, MemoryUnitedDependency> filter : unitedDependencyFilters) {
        if (!filter.test(execution1, unitedDependency)) {
          // Filter off.
          return false;
        }
      }
    }

    return true;
  }
}
