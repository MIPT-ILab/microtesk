/*
 * Copyright 2015-2016 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.model.api.metadata;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.model.api.data.Type;

/**
 * The {@code MetaGroup} class describes a group of metadata items
 * such as addressing modes and operations.
 * 
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public class MetaGroup implements MetaData {
  /**
   * Specifies the kind of stored items.
   */
  public enum Kind {
    /** Addressing mode */
    MODE,
    /** Operation */
    OP
  }

  private final Kind kind;
  private final String name;
  private final Collection<? extends MetaData> items;
  private final Type dataType;

  /**
   * Constructs a {@code MetaGroup} object.
   * 
   * @param kind Kind of items being grouped.
   * @param name Group name.
   * @param items Items being grouped.
   * 
   * @throws IllegalArgumentException if any parameter is {@code null};
   *         if the collection of items is empty.
   */
  public MetaGroup(
      final Kind kind,
      final String name,
      final MetaData... items) {
    this(kind, name, Arrays.asList(items));
  }

  /**
   * Constructs a {@code MetaGroup} object.
   * 
   * @param kind Kind of items being grouped.
   * @param name Group name.
   * @param items Items being grouped.
   * 
   * @throws IllegalArgumentException if any parameter is {@code null};
   *         if the collection of items is empty.
   */
  public MetaGroup(
      final Kind kind,
      final String name,
      final Collection<? extends MetaData> items) {
    InvariantChecks.checkNotNull(kind);
    InvariantChecks.checkNotNull(name);
    InvariantChecks.checkNotEmpty(items);

    this.kind = kind;
    this.name = name;
    this.items = Collections.unmodifiableCollection(items);
    this.dataType = items.iterator().next().getDataType();
  }

  /**
   * Returns the kind of stored items.
   * 
   * @return Item kind.
   */
  public final Kind getKind() {
    return kind;
  }

  /**
   * Returns the name of the group.
   * 
   * @return Group name.
   */
  @Override
  public final String getName() {
    return name;
  }

  @Override
  public final Type getDataType() {
    return dataType;
  }

  /**
   * Returns items being grouped.
   * 
   * @return Items being grouped (an {@link Iterable} object).
   */
  public final Collection<? extends MetaData> getItems() {
    return items;
  }
}
