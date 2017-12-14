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

package ru.ispras.microtesk.test.engine.branch;

import java.util.LinkedHashSet;
import java.util.Set;

import ru.ispras.microtesk.utils.SharedObject;

/**
 * {@link BranchEntry} represents a node of the internal representation of a branch structure.
 *
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class BranchEntry extends SharedObject<BranchEntry> {
  public static enum Type {
    /** Conditional branch instruction. */
    IF_THEN,
    /** Unconditional branch instruction. */
    GOTO,
    /** Delay slot. */
    DELAY_SLOT,
    /** Basic block. */
    BASIC_BLOCK
  }

  /** Entry type. */
  private Type type;

  /** The register identifier (for branches only) */
  private int registerId;

  /** The flag that shows whether the register is used for the first time in the execution. */
  private boolean isRegisterFirstUse;

  /** Group identifier of the entry. */
  private int groupId;

  /** Branch label (index of the target instruction in the branch structure). */
  private int branchLabel;

  /** Execution trace of the branch execution. */
  private BranchTrace branchTrace;

  /**
   * Basic block coverage.
   *
   * <p>Block coverage is a set of basic blocks that cover all segments of the branch instruction,
   * where a segment is a sequence of blocks between two executions of the branch.</p>
   */
  private Set<Integer> blockCoverage;

  /**
   * Delay slot coverage.
   *
   * <p>Slot coverage is a set of slots that are included in all segments of the branch instruction,
   * where a segment is a sequence of slots between two executions of the branch.</p>
   */
  private Set<Integer> slotCoverage;

  private boolean controlCodeInBasicBlock = false;
  private boolean controlCodeInDelaySlot = false;

  public BranchEntry(
      final Type type, final int registerId, final int groupId, final int branchLabel) {
    this.type = type;
    this.registerId = registerId;
    this.groupId = groupId;
    this.branchLabel = branchLabel;
    this.branchTrace = new BranchTrace();
  }

  public BranchEntry(final Type type) {
    this(type, -1, -1, -1);
  }

  private BranchEntry(final BranchEntry other) {
    super(other);

    this.type = other.type;
    this.registerId = other.registerId;
    this.isRegisterFirstUse = other.isRegisterFirstUse;
    this.groupId = other.groupId;
    this.branchLabel = other.branchLabel;
    this.branchTrace = other.branchTrace.clone();

    this.blockCoverage =
        null != other.blockCoverage ? new LinkedHashSet<>(other.blockCoverage) : null;
    this.slotCoverage =
        null != other.slotCoverage ? new LinkedHashSet<>(other.slotCoverage) : null;

    this.controlCodeInBasicBlock = other.controlCodeInBasicBlock;
    this.controlCodeInDelaySlot = other.controlCodeInDelaySlot;
  }

  /**
   * Returns the type of the entry.
   *
   * @return the entry type.
   */
  public Type getType() {
    return type;
  }

  /**
   * Sets the type of the entry.
   *
   * @param type the type to be set.
   */
  public void setType(final Type type) {
    this.type = type;
  }

  /**
   * Checks whether the entry is a conditional branch.
   *
   * @return {@code true} if the entry is a conditional branch; {@code false} otherwise.
   */
  public boolean isIfThen() {
    return type == Type.IF_THEN;
  }

  /**
   * Checks whether the entry is an unconditional branch.
   *
   * @return {@code true} if the entry is a unconditional branch; {@code false} otherwise.
   */
  public boolean isGoto() {
    return type == Type.IF_THEN;
  }

  /**
   * Checks whether the entry is a delay slot.
   *
   * @return {@code true} if the entry is a delay slot; {@code false} otherwise.
   */
  public boolean isDelaySlot() {
    return type == Type.DELAY_SLOT;
  }

  /**
   * Checks if the entry is a basic block.
   *
   * @return {@code true} if the entry is a basic block; {@code false} otherwise.
   */
  public boolean isBasicBlock() {
    return type == Type.BASIC_BLOCK;
  }

  /**
   * Checks whether the entry is a conditional or an unconditional branch.
   *
   * @return {@code true} if the entry is a branch; {@code false} otherwise.
   */
  public boolean isBranch() {
    return type == Type.IF_THEN || type == Type.GOTO;
  }

  /**
   * Returns the register identifier of the branch entry.
   *
   * @return the register identifier.
   */
  public int getRegisterId() {
    return registerId;
  }

  /**
   * Sets the register identifier of the branch entry.
   *
   * @param registerId the register identifier to be set.
   */
  public void setRegisterId(final int registerId) {
    this.registerId = registerId;
  }

  /**
   * Checks whether the register is used for the first time.
   *
   * @return {@code} if the register is used for the first time; {@code false} otherwise.
   */
  public boolean isRegisterFirstUse() {
    return isRegisterFirstUse;
  }

  /**
   * Specifies whether the register is used for the first time.
   *
   * @param isRegisterFirstUse the value to be set.
   */
  public void setRegisterFirstUse(final boolean isRegisterFirstUse) {
    this.isRegisterFirstUse = isRegisterFirstUse;
  }

  /**
   * Returns the group identifier of the entry.
   *
   * @return the group identifier.
   */
  public int getGroupId() {
    return groupId;
  }

  /**
   * Sets the group identifier of the entry.
   *
   * @param groupId the group identifier to be set.
   */
  public void setGroupId(final int groupId) {
    this.groupId = groupId;
  }

  /**
   * Returns the branch label (index of the target instruction in the branch structure).
   *
   * @return the branch label.
   */
  public int getBranchLabel() {
    return branchLabel;
  }

  /**
   * Sets the branch label (index of the target instruction in the branch structure).
   *
   * @param branchLabel the branch label to be set.
   */
  public void setBranchLabel(final int branchLabel) {
    this.branchLabel = branchLabel;
  }

  /**
   * Returns the execution trace of the branch instruction.
   *
   * @return the execution trace.
   */
  public BranchTrace getBranchTrace() {
    return branchTrace;
  }

  /**
   * Returns the block coverage of the branch instruction.
   *
   * @return the block coverage.
   */
  public Set<Integer> getBlockCoverage() {
    return blockCoverage;
  }

  /**
   * Sets the block coverage of the branch instruction.
   *
   * @param blockCoverage the block coverage to be set.
   */
  public void setBlockCoverage(final Set<Integer> blockCoverage) {
    this.blockCoverage = blockCoverage;
  }

  /**
   * Returns the slot coverage of the branch instruction.
   *
   * @return the slot coverage.
   */
  public Set<Integer> getSlotCoverage() {
    return slotCoverage;
  }

  /**
   * Sets the slot coverage of the branch instruction.
   *
   * @param slotCoverage the slot coverage to be set.
   */
  public void setSlotCoverage(final Set<Integer> slotCoverage) {
    this.slotCoverage = slotCoverage;
  }

  public boolean isControlCodeInBasicBlock() {
    return controlCodeInBasicBlock;
  }

  public void setControlCodeInBasicBlock(final boolean controlCodeInBasicBlock) {
    this.controlCodeInBasicBlock = controlCodeInBasicBlock;
  }

  public boolean isControlCodeInDelaySlot() {
    return controlCodeInDelaySlot;
  }

  public void setControlCodeInDelaySlot(final boolean controlCodeInDelaySlot) {
    this.controlCodeInDelaySlot = controlCodeInDelaySlot;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder(type.name());

    if (isBranch()) {
      builder.append(String.format(": Target=%d, Trace=%s, BlockCoverage=%s, SlotCoverage=%s",
          branchLabel, branchTrace, blockCoverage, slotCoverage));
    }

    return builder.toString();
  }

  @Override
  public BranchEntry clone() {
    return newCopy();
  }

  @Override
  public BranchEntry newCopy() {
    return new BranchEntry(this);
  }
}
