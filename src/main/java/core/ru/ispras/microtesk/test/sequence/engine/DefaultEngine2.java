/*
 * Copyright 2017 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.test.sequence.engine;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.model.api.ConfigurationException;
import ru.ispras.microtesk.model.api.InstructionCall;
import ru.ispras.microtesk.model.api.IsaPrimitive;
import ru.ispras.microtesk.options.Option;
import ru.ispras.microtesk.test.CodeAllocator;
import ru.ispras.microtesk.test.Executor;
import ru.ispras.microtesk.test.LabelManager;
import ru.ispras.microtesk.test.TestSequence;
import ru.ispras.microtesk.test.sequence.engine.utils.AddressingModeWrapper;
import ru.ispras.microtesk.test.sequence.engine.utils.EngineUtils;
import ru.ispras.microtesk.test.template.Argument;
import ru.ispras.microtesk.test.template.Call;
import ru.ispras.microtesk.test.template.ConcreteCall;
import ru.ispras.microtesk.test.template.DataSection;
import ru.ispras.microtesk.test.template.Primitive;
import ru.ispras.testbase.knowledge.iterator.SingleValueIterator;

/**
 * The job of the {@link DefaultEngine2} class is to processes an abstract instruction call
 * sequence (uses symbolic values) and to build a concrete instruction call sequence (uses only
 * concrete values and can be simulated and used to generate source code in assembly language).
 * The {@link DefaultEngine2} class performs all necessary data generation and all initializing
 * calls to the generated instruction sequence.
 * 
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class DefaultEngine2 implements Engine<TestSequence> {
  @Override
  public Class<TestSequence> getSolutionClass() {
    return TestSequence.class;
  }

  @Override
  public void configure(final Map<String, Object> attributes) {
    // Do nothing.
  }

  @Override
  public void onStartProgram() {
    // Empty
  }

  @Override
  public void onEndProgram() {
    // Empty
  }

  @Override
  public EngineResult<TestSequence> solve(
      final EngineContext engineContext,
      final List<Call> abstractSequence) {
    InvariantChecks.checkNotNull(engineContext);
    InvariantChecks.checkNotNull(abstractSequence);

    try {
      final TestSequence testSequence = processSequence(engineContext, abstractSequence);
      return new EngineResult<>(new SingleValueIterator<>(testSequence));
    } catch (final ConfigurationException e) {
      return new EngineResult<>(e.getMessage());
    }
  }

  private static TestSequence processSequence(
      final EngineContext engineContext,
      final List<Call> abstractSequence) throws ConfigurationException {
    InvariantChecks.checkNotNull(engineContext);
    InvariantChecks.checkNotNull(abstractSequence);

    final int sequenceIndex =
        engineContext.getStatistics().getSequences();

    final List<ConcreteCall> sequence =
        EngineUtils.makeConcreteCalls(engineContext, abstractSequence);

    final long baseAddress =
        engineContext.getOptions().getValueAsBigInteger(Option.BASE_VA).longValue();

    final TestSequenceCreator creator = new TestSequenceCreator(abstractSequence, sequence);
    execute(engineContext, creator, baseAddress, sequence, sequenceIndex);

    return creator.createTestSequence();
  }

  private static void execute(
      final EngineContext engineContext,
      final ExecutorListener listener,
      final long allocationAddress,
      final List<ConcreteCall> sequence,
      final int sequenceIndex) {
    InvariantChecks.checkNotNull(engineContext);
    InvariantChecks.checkNotNull(listener);
    InvariantChecks.checkNotNull(sequence);

    final LabelManager labelManager = new LabelManager(engineContext.getLabelManager());
    allocateData(engineContext, labelManager, sequence, sequenceIndex);

    final CodeAllocator codeAllocator = new CodeAllocator(labelManager, allocationAddress);
    codeAllocator.init();
    codeAllocator.allocateCalls(sequence, sequenceIndex);

    final Executor executor = new Executor(engineContext);
    executor.setListener(listener);

    final long startAddress = sequence.get(0).getAddress();
    executor.execute(codeAllocator.getCode(), startAddress);
  }

  private static void allocateData(
      final EngineContext engineContext,
      final LabelManager labelManager,
      final List<ConcreteCall> sequence,
      final int sequenceIndex) {
    InvariantChecks.checkNotNull(engineContext);
    InvariantChecks.checkNotNull(labelManager);
    InvariantChecks.checkNotNull(sequence);

    for (final ConcreteCall call : sequence) {
      if (call.getData() != null) {
        final DataSection data = call.getData();
        data.setSequenceIndex(sequenceIndex);
        data.allocate(engineContext.getModel().getMemoryAllocator());
        data.registerLabels(labelManager);
      }
    }
  }

  private static class ExecutorListener implements Executor.Listener {
    private ConcreteCall lastExecutedCall = null;

    @Override
    public void onBeforeExecute(final EngineContext context, final ConcreteCall concreteCall) {
      // Empty
    }

    @Override
    public void onAfterExecute(final EngineContext context, final ConcreteCall concreteCall) {
      lastExecutedCall = concreteCall;
    }

    public final ConcreteCall getLastExecutedCall() {
      return lastExecutedCall;
    }

    public final void resetLastExecutedCall() {
      lastExecutedCall = null;
    }
  }

  private static final class TestSequenceCreator extends ExecutorListener {
    private final Map<ConcreteCall, Call> callMap;
    private final Set<AddressingModeWrapper> initializedModes;
    private final TestSequence.Builder testSequenceBuilder;

    private TestSequenceCreator(
        final List<Call> abstractSequence,
        final List<ConcreteCall> concreteSequence) {
      InvariantChecks.checkNotNull(abstractSequence);
      InvariantChecks.checkNotNull(concreteSequence);
      InvariantChecks.checkTrue(abstractSequence.size() == concreteSequence.size());

      this.callMap = new IdentityHashMap<>();
      this.initializedModes = new HashSet<>();

      for (int index = 0; index < abstractSequence.size(); ++index) {
        final Call abstractCall = abstractSequence.get(index);
        final ConcreteCall concreteCall = concreteSequence.get(index);

        InvariantChecks.checkNotNull(abstractCall);
        InvariantChecks.checkNotNull(concreteCall);

        callMap.put(concreteCall, abstractCall);
      }

      this.testSequenceBuilder = new TestSequence.Builder();
      this.testSequenceBuilder.add(concreteSequence);
    }

    public TestSequence createTestSequence() {
      return testSequenceBuilder.build();
    }

    @Override
    public void onBeforeExecute(
        final EngineContext engineContext,
        final ConcreteCall concreteCall) {
      InvariantChecks.checkNotNull(concreteCall);
      InvariantChecks.checkNotNull(engineContext);

      final Call abstractCall = callMap.get(concreteCall);
      if (null == abstractCall) {
        return; // Already processed
      }

      callMap.put(concreteCall, null);
      try {
        processCall(engineContext, abstractCall, concreteCall);
      } catch (final ConfigurationException e) {
        e.printStackTrace();
      }
    }

    private void processCall(
        final EngineContext engineContext,
        final Call abstractCall,
        final ConcreteCall concreteCall) throws ConfigurationException {
      InvariantChecks.checkNotNull(engineContext);
      InvariantChecks.checkNotNull(abstractCall);
      InvariantChecks.checkNotNull(concreteCall);

      // Not executable calls do not need test data
      if (!abstractCall.isExecutable()) {
        return;
      }

      Logger.debug("Generating test data...");

      final Primitive abstractPrimitive = abstractCall.getRootOperation();
      EngineUtils.checkRootOp(abstractPrimitive);

      final InstructionCall instructionCall = concreteCall.getExecutable();
      InvariantChecks.checkNotNull(instructionCall);

      final IsaPrimitive concretePrimitive = instructionCall.getRootPrimitive();
      InvariantChecks.checkNotNull(concretePrimitive);

      processPrimitive(engineContext, abstractPrimitive, concretePrimitive);
    }

    private void processPrimitive(
        final EngineContext engineContext,
        final Primitive abstractPrimitive,
        final IsaPrimitive concretePrimitive) throws ConfigurationException {
      InvariantChecks.checkNotNull(engineContext);
      InvariantChecks.checkNotNull(abstractPrimitive);
      InvariantChecks.checkNotNull(concretePrimitive);

      for (final Argument argument : abstractPrimitive.getArguments().values()) {
        if (Argument.Kind.OP == argument.getKind()) {
          final String argumentName = argument.getName();

          final Primitive abstractArgument = (Primitive) argument.getValue();
          final IsaPrimitive concreteArgument = concretePrimitive.getArguments().get(argumentName);

          processPrimitive(engineContext, abstractArgument, concreteArgument);
        }
      }

      final List<Call> initializer = EngineUtils.makeInitializer(
          engineContext,
          abstractPrimitive,
          abstractPrimitive.getSituation(),
          initializedModes
          );

      processInitializer(engineContext, initializer);
    }

    private void processInitializer(
        final EngineContext engineContext,
        final List<Call> initializingCalls) {
      // TODO Auto-generated method stub
    }
  }
}
