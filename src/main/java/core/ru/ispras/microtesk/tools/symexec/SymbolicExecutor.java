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

package ru.ispras.microtesk.tools.symexec;

import ru.ispras.castle.util.Logger;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.solver.engine.smt.Cvc4Solver;
import ru.ispras.fortress.solver.engine.smt.SmtTextBuilder;
import ru.ispras.fortress.util.InvariantChecks;

import ru.ispras.microtesk.model.IsaPrimitive;
import ru.ispras.microtesk.model.Model;
import ru.ispras.microtesk.model.TemporaryVariables;
import ru.ispras.microtesk.options.Options;
import ru.ispras.microtesk.tools.Disassembler;
import ru.ispras.microtesk.tools.Disassembler.Output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class SymbolicExecutor {
  private SymbolicExecutor() {}

  public static boolean execute(
      final Options options,
      final String modelName,
      final String fileName) {
    InvariantChecks.checkNotNull(options);
    InvariantChecks.checkNotNull(modelName);
    InvariantChecks.checkNotNull(fileName);

    Logger.message("Analyzing file: %s...", fileName);
    final DisassemblerOutputFactory outputFactory = new DisassemblerOutputFactory();
    if (!Disassembler.disassemble(options, modelName, fileName, outputFactory)) {
      Logger.error("Failed to disassemble " + fileName);
    }

    final DisassemblerOutput output = outputFactory.getOutput();
    InvariantChecks.checkNotNull(output);

    final List<IsaPrimitive> instructions = output.getInstructions();
    InvariantChecks.checkNotNull(instructions);

    final List<Node> ssa =
      FormulaBuilder.buildFormulas(outputFactory.getModel(), instructions);

    final String smtFileName = fileName + ".smt2";
    writeSmt(smtFileName, ssa);

    Logger.message("Created file: %s", smtFileName);
    return true;
  }

  private static void writeSmt(
      final String fileName,
      final Collection<? extends Node> formulas) {
    final Cvc4Solver solver = new Cvc4Solver();

    try {
      SmtTextBuilder.saveToFile(
          fileName,
          Collections.<String>emptyList(),
          formulas,
          solver.getOperations()
      );
    } catch (final java.io.IOException e) {
      Logger.error(e.getMessage());
    }
  }

  private static final class DisassemblerOutput implements Disassembler.Output {
    private final TemporaryVariables tempVars;
    private final List<IsaPrimitive> instructions;

    private DisassemblerOutput(final TemporaryVariables tempVars) {
      InvariantChecks.checkNotNull(tempVars);

      this.tempVars = tempVars;
      this.instructions = new ArrayList<>();
    }

    @Override
    public void add(final IsaPrimitive primitive) {
      final String text = primitive.text(tempVars);
      Logger.debug(text);
      instructions.add(primitive);
    }

    @Override
    public void close() {
      // Nothing
    }

    public List<IsaPrimitive> getInstructions() {
      return instructions;
    }
  }

  private static final class DisassemblerOutputFactory implements Disassembler.OutputFactory {
    private DisassemblerOutput output = null;
    private Model model = null;

    @Override
    public Output createOutput(final Model model) {
      InvariantChecks.checkNotNull(model);
      final TemporaryVariables tempVars = model.getTempVars();

      this.output = new DisassemblerOutput(tempVars);
      this.model = model;

      return output;
    }

    public DisassemblerOutput getOutput() {
      return output;
    }

    public Model getModel() {
      return model;
    }
  }
}
