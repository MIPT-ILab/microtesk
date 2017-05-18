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

package ru.ispras.microtesk.test.engine.branch;

import static ru.ispras.microtesk.test.engine.utils.EngineUtils.getSituationName;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.test.LabelManager;
import ru.ispras.microtesk.test.engine.Engine;
import ru.ispras.microtesk.test.engine.EngineContext;
import ru.ispras.microtesk.test.engine.EngineResult;
import ru.ispras.microtesk.test.template.AbstractCall;
import ru.ispras.microtesk.test.template.Label;
import ru.ispras.testbase.knowledge.iterator.Iterator;
import ru.ispras.testbase.knowledge.iterator.SingleValueIterator;

/**
 * {@link BranchEngine} implements a test engine that constructs test cases by enumerating
 * feasible execution traces of bounded length.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class BranchEngine implements Engine<BranchSolution> {
  /** Maximum number of executions of a single branch instruction. */
  public static final String PARAM_BRANCH_LIMIT = "branch_exec_limit";
  public static final int PARAM_BRANCH_LIMIT_DEFAULT = 1;

  /** Maximum number of execution traces to be enumerated. */
  public static final String PARAM_TRACE_LIMIT = "trace_count_limit";
  public static final int PARAM_TRACE_LIMIT_DEFAULT = -1;

  public static final String IF_THEN_SITUATION_SUFFIX = "if-then";
  public static final String GOTO_SITUATION_SUFFIX = "goto";

  public static boolean isIfThen(final AbstractCall abstractCall) {
    InvariantChecks.checkNotNull(abstractCall);

    // Self check.
    final String situationName = getSituationName(abstractCall);
    final boolean flag = situationName != null && situationName.endsWith(IF_THEN_SITUATION_SUFFIX);

    /*
    // FIXME: The check is temporary disabled
    final boolean result = abstractCall.isBranch() && abstractCall.isConditionalBranch();
    InvariantChecks.checkTrue(result == flag);

    return result;*/

    return flag;
  }

  public static boolean isGoto(final AbstractCall abstractCall) {
    InvariantChecks.checkNotNull(abstractCall);

    // Self check.
    final String situationName = getSituationName(abstractCall);
    final boolean flag = situationName != null && situationName.endsWith(GOTO_SITUATION_SUFFIX);

    /*
    // FIXME: The check is temporary disabled
    final boolean result = abstractCall.isBranch() && !abstractCall.isConditionalBranch();
    InvariantChecks.checkTrue(result == flag);

    return result;
    */

    return flag;
  }

  /** Branch execution limit: default value is 1. */
  private int maxBranchExecutions = PARAM_BRANCH_LIMIT_DEFAULT;
  /** Trace count limit: default value is -1 (no limitations). */
  private int maxExecutionTraces = PARAM_TRACE_LIMIT_DEFAULT;

  @Override
  public Class<BranchSolution> getSolutionClass() {
    return BranchSolution.class;
  }

  @Override
  public void configure(final Map<String, Object> attributes) {
    InvariantChecks.checkNotNull(attributes);

    final Object branchExecLimit = attributes.get(PARAM_BRANCH_LIMIT);
    maxBranchExecutions = branchExecLimit != null ?
        Integer.parseInt(branchExecLimit.toString()) : PARAM_BRANCH_LIMIT_DEFAULT;

    final Object traceCountLimit = attributes.get(PARAM_TRACE_LIMIT);
    maxExecutionTraces = traceCountLimit != null ?
        Integer.parseInt(traceCountLimit.toString()) : PARAM_TRACE_LIMIT_DEFAULT;
  }

  @Override
  public EngineResult<BranchSolution> solve(
      final EngineContext engineContext, final List<AbstractCall> abstractSequence) {
    InvariantChecks.checkNotNull(engineContext);
    InvariantChecks.checkNotNull(abstractSequence);

    // Collect information about labels.
    final LabelManager labels = new LabelManager();
    for (int i = 0; i < abstractSequence.size(); i++) {
      final AbstractCall call = abstractSequence.get(i);
      for (final Label label : call.getLabels()) {
        labels.addLabel(label, i);
      }
    }

    // Transform the abstract sequence into the branch structure.
    final BranchStructure branchStructure = new BranchStructure(abstractSequence.size());

    int delaySlot = 0;
    for (int i = 0; i < abstractSequence.size(); i++) {
      final AbstractCall abstractCall = abstractSequence.get(i);
      final BranchEntry branchEntry = branchStructure.get(i);

      final boolean isIfThen = isIfThen(abstractCall);
      final boolean isGoto = isGoto(abstractCall);

      // Set the branch entry type.
      if (isIfThen) {
        // Using branches in delay slots is prohibited.
        InvariantChecks.checkTrue(delaySlot == 0);
        branchEntry.setType(BranchEntry.Type.IF_THEN);
      } else if (isGoto) {
        // Using branches in delay slots is prohibited.
        InvariantChecks.checkTrue(delaySlot == 0);
        branchEntry.setType(BranchEntry.Type.GOTO);
      } else if (delaySlot > 0) {
        branchEntry.setType(BranchEntry.Type.DELAY_SLOT);
        delaySlot--;
      } else {
        branchEntry.setType(BranchEntry.Type.BASIC_BLOCK);
      }

      // Set the target label and start the delay slot.
      if (isIfThen || isGoto) {
        final Label label = abstractCall.getTargetLabel();

        final LabelManager.Target target = labels.resolve(label);
        InvariantChecks.checkNotNull(target, "Undefined label: " + label);

        branchEntry.setBranchLabel((int) target.getAddress());
        delaySlot = engineContext.getDelaySlotSize();
      }
    }

    Logger.debug("Branch Structure: %s", branchStructure);

    final Iterator<BranchSolution> iterator = new Iterator<BranchSolution>() {
      /** Iterator of branch structures. */
      private final Iterator<BranchStructure> branchStructureIterator =
          new SingleValueIterator<BranchStructure>(branchStructure);

      /** Iterator of branch structures and execution trances. */
      private final BranchExecutionIterator branchStructureExecutionIterator =
          new BranchExecutionIterator(
              branchStructureIterator, maxBranchExecutions, maxExecutionTraces);

      @Override
      public void init() {
        branchStructureExecutionIterator.init();
      }

      @Override
      public boolean hasValue() {
        return branchStructureExecutionIterator.hasValue();
      }

      @Override
      public BranchSolution value() {
        final BranchSolution branchSolution = new BranchSolution();
        branchSolution.setBranchStructure(branchStructureExecutionIterator.value());

        return branchSolution;
      }

      @Override
      public void next() {
        branchStructureExecutionIterator.next();
      }

      @Override
      public void stop() {
        branchStructureExecutionIterator.stop();
      }

      @Override
      public Iterator<BranchSolution> clone() {
        throw new UnsupportedOperationException();
      }
    };

    return new EngineResult<>(EngineResult.Status.OK, iterator, Collections.<String>emptyList());
  }

  @Override
  public void onStartProgram() {
    // Empty
  }

  @Override
  public void onEndProgram() {
    // Empty
  }
}
