/*
 * Copyright 2014-2016 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.test;

import java.math.BigInteger;
import java.util.List;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.model.api.ConfigurationException;
import ru.ispras.microtesk.model.api.ProcessingElement;
import ru.ispras.microtesk.model.api.memory.LocationAccessor;
import ru.ispras.microtesk.model.api.tarmac.Tarmac;
import ru.ispras.microtesk.model.api.tarmac.Record;
import ru.ispras.microtesk.options.Option;
import ru.ispras.microtesk.test.engine.Code;
import ru.ispras.microtesk.test.sequence.engine.EngineContext;
import ru.ispras.microtesk.test.sequence.engine.utils.EngineUtils;
import ru.ispras.microtesk.test.template.ConcreteCall;
import ru.ispras.microtesk.test.template.Label;
import ru.ispras.microtesk.test.template.LabelReference;
import ru.ispras.microtesk.test.template.Output;

/**
 * The role of the {@link Executor} class is to execute (simulate) sequences of instruction calls
 * (concrete calls). It executes instruction by instruction, perform control transfers by labels
 * (if needed) and prints information about important events to the simulator log (currently,
 * the console).
 * 
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class Executor {
  /**
   * The {@link Listener} interface is to be implemented by classes that monitor
   * execution of instruction calls.
   * 
   * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
   */
  public interface Listener {
    void onBeforeExecute(EngineContext context, ConcreteCall call);
    void onAfterExecute(EngineContext context, ConcreteCall call);
  }

  public interface ICode {
    boolean hasAddress(long address);
    boolean hasHandler(String id);
    long getHandlerAddress(String id);
    void addHandlerAddress(String id, long address);
    void addTestSequence(TestSequence sequence);
  }

  private static final class LabelTracker {
    private final int delaySlotSize;
    private List<LabelReference> labels;
    private int distance;

    public LabelTracker(final int delaySlotSize) {
      InvariantChecks.checkGreaterOrEqZero(delaySlotSize);
      this.delaySlotSize = delaySlotSize;
      reset();
    }

    public void track(final ConcreteCall call) {
      if (!call.isExecutable()) {
        return;
      }

      if (labels != null) {
        if (distance > delaySlotSize) {
          reset();
        } else {
          distance++;
        }
      }

      if (!call.getLabelReferences().isEmpty()) {
        labels = call.getLabelReferences();
        distance = 0;
      }
    }

    public void reset() {
      this.labels = null;
      this.distance = 0;
    }

    public LabelReference getLabel() {
      return null != labels ? labels.get(0) : null;
    }
  }

  private final EngineContext context;
  private Listener listener;

  private final ConcreteCall exceptionCall;
  private final ConcreteCall invalidCall;
  private final boolean isInvalidCallHandled;
  private final int branchExecutionLimit;
  private final boolean isLoggingEnabled;
  private final String originFormat;
  private final String alignFormat;

  /**
   * Constructs an Executor object.
   * 
   * @param context Generation engine context.
   * 
   * @throws IllegalArgumentException if the argument is {@code null}.
   */
  public Executor(final EngineContext context) {
    InvariantChecks.checkNotNull(context);

    this.context = context;
    this.listener = null;

    this.exceptionCall = EngineUtils.makeSpecialConcreteCall(context, "exception");
    this.invalidCall = EngineUtils.makeSpecialConcreteCall(context, "invalid_instruction");
    this.isInvalidCallHandled = null != invalidCall;
    this.branchExecutionLimit = context.getOptions().getValueAsInteger(Option.BRANCH_LIMIT);
    this.isLoggingEnabled = context.getOptions().getValueAsBoolean(Option.VERBOSE);
    this.originFormat = context.getOptions().getValueAsString(Option.ORIGIN_FORMAT);
    this.alignFormat = context.getOptions().getValueAsString(Option.ALIGN_FORMAT);
  }

  public void setListener(final Listener listener) {
    this.listener = listener;
  }

  private ProcessingElement getStateObserver() {
    return context.getModel().getPE();
  }

  private LocationAccessor getPCLocation() throws ConfigurationException {
    return getStateObserver().accessLocation("PC");
  }

  private long getPC() throws ConfigurationException {
    return getPCLocation().getValue().longValue();
  }

  private void setPC(final long address) throws ConfigurationException {
    getPCLocation().setValue(BigInteger.valueOf(address));
  }

  /**
   * Executes the specified sequence of instruction calls (concrete calls) and prints information
   * about important events to the simulator log.
   * 
   * @throws IllegalArgumentException if the parameter is {@code null}.
   * @throws GenerationAbortedException if during the interaction with the microprocessor model
   *         an error caused by an invalid format of the request has occurred (typically, it
   *         happens when evaluating an {@link Output} object causes an invalid request to the
   *         model state observer).
   */
  public void execute(
      final ExecutorCode executorCode,
      final long startAddress,
      final long endAddress,
      final int startIndex,
      final int endIndex) {
    InvariantChecks.checkNotNull(executorCode);
    InvariantChecks.checkTrue(startIndex <= endIndex);
    InvariantChecks.checkTrue(executorCode.isInBounds(startIndex));
    InvariantChecks.checkTrue(executorCode.isInBounds(endIndex));

    if (context.getOptions().getValueAsBoolean(Option.NO_SIMULATION)) {
      Logger.debug("Simulation is disabled");
      return;
    }

    context.getStatistics().pushActivity(Statistics.Activity.SIMULATING);

    try {
      for (int index = 0; index < context.getModel().getPENumber(); index++) {
        Logger.debugHeader("Instance %d", index);
        context.getModel().setActivePE(index);
        executeCalls(executorCode, startAddress, endAddress, startIndex, endIndex);
      }
    } catch (final ConfigurationException e) {
      throw new GenerationAbortedException("Simulation failed", e);
    } finally {
      context.getStatistics().popActivity();
    }
  }

  private void executeCalls(
      final ExecutorCode code,
      final long startAddress,
      final long endAddress,
      final int startIndex,
      final int endIndex) throws ConfigurationException {
    final LabelTracker labelTracker = new LabelTracker(context.getDelaySlotSize());

    int index = startIndex;
    boolean isInvalidNeverCalled = true;

    while (code.isInBounds(index) || (isInvalidCallHandled && isInvalidNeverCalled)) {
      final ConcreteCall call = code.isInBounds(index) ? code.getCall(index) : invalidCall;
      isInvalidNeverCalled = isInvalidNeverCalled && (call != invalidCall);

      if (call != invalidCall) {
        setPC(call.getAddress());
      }

      logCall(call);
      labelTracker.track(call);

      // NON-EXECUTABLE
      if (!call.isExecutable()) {
        if (index == endIndex) break;
        index++;
        continue;
      }

      final String exception = executeCall(call);

      // EXCEPTION
      if (null != exception) {
        final Long handlerAddress = getExceptionHandlerAddress(code, exception);
        if (null != handlerAddress) {
          labelTracker.reset(); // Resets labels to jump (no longer needed after jump to handler).
          logJump(handlerAddress, null);
          index = getCallIndex(code, handlerAddress);
        } else {
          Logger.error("Exception handler for %s is not found.", exception);
          if (call == invalidCall) {
            Logger.message("Execution will be terminated.");
          } else {
            Logger.message("Execution will be continued from the next instruction.");
            if (index == endIndex) break;
            index = getNextCallIndex(code, index);
          }
        }
        continue;
      }

      final long address = getPC();
      final boolean isJump = address != call.getAddress() + call.getByteSize();

      // NORMAL
      if (!isJump) {
        if (index == endIndex) break;
        index = getNextCallIndex(code, index); 
        continue;
      }

      // JUMP
      final LabelReference reference = labelTracker.getLabel();
      labelTracker.reset(); // Resets labels to jump (no longer needed after being used).

      if (null != reference) {
        final long labelAddress = getLabelAddress(code, call, reference);
        logJump(labelAddress, reference.getTarget().getLabel());
        index = getCallIndex(code, labelAddress);
      } else {
        // If no label references are found within the delay slot we try to use PC to jump
        logJump(address, null);
        index = getCallIndex(code, address);
      }
    }
  }

  private int getNextCallIndex(final ExecutorCode code, final int currentIndex) {
    InvariantChecks.checkNotNull(code);
    InvariantChecks.checkGreaterOrEqZero(currentIndex);

    final ConcreteCall currentCall = code.getCall(currentIndex);
    final long nextAddress = currentCall.getAddress() + currentCall.getByteSize();

    final int nextIndex = currentIndex + 1;
    final ConcreteCall nextCall = code.getCall(nextIndex);

    if (nextCall.getAddress() == nextAddress) {
      return nextIndex;
    }

    // If the next call is aligned, it is not a problem.
    if (null != nextCall.getAlignment() && null == nextCall.getOrigin()) {
      return nextIndex;
    }

    if (!isInvalidCallHandled) {
      throw new GenerationAbortedException(String.format(
          "Simulation error. There is no executable code at 0x%x", nextAddress));
    }

    return -1;
  }

  private int getCallIndex(final ExecutorCode code, final long address) {
    InvariantChecks.checkNotNull(code);

    if (code.hasAddress(address)) {
      return code.getCallIndex(address);
    }

    if (!isInvalidCallHandled) {
      throw new GenerationAbortedException(
          String.format("Simulation error. No executable code at 0x%016x", address));
    }

    return -1;
  }

  private long getLabelAddress(
      final ICode code,
      final ConcreteCall call,
      final LabelReference reference) {
    InvariantChecks.checkNotNull(code);
    InvariantChecks.checkNotNull(call);
    InvariantChecks.checkNotNull(reference);

    final LabelManager.Target target = reference.getTarget();
    if (null != target && code.hasAddress(target.getAddress())) {
      return target.getAddress();
    }

    throw new GenerationAbortedException(String.format(
          "Label '%s' passed to '%s' (0x%016x) is not defined or%n" +
          "is not accessible in the scope of the current test sequence.",
          reference.getReference().getName(), call.getText(), call.getAddress()));
  }

  private Long getExceptionHandlerAddress(
      final ICode code,
      final String exception) throws ConfigurationException {
    InvariantChecks.checkNotNull(code);
    InvariantChecks.checkNotNull(exception);

    if (null != exceptionCall) {
      // op exception is defined and must do all dispatching job
      exceptionCall.execute(context.getModel().getPE());
      return getPC();
    }

    if (code.hasHandler(exception)) {
      return code.getHandlerAddress(exception);
    }

    return null;
  }

  private String executeCall(final ConcreteCall call) {
    if (null != listener) {
      listener.onBeforeExecute(context, call);
    }

    if (invalidCall != call) {
      context.getStatistics().incTraceLength();
      if (Tarmac.isEnabled()) {
        Tarmac.addRecord(Record.newInstruction(call));
      }
    }

    Tarmac.setEnabled(true);
    final String exception = call.execute(context.getModel().getPE());
    Tarmac.setEnabled(false);

    if (null != listener) {
      listener.onAfterExecute(context, call);
    }

    if (invalidCall != call) {
      checkExecutionCount(call);
    }

    return exception;
  }

  private void checkExecutionCount(final ConcreteCall call) {
    if (branchExecutionLimit > 0 && call.getExecutionCount() >= branchExecutionLimit) {
      throw new GenerationAbortedException(String.format(
          "Instruction %s reached its limit on execution count (%d). " +
          "Probably, the program entered an endless loop. Generation was aborted.",
          call.getText(), branchExecutionLimit
          ));
    }
  }

  private void logCall(final ConcreteCall call) throws ConfigurationException {
    if(!isLoggingEnabled) {
      return;
    }

    if (null != call.getOrigin()) {
      Logger.debug(originFormat, call.getOrigin());
    }

    if (null != call.getAlignment()) {
      Logger.debug(alignFormat, call.getAlignment());
    }

    for (final Output output : call.getOutputs()) {
      if (output.isRuntime()) {
        Logger.debug(output.evaluate(getStateObserver()));
      }
    }

    for (final Label label : call.getLabels()) {
      Logger.debug(label.getUniqueName() + ":");
    }

    if (invalidCall != call && null != call.getText()) {
      Logger.debug("0x%016x %s", call.getAddress(), call.getText());
    }
  }

  private void logJump(final long address, final Label label) {
    if(!isLoggingEnabled) {
      return;
    }

    final String addressText = String.format("0x%016x", address);
    final StringBuilder sb = new StringBuilder("Jump to ");

    if (null != label) {
      sb.append(label.getUniqueName());
      sb.append(" at ");
    }

    sb.append(addressText);
    Logger.debug(sb.toString());
  }

  public void execute(
      final Code executorCode,
      final long startAddress,
      final long endAddress) {
    InvariantChecks.checkNotNull(executorCode);

    if (context.getOptions().getValueAsBoolean(Option.NO_SIMULATION)) {
      Logger.debug("Simulation is disabled");
      return;
    }

    context.getStatistics().pushActivity(Statistics.Activity.SIMULATING);

    try {
      for (int index = 0; index < context.getModel().getPENumber(); index++) {
        Logger.debugHeader("Instance %d", index);
        context.getModel().setActivePE(index);
        executeCalls(executorCode, startAddress, endAddress);
      }
    } catch (final ConfigurationException e) {
      throw new GenerationAbortedException("Simulation failed", e);
    } finally {
      context.getStatistics().popActivity();
    }
  }

  private final class Fetcher {
    private final Code code;
    private long address;
    private Code.Iterator iterator;

    private Fetcher(final Code code, final long address) {
      InvariantChecks.checkNotNull(code);
      this.code = code;

      this.address = address;
      this.iterator = code.getIterator(address);
    }

    public ConcreteCall getCall() {
      return null != iterator ? iterator.current() : invalidCall;
    }

    public boolean isAddressReached(final long targetAddress) {
      return targetAddress == address;
    }

    public void next() {
      InvariantChecks.checkNotNull(iterator);

      final ConcreteCall current = iterator.current();
      InvariantChecks.checkNotNull(current); 

      iterator.next();
      final ConcreteCall next = iterator.current();

      address = null != next ? next.getAddress() : address + current.getByteSize();
    }

    public void jump(final long jumpAddress) {
      address = jumpAddress;
      iterator = code.hasAddress(jumpAddress) ? code.getIterator(jumpAddress) : null;
    }
  }

  private void executeCalls(
      final Code code,
      final long startAddress,
      final long endAddress) throws ConfigurationException {
    final LabelTracker labelTracker = new LabelTracker(context.getDelaySlotSize());
    final Fetcher fetcher = new Fetcher(code, startAddress);

    while (fetcher.getCall() != null && !fetcher.isAddressReached(endAddress)) {
      final ConcreteCall call = fetcher.getCall();
      if (call != invalidCall) { 
        setPC(call.getAddress());
      }

      logCall(call);
      labelTracker.track(call);

      // NON-EXECUTABLE
      if (!call.isExecutable()) {
        fetcher.next();
        continue;
      }

      final String exception = executeCall(call);

      // EXCEPTION
      if (null != exception) {
        final Long handlerAddress = getExceptionHandlerAddress(code, exception);

        if (null != handlerAddress) {
          labelTracker.reset(); // Resets labels to jump (no longer needed after jump to handler).
          logJump(handlerAddress, null);
          fetcher.jump(handlerAddress);
        } else {
          Logger.error("Exception handler for %s is not found.", exception);
          Logger.message("Execution will be continued from the next instruction.");
          fetcher.next();
        }

        continue;
      }

      final long address = getPC();
      final boolean isJump = address != call.getAddress() + call.getByteSize();

      // NORMAL
      if (!isJump) {
        fetcher.next();
        continue;
      }

      // JUMP
      final LabelReference reference = labelTracker.getLabel();
      labelTracker.reset(); // Resets labels to jump (no longer needed after being used).

      if (null != reference) {
        final long labelAddress = getLabelAddress(code, call, reference);
        logJump(labelAddress, reference.getTarget().getLabel());
        fetcher.jump(labelAddress);
      } else {
        // If no label references are found within the delay slot we try to use PC to jump
        logJump(address, null);
        fetcher.jump(address);
      }
    }
  }
}
