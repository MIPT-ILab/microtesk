/*
 * Copyright 2016-2018 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.mmu.basis;

import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.mmu.model.spec.MmuTransition;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * {@link MemoryAccessStack} represents a memory access stack.
 *
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class MemoryAccessStack {
  public static final MemoryAccessStack EMPTY = new MemoryAccessStack();

  /**
   * {@link Frame} represents a memory access stack's frame.
   */
  public  static final class Frame {
    private final String id;
    private final MmuTransition transition;
    private final Map<NodeVariable, NodeVariable> frame = new HashMap<>();

    private Frame(final String id, final MmuTransition transition) {
      InvariantChecks.checkNotNull(id);

      this.id = id;
      this.transition = transition;
    }

    public String getId() {
      return id;
    }

    public MmuTransition getTransition() {
      return transition;
    }

    public NodeVariable getInstance(final NodeVariable variable) {
      InvariantChecks.checkNotNull(variable);

      // Constants are not duplicated in stack frames.
      if (variable.getVariable().hasValue()) {
        return variable;
      }

      NodeVariable frameVariable = frame.get(variable);

      if (frameVariable == null) {
        final String name = String.format("%s$%s", id, variable.getName());
        frame.put(variable, frameVariable = new NodeVariable(name, variable.getDataType()));
      }

      return frameVariable;
    }

    @Override
    public String toString() {
      return String.format("%s: %s", id, frame);
    }
  }

  private final String id;
  private final Stack<Frame> stack = new Stack<>();

  public MemoryAccessStack(final String id) {
    InvariantChecks.checkNotNull(id);
    this.id = id;
  }

  public MemoryAccessStack() {
    this("");
  }

  public MemoryAccessStack(final MemoryAccessStack r) {
    InvariantChecks.checkNotNull(r);
    this.id = r.id;
    this.stack.addAll(r.stack);
  }

  public String getId() {
    return id;
  }

  public boolean isEmpty() {
    return stack.isEmpty();
  }

  public int size() {
    return stack.size();
  }

  public Frame call(final String id, final MmuTransition transition) {
    InvariantChecks.checkNotNull(id);

    final Frame frame = new Frame(getFullId(id), transition);
    return call(frame);
  }

  public Frame call(final Frame frame) {
    InvariantChecks.checkNotNull(frame);
    InvariantChecks.checkFalse(stack.contains(frame));

    stack.push(frame);
    return frame;
  }

  public Frame ret() {
    InvariantChecks.checkNotEmpty(stack);
    return stack.pop();
  }

  public Frame getFrame() {
    InvariantChecks.checkNotNull(stack);
    return stack.peek();
  }

  public NodeVariable getInstance(final NodeVariable variable) {
    InvariantChecks.checkNotNull(variable);

    if (stack.isEmpty()) {
      return variable;
    }

    final Frame frame = stack.peek();
    return frame.getInstance(variable);
  }

  private String getFullId(final String localId) {
    final StringBuffer buffer = new StringBuffer(id);

    boolean delimiter = !id.isEmpty();

    for (final Frame frame : stack) {
      if (delimiter) {
        buffer.append(".");
      }

      buffer.append(frame.id);
      delimiter = true;
    }

    if (delimiter) {
      buffer.append(".");
    }

    buffer.append(localId);

    return buffer.toString();
  }

  @Override
  public String toString() {
    return stack.toString();
  }
}
