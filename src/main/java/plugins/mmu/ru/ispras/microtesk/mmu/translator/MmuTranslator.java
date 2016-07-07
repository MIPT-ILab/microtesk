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

package ru.ispras.microtesk.mmu.translator;

import java.io.FileReader;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.mmu.translator.generation.sim.SimGenerator;
import ru.ispras.microtesk.mmu.translator.generation.spec.SpecGenerator;
import ru.ispras.microtesk.mmu.translator.grammar.MmuLexer;
import ru.ispras.microtesk.mmu.translator.grammar.MmuParser;
import ru.ispras.microtesk.mmu.translator.grammar.MmuTreeWalker;
import ru.ispras.microtesk.mmu.translator.ir.Buffer;
import ru.ispras.microtesk.mmu.translator.ir.Ir;
import ru.ispras.microtesk.mmu.translator.ir.Memory;
import ru.ispras.microtesk.translator.Translator;
import ru.ispras.microtesk.translator.antlrex.ReservedKeywords;
import ru.ispras.microtesk.utils.FileUtils;

public final class MmuTranslator extends Translator<Ir> {
  private static final Set<String> FILTER = Collections.singleton(".mmu");

  public MmuTranslator() {
    super(FILTER);

    getSymbols().defineReserved(MmuSymbolKind.KEYWORD, ReservedKeywords.JAVA);
    getSymbols().defineReserved(MmuSymbolKind.KEYWORD, ReservedKeywords.RUBY);

    addHandler(new SimGenerator(this));
    addHandler(new SpecGenerator(this));
  }

  @Override
  protected TokenSource newLexer(final CharStream stream) {
    return new MmuLexer(stream, getPreprocessor(), getSymbols());
  }

  @Override
  public void start(final List<String> fileNames) {
    InvariantChecks.checkNotNull(fileNames);

    final String fileName = fileNames.get(0);
    final String modelName = FileUtils.getShortFileNameNoExt(fileName);

    Logger.message("Translating: " + fileName);
    Logger.message("Model name: " + modelName);
    Logger.message("");

    final Ir ir = new Ir(modelName);

    try {
      final ANTLRReaderStream input = new ANTLRReaderStream(new FileReader(fileName));
      input.name = fileName;

      final TokenSource source = startLexer(fileNames);

      final CommonTokenStream tokens = new TokenRewriteStream();
      tokens.setTokenSource(source);

      final MmuParser parser = new MmuParser(tokens);
      parser.assignLog(getLog());
      parser.assignSymbols(getSymbols());
      parser.commonParser.assignLog(getLog());
      parser.commonParser.assignSymbols(getSymbols());

      final MmuParser.startRule_return r = parser.startRule();
      final CommonTree t = (CommonTree) r.getTree();

      Logger.debug("AST: " + t.toStringTree());

      if (!parser.isCorrect()) {
        Logger.error("TRANSLATION WAS INTERRUPTED DUE TO SYNTACTIC ERRORS.");
        return;
      }

      final CommonTreeNodeStream nodes = new CommonTreeNodeStream(t);
      nodes.setTokenStream(tokens);

      final MmuTreeWalker walker = new MmuTreeWalker(nodes);
      walker.assignLog(getLog());
      walker.assignSymbols(getSymbols());
      walker.assignIR(ir);
      walker.assignContext(getContext());

      walker.startRule();

      if (!walker.isCorrect()) {
        Logger.error("TRANSLATION WAS INTERRUPTED DUE TO SEMANTIC ERRORS.");
        return;
      }

      if (!checkIr(ir)) {
        Logger.error("TRANSLATION WAS INTERRUPTED DUE TO SEMANTIC ERRORS.");
        return;
      }

      processIr(ir);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  // Performs size checks for mapped buffers.
  private boolean checkIr(final Ir ir) {
    final ru.ispras.microtesk.translator.nml.ir.Ir isaIr =
        getContext().getIr(ru.ispras.microtesk.translator.nml.ir.Ir.class);

    for (final Buffer buffer: ir.getBuffers().values()) {
      switch (buffer.getKind()) {
        case MEMORY: {
          InvariantChecks.checkTrue(ir.getMemories().size() == 1);
          final Memory mmu = ir.getMemories().values().iterator().next();

          if (buffer.getEntry().getBitSize() != mmu.getDataArg().getBitSize()) {
            Logger.error("Size of %s.Entry must match size of %s data argument: %d.",
                buffer.getId(), mmu.getId(), mmu.getDataArg().getBitSize());
            return false;
          }

          break;
        }

        case REGISTER: {
          final ru.ispras.microtesk.translator.nml.ir.shared.MemoryExpr register =
              isaIr.getMemory().get(buffer.getId());

          if (buffer.getEntry().getBitSize() != register.getType().getBitSize()) {
            Logger.error("Size of %s.Entry must match size of %s registers: %d.",
                buffer.getId(), buffer.getId(), register.getType().getBitSize());
            return false;
          }

          break;
        }
      }
    }

    return true;
  }
}
