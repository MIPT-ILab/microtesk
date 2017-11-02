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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.settings.GeneratorSettings;
import ru.ispras.microtesk.settings.RegionSettings;

/**
 * {@link MmuSubsystem} describes a memory management unit (MMU).
 *
 * <p>The description includes a set of buffers and a network (directed acyclic graph with one
 * source and multiple sink nodes) of actions.</p>
 *
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class MmuSubsystem {
  /** Name of MMU */
  private final String name;

  /** Stores available variables. */
  private final Map<String, NodeVariable> variables;

  /** Refers to the data variable type of the MMU. */
  private final NodeVariable dataVariable;

  /**
   * Stores available address types.
   * <p>Typically, includes two types: Virtual Address and Physical Address.</p>
   */
  private final Map<String, MmuAddressInstance> addresses;

  // TODO:
  private final List<MmuAddressInstance> sortedAddresses;

  /** Refers to the virtual address type of the MMU. */
  private final MmuAddressInstance virtualAddress;

  /** Refers to the physical address type of the MMU. */
  private final MmuAddressInstance physicalAddress;

  /** Stores operations of MMU (linked to with operations in SA specs) */
  private final Map<String, MmuOperation> operations;

  /** Stores segments of the MMU. */
  private final Map<String, MmuSegment> segments;

  /** Stores regions of the MMU. */
  private Map<String, RegionSettings> regions;

  /** Stores buffers of the MMU. */
  private final Map<String, MmuBuffer> buffers;

  // TODO:
  private final List<MmuBuffer> sortedBuffers;

  /** Refers to the target buffer of the MMU.*/
  private final MmuBuffer targetBuffer;

  /** Maps actions to out-going transitions. */
  private final Map<MmuAction, List<MmuTransition>> actions;

  /** Refers to the initial (root) action of the memory management unit. */
  private final MmuAction startAction;

  /**
   * Constructs an instance of {@code MmuSubsystem}.
   */
  private MmuSubsystem(
      final String name,
      final Map<String, NodeVariable> variables,
      final NodeVariable dataVariable,
      final Map<String, MmuAddressInstance> addresses,
      final List<MmuAddressInstance> sortedAddresses,
      final MmuAddressInstance virtualAddress,
      final MmuAddressInstance physicalAddress,
      final Map<String, MmuOperation> operations,
      final Map<String, MmuSegment> segments,
      final Map<String, MmuBuffer> buffers,
      final List<MmuBuffer> sortedBuffers,
      final MmuBuffer targetBuffer,
      final Map<MmuAction, List<MmuTransition>> actions,
      final MmuAction startAction) {
    InvariantChecks.checkNotNull(name);
    InvariantChecks.checkNotNull(variables);
    InvariantChecks.checkNotNull(dataVariable);
    InvariantChecks.checkNotNull(addresses);
    InvariantChecks.checkNotNull(sortedAddresses);
    // InvariantChecks.checkNotNull(virtualAddress);
    // InvariantChecks.checkNotNull(physicalAddress);

    InvariantChecks.checkNotNull(operations);
    InvariantChecks.checkNotNull(segments);
    InvariantChecks.checkNotNull(buffers);
    InvariantChecks.checkNotNull(sortedBuffers);
    // InvariantChecks.checkNotNull(targetBuffer);

    InvariantChecks.checkNotNull(actions);
    InvariantChecks.checkNotNull(startAction);

    this.name = name;
    this.variables = Collections.unmodifiableMap(variables);
    this.dataVariable = dataVariable;
    this.addresses = Collections.unmodifiableMap(addresses);
    this.sortedAddresses = Collections.unmodifiableList(sortedAddresses);
    this.virtualAddress = virtualAddress;
    this.physicalAddress = physicalAddress;

    this.operations = Collections.unmodifiableMap(operations);
    this.segments = Collections.unmodifiableMap(segments);
    this.regions = Collections.emptyMap();
    this.buffers = Collections.unmodifiableMap(buffers);
    this.sortedBuffers = Collections.unmodifiableList(sortedBuffers);
    this.targetBuffer = targetBuffer;

    this.actions = Collections.unmodifiableMap(actions);
    this.startAction = startAction;
  }

  public final String getName() {
    return name;
  }

  public void setSettings(final GeneratorSettings settings) {
    InvariantChecks.checkNotNull(settings);

    regions = new LinkedHashMap<>();
    for (final RegionSettings region : settings.getMemory().getRegions()) {
      if (region.isEnabled() && region.getType() == RegionSettings.Type.DATA) {
        regions.put(region.getName(), region);
      }
    }
  }

  public NodeVariable getVariable(final String name) {
    return variables.get(name);
  }

  public NodeVariable getDataVariable() {
    return dataVariable;
  }

  /**
   * Returns the collection of addresses registered in the MMU.
   * 
   * @return the collection of addresses.
   */
  public Collection<MmuAddressInstance> getAddresses() {
    return addresses.values();
  }

  // TODO:
  public List<MmuAddressInstance> getSortedListOfAddresses() {
    return sortedAddresses;
  }

  /**
   * Returns an address registered in the MMU by its name.
   * 
   * @param name the name of the address.
   * @return address or {@code null} it is undefined.
   */
  public MmuAddressInstance getAddress(final String name) {
    return addresses.get(name);
  }

  public MmuAddressInstance getVirtualAddress() {
    return virtualAddress;
  }

  public MmuAddressInstance getPhysicalAddress() {
    return physicalAddress;
  }

  /**
   * Returns the target buffer (the main memory device).
   *
   * @return the target buffer.
   */
  public MmuBuffer getTargetBuffer() {
    return targetBuffer;
  }

  /**
   * Returns the collection of operations registered in the MMU.
   *
   * @return the collection of operations.
   */
  public Collection<MmuOperation> getOperations() {
    return operations.values();
  }

  /**
   * Returns an operation registered in the MMU by its name.
   *
   * @param name the name of the operation.
   * @return operation or {@code null} if it is undefined.
   */
  public MmuOperation getOperation(final String name) {
    return operations.get(name);
  }

  /**
   * Returns the collection of segments registered in the MMU.
   *
   * @return the collection of segments.
   */
  public Collection<MmuSegment> getSegments() {
    return segments.values();
  }

  /**
   * Returns a segment registered in the MMU by its name.
   *
   * @param name the name of the segment.
   * @return segment or {@code null} if it is undefined.
   */
  public MmuSegment getSegment(final String name) {
    return segments.get(name);
  }

  public Collection<RegionSettings> getRegions() {
    return regions.values();
  }

  public RegionSettings getRegion(final String name) {
    return regions.get(name);
  }

  /**
   * Returns the collection of buffers registered in the MMU.
   *
   * @return the collection of buffers.
   */
  public Collection<MmuBuffer> getBuffers() {
    return buffers.values();
  }

  // TODO:
  public List<MmuBuffer> getSortedListOfBuffers() {
    return sortedBuffers;
  }

  /**
   * Returns a buffer registered in the MMU by its name.
   *
   * @param name the name of the buffer.
   * @return buffer or {@code null} if it is undefined.
   */
  public MmuBuffer getBuffer(final String name) {
    return buffers.get(name);
  }

  /**
   * Returns the set of actions registered in the memory management unit.
   *
   * @return the set of actions.
   */
  public Set<MmuAction> getActions() {
    return actions.keySet();
  }

  /**
   * Returns the list of transitions for the given action of the memory management unit.
   *
   * @param action the action.
   * @return the list of transitions.
   * @throws IllegalArgumentException if {@code action} is null.
   */
  public List<MmuTransition> getTransitions(final MmuAction action) {
    InvariantChecks.checkNotNull(action);

    return actions.get(action);
  }

  /**
   * Returns the list of all transitions.
   *
   * @return List of transitions.
   */
  public List<MmuTransition> getTransitions() {
    final List<MmuTransition> transitions = new ArrayList<>();

    for (final List<MmuTransition> actionTransitions : actions.values()) {
      transitions.addAll(actionTransitions);
    }

    return transitions;
  }

  /**
   * Returns the initial (root) action of the memory management unit.
   *
   * @return the initial action.
   */
  public MmuAction getStartAction() {
    return startAction;
  }

  @Override
  public String toString() {
    final String newline = System.getProperty("line.separator");

    final StringBuilder builder = new StringBuilder("MMU:");

    builder.append(String.format("%nAddresses: %d", addresses.size()));
    for (final MmuAddressInstance address : getAddresses()) {
      builder.append(newline);
      builder.append(address);
    }

    builder.append(String.format("%nStart address: %s%n", virtualAddress));

    builder.append(String.format("%nDevices: %d", buffers.size()));
    for (final MmuBuffer buffer : getBuffers()) {
      builder.append(newline);
      builder.append("    ");
      builder.append(buffer);
    }
    builder.append(newline);

    builder.append(newline);
    builder.append("Actions: ");
    builder.append(actions.size());
    builder.append(newline);
    builder.append("Start action: ");
    builder.append(startAction);
    builder.append(newline);

    for (final Map.Entry<MmuAction, List<MmuTransition>> action : actions.entrySet()) {
      builder.append(action.getKey());
      builder.append(": ");
      for (final MmuTransition transition : action.getValue()) {
        builder.append(newline);
        builder.append("    ");
        builder.append(transition);
      }
      builder.append(newline);
    }
    return builder.toString();
  }

  /**
   * The {@link Holder} interface must be implemented by classes generated
   * by the MMU translator which hold MMU specifications. This is needed
   * to deal with such classes in a uniform way.
   * 
   * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
   */

  public static interface Holder {
    MmuSubsystem getSpecification();
  }

  public static final class Builder {
    /** Name of the MMU */
    private String name = null;

    /** Stores available address types. */
    private Map<String, NodeVariable> variables = new LinkedHashMap<>();

    private NodeVariable dataVariable = null;

    /**
     * Stores available address types.
     * <p>Typically, includes two types: Virtual Address and Physical Address.</p>
     */
    private final Map<String, MmuAddressInstance> addresses = new LinkedHashMap<>();

    // TODO:
    private final List<MmuAddressInstance> sortedAddresses = new ArrayList<>();

    /** Refers to the virtual address type of the MMU. */
    private MmuAddressInstance virtualAddress;

    /** Refers to the physical address type of the MMU. */
    private MmuAddressInstance physicalAddress;

    /** Stores operations of MMU (linked to with operations in SA specs) */
    private final Map<String, MmuOperation> operations = new LinkedHashMap<>();

    /** Stores segments of the MMU. */
    private final Map<String, MmuSegment> segments = new LinkedHashMap<>();

    /** Stores buffers of the MMU. */
    private final Map<String, MmuBuffer> buffers = new LinkedHashMap<>();

    // TODO:
    private final List<MmuBuffer> sortedBuffers = new ArrayList<>();

    /** Refers to the target buffer of the MMU. */
    private MmuBuffer targetBuffer;

    /** Maps actions to out-going transitions. */
    private Map<MmuAction, List<MmuTransition>> actions = new LinkedHashMap<>();

    /** Refers to the initial (root) action of the memory management unit. */
    private MmuAction startAction;

    public MmuSubsystem build() {
      return new MmuSubsystem(
          name,
          variables,
          dataVariable,
          addresses,
          sortedAddresses,
          virtualAddress,
          physicalAddress,
          operations,
          segments,
          buffers,
          sortedBuffers,
          targetBuffer,
          actions,
          startAction
          );
    }

    public void setName(final String name) {
      InvariantChecks.checkNotNull(name);
      this.name = name;
    }

    public void registerVariable(final NodeVariable variable) {
      InvariantChecks.checkNotNull(variable);
      variables.put(variable.getVariable().getName(), variable);
    }

    public void registerVariable(final MmuStruct struct) {
      InvariantChecks.checkNotNull(struct);
      for (final NodeVariable variable : struct.getFields()) {
        registerVariable(variable);
      }
    }

    public void setDataVariable(final NodeVariable variable) {
      InvariantChecks.checkNotNull(variable);
      dataVariable = variable;
    }

    /**
     * Registers the address type in the MMU. Address that have the same 
     * name are considered as duplicates and ignored.
     *
     * @param address the address to be registered.
     * @throws IllegalArgumentException if {@code address} is {@code null}.
     */
    public void registerAddress(final MmuAddressInstance address) {
      InvariantChecks.checkNotNull(address);
      registerVariable(address);

      addresses.put(address.getName(), address);
      sortedAddresses.add(address);
    }

    public MmuAddressInstance getAddress(final String name) {
      return addresses.get(name);
    }

    public void setVirtualAddress(final MmuAddressInstance address) {
      InvariantChecks.checkNotNull(address);
      virtualAddress = address;
    }

    public void setPhysicalAddress(final MmuAddressInstance address) {
      InvariantChecks.checkNotNull(address);
      physicalAddress = address;
    }

    /**
     * Sets the target buffer (the main memory device).
     *
     * @param buffer the buffer to be set.
     * @throws IllegalArgumentException if {@code buffer} is {@code null}.
     */
    public void setTargetBuffer(final MmuBuffer buffer) {
      InvariantChecks.checkNotNull(buffer);
      targetBuffer = buffer;
    }

    /**
     * Registers an operation in the MMU.
     *
     * @param operation the operation to be registered.
     * @throws IllegalArgumentException if {@code operation} is {@code null}.
     */
    public void registerOperation(final MmuOperation operation) {
      InvariantChecks.checkNotNull(operation);
      operations.put(operation.getName(), operation);
    }

    /**
     * Registers a segment in the MMU.
     *
     * @param segment the segment to be registered.
     * @throws IllegalArgumentException if {@code segment} is {@code null}.
     */
    public void registerSegment(final MmuSegment segment) {
      InvariantChecks.checkNotNull(segment);
      segments.put(segment.getName(), segment);
    }

    /**
     * Registers a buffer in the MMU.
     *
     * <p>Buffers are identified by their name. Buffers with equal names are considered duplicates
     * and ignored.</p>
     *
     * @param buffer the buffer to be registered.
     * @throws IllegalArgumentException if {@code buffer} is {@code null}.
     */
    public void registerBuffer(final MmuBuffer buffer) {
      InvariantChecks.checkNotNull(buffer);
      registerVariable(buffer);

      buffers.put(buffer.getName(), buffer);
      sortedBuffers.add(buffer);
    }

    public MmuBuffer getBuffer(final String name) {
      return buffers.get(name);
    }

    /**
     * Registers the action in the memory management unit.
     *
     * <p>Actions should be registered before transitions.</p>
     *
     * @param action the action to be registered.
     * @throws IllegalArgumentException if {@code action} is null.
     */
    public void registerAction(final MmuAction action) {
      InvariantChecks.checkNotNull(action);

      actions.put(action, new ArrayList<MmuTransition>());
    }

    /**
     * Registers the transition in the memory management unit.
     *
     * <p>Transitions should be registered after actions.</p>
     *
     * @param transition the transition to be registered.
     * @throws IllegalArgumentException if {@code transition} is null.
     */
    public void registerTransition(final MmuTransition transition) {
      InvariantChecks.checkNotNull(transition);

      final List<MmuTransition> transitions = actions.get(transition.getSource());
      transitions.add(transition);
    }

    /**
     * Sets the initial (root) action of the memory management unit.
     *
     * @param action the initial action.
     * @throws IllegalArgumentException if {@code action} is null.
     */
    public void setStartAction(final MmuAction action) {
      InvariantChecks.checkNotNull(action);

      startAction = action;
    }
  }
  
}
