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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The {@link MetaOperation} class stores information on the given operation.
 * 
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public class MetaOperation implements MetaData {
  private final String name;
  private final String typeName;
  private final boolean isRoot;

  private final Map<String, MetaArgument> args;
  private final Map<String, MetaShortcut> shortcuts;
  private boolean hasRootShortcuts;

  private final boolean branch;
  private final boolean conditionalBranch;
  private final boolean exception;

  private final boolean load;
  private final boolean store;
  private final int blockSize;

  public MetaOperation(
      final String name,
      final String typeName,
      final boolean isRoot,
      final Map<String, MetaArgument> args,
      final Map<String, MetaShortcut> shortcuts,
      final boolean branch,
      final boolean conditionalBranch,
      final boolean exception,
      final boolean load,
      final boolean store,
      final int blockSize) {
    InvariantChecks.checkNotNull(name);
    InvariantChecks.checkNotNull(typeName);
    InvariantChecks.checkNotNull(args);
    InvariantChecks.checkNotNull(shortcuts);

    if (!branch) {
      InvariantChecks.checkFalse(conditionalBranch);
    }

    if (conditionalBranch) {
      InvariantChecks.checkTrue(branch);
    }

    this.name = name;
    this.typeName = typeName;
    this.isRoot = isRoot;
    this.args = args;
    this.shortcuts = shortcuts;

    boolean rootShortcuts = false;
    for (final MetaShortcut ms : shortcuts.values()) {
      if (ms.getOperation().isRoot()) {
        rootShortcuts = true;
        break;
      }
    }

    this.hasRootShortcuts = rootShortcuts;
    this.branch = branch;
    this.conditionalBranch = conditionalBranch;
    this.exception = exception;

    this.load = load;
    this.store = store;
    this.blockSize = blockSize;
  }

  protected MetaOperation(
      final String name,
      final String typeName,
      final boolean isRoot,
      final boolean branch,
      final boolean conditionalBranch,
      final boolean exception,
      final boolean load,
      final boolean store,
      final int blockSize) {
    this(
        name,
        typeName,
        isRoot,
        new LinkedHashMap<String, MetaArgument>(),
        new LinkedHashMap<String, MetaShortcut>(),
        branch,
        conditionalBranch,
        exception,
        load,
        store,
        blockSize
        );
  }

  protected final void addArgument(final MetaArgument argument) {
    InvariantChecks.checkNotNull(argument);
    args.put(argument.getName(), argument);
  }

  protected final void addShortcut(final String contextName, final MetaOperation operation) {
    InvariantChecks.checkNotNull(contextName);
    InvariantChecks.checkNotNull(operation);

    if (operation.isRoot) {
      hasRootShortcuts = true;
    }

    shortcuts.put(contextName, new MetaShortcut(contextName, operation));
  }

  /**
   * Returns the operation name.
   * 
   * @return The operation name.
   */
  @Override
  public final String getName() {
    return name;
  }

  /**
   * Returns the type name of the operation. If a meta operation describes a shortcut the type name
   * is different from the name. The name means the name of the target operation, while the type
   * name means the name of the entry operation (that encapsulates the path to the target).
   * 
   * @return The operation type name (for composite operation the name of the topmost operation).
   */
  public final String getTypeName() {
    return typeName;
  }

  @Override
  public final Type getDataType() {
    return null;
  }

  /**
   * Checks whether the current operation is a root. An operation is a root if it does not have
   * parents.
   * 
   * @return {@code true} if it is a root operation or {@code false} otherwise.
   */
  public final boolean isRoot() {
    return isRoot;
  }

  /**
   * Returns a collection of operation arguments.
   * 
   * @return Collection of operation arguments.
   */
  public final Iterable<MetaArgument> getArguments() {
    return args.values();
  }

  /**
   * Return an argument of the given operation that has the specified name.
   * 
   * @param name Argument name.
   * @return Argument with the specified name or {@code null} if no such argument is defined.
   */
  public final MetaArgument getArgument(final String name) {
    return args.get(name);
  }

  /**
   * Returns a collection of shortcuts applicable to the given operation in different contexts.
   * 
   * @return A collection of shortcuts.
   */
  public final Iterable<MetaShortcut> getShortcuts() {
    return shortcuts.values();
  }

  /**
   * Returns a shortcut for the given operation that can be used in the specified context.
   * 
   * @param contextName Context name.
   * @return Shortcut for the given context or {@code null} if no such shortcut exists.
   */
  public final MetaShortcut getShortcut(final String contextName) {
    return shortcuts.get(contextName);
  }

  /**
   * Checks whether the current operation has root shortcuts. This means that the operation can be
   * addressed directly in a test template to specify a complete instruction call (not as a part of
   * a call specified as an argument for other operation).
   * 
   * @return {@code true} if it the operation has root shortcuts or {@code false} otherwise.
   */
  public final boolean hasRootShortcuts() {
    return hasRootShortcuts;
  }

  /**
   * Checks whether the operation is a branch operation (causes control transfer).
   * 
   * @return {@code true} if the operation is a branch operation
   * or {@code false} otherwise.
   */
  public final boolean isBranch() {
    return branch;
  }

  /**
   * Checks whether the operation is a conditional branch operation
   * (causes control transfer on some condition).
   * 
   * @return {@code true} if the operation is a conditional branch operation
   * or {@code false} otherwise.
   */
  public final boolean isConditionalBranch() {
    return conditionalBranch;
  }

  /**
   * Checks the operation has a delay slot.
   *
   * @return count of delay slots.
   */
  public final int getDelaySlotSize() {
    // TODO:
    return 0;
  }

  /**
   * Checks whether the current operation can throw an exception.
   * 
   * @return {@code true} if the operation can throw an exception
   * or {@code false} otherwise.
   */
  public final boolean canThrowException() {
    return exception;
  }

  /**
   * Checks whether the operation performs a memory load action.
   * 
   * <p>NOTE: This covers only situations when a load can be unambiguously defected
   * by exploring operation attributes. For situations when a memory access
   * is performed via parameters (other operation or addressing modes) which
   * can be dynamically chosen (OR rules), these parameters (their meta data) 
   * must be examined additionally to make a conclusion.
   * 
   * @return {@code true} if the operation performs a memory load action
   * or {@code false} otherwise.
   */
  public final boolean isLoad() {
    return load;
  }

  /**
   * Checks whether the operation performs a memory store action.
   * 
   * <p>NOTE: This covers only situations when a load can be unambiguously defected
   * by exploring operation attributes. For situations when a memory access
   * is performed via parameters (other operation or addressing modes) which
   * can be dynamically chosen (OR rules), these parameters (their meta data)
   * must be examined additionally to make a conclusion.
   * 
   * @return {@code true} if the operation performs a memory store action
   * or {@code false} otherwise.
   */
  public final boolean isStore() {
    return store;
  }

  /**
   * Returns the size of block read or written to memory. Applicable
   * for load or store operations.
   * 
   * @return Size of memory block in bits.
   */
  public final int getBlockSize() {
    return blockSize;
  }

  /**
   * Returns the list of operation exceptions.
   *
   * @return a list of exceptions.
   */
  public final Collection<String> getExceptions() {
    // TODO:
    return null;
  }

  /**
   * Returns the list of operation marks.
   *
   * @return a list of marks.
   */
  public final Collection<String> getMarks() {
    // TODO:
    return null;
  }

  /**
   * Returns the operation arguments.
   *
   * @return the operation signature.
   */
  public final Object getSignature() {
    // TODO: Object -> Signature
    return null;
  }
}
