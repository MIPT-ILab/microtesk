/*
 * Copyright 2013-2015 ISP RAS (http://www.ispras.ru)
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

import static ru.ispras.fortress.util.InvariantChecks.checkNotNull;
import static ru.ispras.microtesk.test.sequence.engine.utils.EngineUtils.checkRootOp;
import static ru.ispras.microtesk.test.sequence.engine.utils.EngineUtils.makeConcreteCall;
import static ru.ispras.microtesk.test.sequence.engine.utils.EngineUtils.makeInitializer;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.model.api.exception.ConfigurationException;
import ru.ispras.microtesk.test.LabelManager;
import ru.ispras.microtesk.test.SelfCheck;
import ru.ispras.microtesk.test.TestSequence;
import ru.ispras.microtesk.test.TestSettings;
import ru.ispras.microtesk.test.sequence.engine.utils.AddressingModeWrapper;
import ru.ispras.microtesk.test.sequence.engine.utils.EngineUtils;
import ru.ispras.microtesk.test.template.Argument;
import ru.ispras.microtesk.test.template.Call;
import ru.ispras.microtesk.test.template.ConcreteCall;
import ru.ispras.microtesk.test.template.Label;
import ru.ispras.microtesk.test.template.LabelReference;
import ru.ispras.microtesk.test.template.Primitive;
import ru.ispras.testbase.knowledge.iterator.SingleValueIterator;

/**
 * The job of the {@link DefaultEngine} class is to processes an abstract instruction call
 * sequence (uses symbolic values) and to build a concrete instruction call sequence (uses only
 * concrete values and can be simulated and used to generate source code in assembly language).
 * The {@link DefaultEngine} class performs all necessary data generation and all initializing
 * calls to the generated instruction sequence.
 * 
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */

public final class DefaultEngine implements Engine<TestSequence> {
  private Set<AddressingModeWrapper> initializedModes;
  private TestSequence.Builder sequenceBuilder;

  @Override
  public Class<TestSequence> getSolutionClass() {
    return TestSequence.class;
  }

  @Override
  public void configure(final Map<String, Object> attributes) {
    // Do nothing.
  }

  @Override
  public EngineResult<TestSequence> solve(
      final EngineContext engineContext,
      final List<Call> abstractSequence) {
    checkNotNull(engineContext);
    checkNotNull(abstractSequence);

    try {
      return new EngineResult<>(new SingleValueIterator<>(process(engineContext, abstractSequence)));
    } catch (final ConfigurationException e) {
      return new EngineResult<>(e.getMessage());
    }
  }

  private TestSequence process(
      final EngineContext engineContext,
      final List<Call> abstractSequence) throws ConfigurationException {
    checkNotNull(engineContext);
    checkNotNull(abstractSequence);

    initializedModes = new HashSet<>();
    sequenceBuilder = new TestSequence.Builder();

    // Get modes for output arguments for self-checks if the setting is enabled.
    final Set<AddressingModeWrapper> outModes = TestSettings.isSelfChecks() ?
        EngineUtils.getOutAddressingModes(abstractSequence):
        Collections.<AddressingModeWrapper>emptySet(); 

    try {
      for (final Call abstractCall : abstractSequence) {
        processAbstractCall(engineContext, abstractCall);
      }

      for (final AddressingModeWrapper mode : outModes) {
        sequenceBuilder.addCheck(new SelfCheck(mode));
      }

      final TestSequence sequence = sequenceBuilder.build();

      final long baseAddress = engineContext.getAddress();
      final long newAddress = sequence.setAddress(baseAddress);
      engineContext.setAddress(newAddress);

      return sequence;
    } finally {
      initializedModes = null;
      sequenceBuilder = null;
    }
  }

  private void registerCall(final ConcreteCall call, final LabelManager labelManager) {
    checkNotNull(call);

    patchLabels(call, labelManager);
    call.execute();

    sequenceBuilder.add(call);
  }

  private void registerPrologueCall(final ConcreteCall call, final LabelManager labelManager) {
    checkNotNull(call);

    patchLabels(call, labelManager);
    call.execute();

    sequenceBuilder.addToPrologue(call);
  }

  private static void patchLabels(final ConcreteCall call, final LabelManager labelManager) {
    for (final LabelReference labelRef : call.getLabelReferences()) {
      labelRef.resetTarget();

      final Label source = labelRef.getReference();
      final LabelManager.Target target = labelManager.resolve(source);

      if (null != target) {
        final long address = target.getAddress();
        labelRef.getPatcher().setValue(BigInteger.valueOf(address));
      }
    }
  }

  private void processAbstractCall(
      final EngineContext context,
      final Call abstractCall) throws ConfigurationException {
    checkNotNull(context);
    checkNotNull(abstractCall);

    // Only executable calls are worth printing.
    if (abstractCall.isExecutable()) {
      Logger.debug("%nProcessing %s...", abstractCall);

      final Primitive rootOp = abstractCall.getRootOperation();
      checkRootOp(rootOp);

      if (context.isGenerateData()) {
        processSituations(context, rootOp);
      }
    }

    final ConcreteCall concreteCall = makeConcreteCall(context, abstractCall);
    registerCall(concreteCall, context.getDataManager().getGlobalLabels());
  }

  private void processSituations(
      final EngineContext engineContext,
      final Primitive primitive) throws ConfigurationException {
    checkNotNull(engineContext);
    checkNotNull(primitive);

    for (Argument arg : primitive.getArguments().values()) {
      if (Argument.Kind.OP == arg.getKind()) {
        processSituations(engineContext, (Primitive) arg.getValue());
      }
    }

    final List<Call> initializingCalls = makeInitializer(
        engineContext, primitive, primitive.getSituation(), initializedModes);
    addCallsToPrologue(engineContext, initializingCalls);
  }

  /* TODO:
  private void generateData(
      final EngineContext engineContext,
      final Primitive primitive) throws ConfigurationException {
    checkNotNull(engineContext);
    checkNotNull(primitive);

    final Situation situation = primitive.getSituation();

    final TestBaseQueryCreator queryCreator =
        new TestBaseQueryCreator(engineContext, situation, primitive);

    final TestData testData = getTestData(engineContext, primitive, queryCreator);
    Logger.debug(testData.toString());

    setUnknownImmValues(queryCreator.getUnknownImmValues(), testData);

    // Set model state using preparators that create initializing
    // sequences based on addressing modes.
    for (final Map.Entry<String, Node> e : testData.getBindings().entrySet()) {
      final String name = e.getKey();

      final Argument arg = queryCreator.getModes().get(name);

      if (null == arg) {
        continue;
      }

      // No point to assign output variables even if values for them are provided.
      // We do not want extra code and conflicts when same registers are used
      // as input and output (see Bug #6057)
      if (arg.getMode() == ArgumentMode.OUT) {
        continue;
      }

      final Primitive mode = (Primitive) arg.getValue();

      final AddressingModeWrapper targetMode = new AddressingModeWrapper(mode);
      if (initializedModes.contains(targetMode)) {
        Logger.debug("%s has already been used to set up the processor state. " +
              "No initialization code will be created.", targetMode);
        continue;
      }

      final BitVector value = FortressUtils.extractBitVector(e.getValue());

      Logger.debug("Creating code to assign %s to %s...", value, targetMode);
      final List<Call> initializingCalls = makeInitializer(engineContext, mode, value);

      addCallsToPrologue(engineContext, initializingCalls);
      initializedModes.add(targetMode);
    }
  }
*/

  private void addCallsToPrologue(
      final EngineContext context,
      final List<Call> abstractCalls) throws ConfigurationException {
    checkNotNull(context);
    checkNotNull(abstractCalls);

    for (final Call abstractCall : abstractCalls) {
      final ConcreteCall concreteCall = makeConcreteCall(context, abstractCall);
      registerPrologueCall(concreteCall, context.getDataManager().getGlobalLabels());
    }
  }
}
