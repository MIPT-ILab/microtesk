/*
 * Copyright 2013-2016 ISP RAS (http://www.ispras.ru)
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import ru.ispras.fortress.randomizer.Randomizer;
import ru.ispras.fortress.solver.Environment;
import ru.ispras.fortress.solver.SolverId;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.Plugin;
import ru.ispras.microtesk.SysUtils;
import ru.ispras.microtesk.model.api.IModel;
import ru.ispras.microtesk.model.api.exception.ConfigurationException;
import ru.ispras.microtesk.model.api.metadata.MetaModel;
import ru.ispras.microtesk.model.api.state.Reader;
import ru.ispras.microtesk.model.api.tarmac.Tarmac;
import ru.ispras.microtesk.settings.AllocationSettings;
import ru.ispras.microtesk.settings.GeneratorSettings;
import ru.ispras.microtesk.test.sequence.GeneratorConfig;
import ru.ispras.microtesk.test.sequence.engine.Adapter;
import ru.ispras.microtesk.test.sequence.engine.AdapterResult;
import ru.ispras.microtesk.test.sequence.engine.Engine;
import ru.ispras.microtesk.test.sequence.engine.EngineContext;
import ru.ispras.microtesk.test.sequence.engine.SelfCheckEngine;
import ru.ispras.microtesk.test.sequence.engine.TestSequenceEngine;
import ru.ispras.microtesk.test.sequence.engine.allocator.ModeAllocator;
import ru.ispras.microtesk.test.sequence.engine.utils.EngineUtils;
import ru.ispras.microtesk.test.template.Block;
import ru.ispras.microtesk.test.template.BufferPreparatorStore;
import ru.ispras.microtesk.test.template.Call;
import ru.ispras.microtesk.test.template.ConcreteCall;
import ru.ispras.microtesk.test.template.DataManager;
import ru.ispras.microtesk.test.template.ExceptionHandler;
import ru.ispras.microtesk.test.template.Label;
import ru.ispras.microtesk.test.template.PreparatorStore;
import ru.ispras.microtesk.test.template.StreamStore;
import ru.ispras.microtesk.test.template.Template;
import ru.ispras.microtesk.test.template.Template.Section;
import ru.ispras.microtesk.translator.nml.coverage.TestBase;
import ru.ispras.microtesk.utils.StringUtils;
import ru.ispras.testbase.knowledge.iterator.Iterator;

public final class TestEngine {
  private static TestEngine instance = null;

  public static TestEngine getInstance() {
    return instance;
  }

  private final IModel model;
  private Statistics statistics;

  // Architecture-specific settings
  private static GeneratorSettings settings;

  private TestEngine(final IModel model) {
    InvariantChecks.checkNotNull(model);

    this.statistics = null;
    this.model = model;

    Reader.setModel(model);
    initSolverPaths(SysUtils.getHomeDir());
  }

  public IModel getModel() {
    return model;
  }

  public String getModelName() {
    return model.getName();
  }

  public MetaModel getMetaModel() {
    return model.getMetaData();
  }

  public static GeneratorSettings getGeneratorSettings() {
    return settings;
  }

  public static void setGeneratorSettings(final GeneratorSettings value) {
    InvariantChecks.checkNotNull(value);

    settings = value;

    final AllocationSettings allocation = value.getAllocation();
    if (allocation != null) {
      ModeAllocator.init(allocation);
    }
  }

  public static Statistics generate(
      final String modelName,
      final String templateFile,
      final List<Plugin> plugins) throws Throwable {
    Logger.debug("Home: " + SysUtils.getHomeDir());
    Logger.debug("Current directory: " + SysUtils.getCurrentDir());
    Logger.debug("Model name: " + modelName);
    Logger.debug("Template file: " + templateFile);

    final IModel model = SysUtils.loadModel(modelName);
    if (null == model) {
      reportAborted("Failed to load the %s model.", modelName);
      return null;
    }

    instance = new TestEngine(model);

    try {
      for (final Plugin plugin : plugins) {
        plugin.initializeGenerationEnvironment();
      }
    } catch (final GenerationAbortedException e) {
      reportAborted(e.getMessage());
      return null;
    }

    return instance.processTemplate(templateFile);
  }

  private Statistics processTemplate(final String templateFile) throws Throwable {
    final String scriptsPath = String.format(
        "%s/lib/ruby/microtesk.rb", SysUtils.getHomeDir());

    final ScriptingContainer container = new ScriptingContainer();
    container.setArgv(new String[] {templateFile});

    try {
      container.runScriptlet(PathType.ABSOLUTE, scriptsPath);
    } catch(final org.jruby.embed.EvalFailedException e) {
      // JRuby wraps exceptions that occur in Java libraries it calls into
      // EvalFailedException. To handle them correctly, we need to unwrap them.

      try {
        throw e.getCause();
      } catch (final GenerationAbortedException e2) {
        handleGenerationAborted(e2);
      }
    }

    return statistics;
  }

  private void handleGenerationAborted(final GenerationAbortedException e) {
    reportAborted(e.getMessage());

    if (null != Printer.getLastFileName()) {
      new File(Printer.getLastFileName()).delete();
      if (null != statistics) {
        statistics.decPrograms();
      }
    }
  }

  public Template newTemplate() {
    statistics = new Statistics(TestSettings.getProgramLengthLimit(),
                                TestSettings.getTraceLengthLimit());
    statistics.pushActivity(Statistics.Activity.PARSING);

    final Printer printer = new Printer(model.getStateObserver(), statistics);
    final DataManager dataManager = new DataManager(printer, statistics);
 
    final EngineContext context = new EngineContext(
        model,
        dataManager,
        new PreparatorStore(),
        new BufferPreparatorStore(),
        new StreamStore(),
        settings,
        statistics,
        TestSettings.isDefaultTestData()
        );

    if (TestSettings.isTarmacLog()) {
      Tarmac.initialize(TestSettings.getOutDir(), TestSettings.getCodeFilePrefix());
    }

    final TemplateProcessor processor = new TemplateProcessor(context, printer);

    return new Template(context, processor);
  }

  public void process(final Template template) throws ConfigurationException, IOException {
    template.getProcessor().finish();

    final Set<Block> unusedBlocks = template.getUnusedBlocks();
    if (!unusedBlocks.isEmpty()) {
      Logger.warning("Unused blocks have been detected at: %s",
          StringUtils.toString(unusedBlocks, ", ", new StringUtils.Converter<Block>() {
              @Override
              public String toString(final Block o) {return o.getWhere().toString();}
          }));
    }
  }

  private static class TemplateProcessor implements Template.Processor {
    private final EngineContext engineContext;
    private final Executor executor;
    private final Printer printer;
 
    private boolean needCreateNewFile = true;
    private String fileName = null;
    private int testIndex = 0;

    private TestSequence prologue = null;
    private Block epilogueBlock = null;

    private List<ConcreteCall> externalCode = new ArrayList<>();

    private TemplateProcessor(final EngineContext engineContext, final Printer printer) {
      this.engineContext = engineContext;
      this.executor = new Executor(engineContext);
      this.printer = printer;
    }

    @Override
    public void process(final Section section, final Block block) {
      InvariantChecks.checkNotNull(section);
      InvariantChecks.checkNotNull(block);

      try {
        if (section == Section.PRE) {
          prologue = makeTestSequenceForExternalBlock(block);
        } else if (section == Section.POST) {
          epilogueBlock = block;
        } else {
          processBlock(block);
        }
      } catch (final ConfigurationException | IOException e) {
        throw new GenerationAbortedException(e.getMessage());
      }
    }

    @Override
    public void finish() {
      if (!needCreateNewFile) {
        try {
          finishCurrentFile();
        } catch (final ConfigurationException e) {
          Logger.error(e.getMessage());
        }

        // No instructions were added to the newly created file, it must be deleted
        if (engineContext.getStatistics().getProgramLength() == 0) {
          new File(fileName).delete();
          engineContext.getStatistics().decPrograms();
        }
      }

      Logger.debugHeader("Ended Processing Template");

      engineContext.getStatistics().popActivity(); // PARSING
      engineContext.getStatistics().saveTotalTime();
    }

    private void processBlock(final Block block) throws ConfigurationException, IOException {
      engineContext.getStatistics().pushActivity(Statistics.Activity.SEQUENCING);

      final Iterator<List<Call>> sequenceIt = block.getIterator();
      final TestSequenceEngine engine = getEngine(block);

      boolean isFirstSequence = true;
      sequenceIt.init();

      while (sequenceIt.hasValue()) {
        Logger.debugHeader("Processing Abstract Sequence");

        final Iterator<AdapterResult> iterator = engine.process(engineContext, sequenceIt.value());
        for (iterator.init(); iterator.hasValue(); iterator.next()) {
          if (needCreateNewFile) {
            createNewFile();
            needCreateNewFile = false;
          }

          if (isFirstSequence) {
            printer.printText("");
            printer.printSeparatorToFile(String.format("Test %d", testIndex++));
            isFirstSequence = false;
          }

          final TestSequence concreteSequence = getTestSequence(iterator.value());

          final int testCaseIndex = engineContext.getStatistics().getSequences();
          engineContext.getDataManager().setTestCaseIndex(testCaseIndex);

          final String sequenceId = String.format("Test Case %d", testCaseIndex);
          processTestSequence(concreteSequence, sequenceId, testCaseIndex, true);

          processSelfChecks(concreteSequence.getChecks(), testCaseIndex);

          engineContext.getStatistics().incSequences();
          Logger.debugHeader("");

          needCreateNewFile = isFileLengthLimitExceeded();
          if (needCreateNewFile) {
            finishCurrentFile();
            engineContext.setAddress(prologue.getEndAddress());
          }
        } // Concrete sequence iterator

        sequenceIt.next();
      } // Abstract sequence iterator

      engineContext.getStatistics().popActivity();
    }

    private void processExternalBlock(
        final Block block,
        final String headerText) throws ConfigurationException {
      try {
        engineContext.getStatistics().pushActivity(Statistics.Activity.SEQUENCING);
        final TestSequence sequence = makeTestSequenceForExternalBlock(block);
        processTestSequence(sequence, headerText, Label.NO_SEQUENCE_INDEX, true);
      } finally {
        engineContext.getStatistics().popActivity();
      }
    }

    private void processTestSequence(
        final TestSequence sequence,
        final String sequenceId,
        final int sequenceIndex,
        final boolean abortOnUndefinedLabel) throws ConfigurationException {
      Logger.debugHeader("Constructed %s", sequenceId);
      printer.printSequence(null, sequence);

      Logger.debugHeader("Executing %s", sequenceId);
      final List<ConcreteCall> calls =
          executor.execute(sequence, sequenceIndex, abortOnUndefinedLabel);
      externalCode.addAll(calls);

      Logger.debugHeader("Printing %s to %s", sequenceId, fileName);
      printer.printSubheaderToFile(sequenceId);
      printer.printSequence(sequence);
    }

    private void processSelfChecks(
        final List<SelfCheck> selfChecks,
        final int testCaseIndex) throws ConfigurationException {
      InvariantChecks.checkNotNull(selfChecks);

      if (!TestSettings.isSelfChecks()) {
        return;
      }

      final String sequenceId = String.format("Self-Checks for Test Case %d", testCaseIndex);
      Logger.debugHeader("Preparing %s", sequenceId);

      final TestSequence selfCheckSequence = SelfCheckEngine.solve(engineContext, selfChecks);
      processTestSequence(selfCheckSequence, sequenceId, testCaseIndex, false);
    }

    private boolean isFileLengthLimitExceeded() {
      return engineContext.getStatistics().isProgramLengthLimitExceeded() ||
             engineContext.getStatistics().isTraceLengthLimitExceeded();
    }

    private void createNewFile() throws IOException, ConfigurationException {
      fileName = printer.createNewFile();
      Tarmac.createFile();

      engineContext.getStatistics().incPrograms();
      processTestSequence(prologue, "Prologue", Label.NO_SEQUENCE_INDEX, true);
    }

    private void finishCurrentFile() throws ConfigurationException {
      try {
        processExternalBlock(epilogueBlock, "Epilogue");

        if (engineContext.getDataManager().containsDecls()) {
          engineContext.getDataManager().printData(printer);
          engineContext.getDataManager().clearLocalData();
        }
      } finally {
        externalCode = new ArrayList<>();
        printer.close();
        Tarmac.closeFile();
      }
    }

    private TestSequence makeTestSequenceForExternalBlock(
        final Block block) throws ConfigurationException {
      InvariantChecks.checkNotNull(block);
      InvariantChecks.checkTrue(block.isExternal());

      final TestSequenceEngine engine = getEngine(block);
      final List<Call> abstractSequence = getSingleSequence(block);

      final Iterator<AdapterResult> iterator = engine.process(engineContext, abstractSequence);
      return getSingleTestSequence(iterator);
    }

    @Override
    public void defineExceptionHandler(final ExceptionHandler handler) {
      final String exceptionFileName = String.format(
          "%s.%s", TestSettings.getExceptionFilePrefix(), TestSettings.getCodeFileExtension());

      Logger.debugHeader("Processing Exception Handler (%s)", exceptionFileName);
      InvariantChecks.checkNotNull(handler);

      final PrintWriter fileWriter;
      try {
        fileWriter = printer.newFileWriter(exceptionFileName);
      } catch (final IOException e) {
        throw new GenerationAbortedException(String.format(
            "Failed to create the %s file. Reason: %s", exceptionFileName, e.getMessage()));
      }

      try {
        final Map<String, List<ConcreteCall>> handlers = new LinkedHashMap<>();
        for (final ExceptionHandler.Section section : handler.getSections()) {
          final TestSequence concreteSequence =
              makeTestSequenceForExceptionHandler(engineContext, section);

          final List<ConcreteCall> handlerCalls = concreteSequence.getAll();
          for (final String exception : section.getExceptions()) {
            if (null != handlers.put(exception, handlerCalls)) {
              Logger.warning("Exception handler for %s is redefined.", exception);
            }
          }

          fileWriter.println();
          Logger.debug("");

          printer.printCommentToFile(fileWriter,
              String.format("Exceptions: %s", section.getExceptions()));

          final String org = String.format(".org 0x%x", section.getAddress());
          Logger.debug(org);
          printer.printToFile(fileWriter, org);
          printer.printSequence(fileWriter, concreteSequence);
        }

        executor.setExceptionHandlers(handlers);
      } catch (final ConfigurationException e) { 
        Logger.error(e.getMessage());
      } finally {
        fileWriter.close();
        Logger.debugBar();
      }
    }
  }

  private static TestSequenceEngine getEngine(final Block block) throws ConfigurationException {
    InvariantChecks.checkNotNull(block);

    final String engineName;
    final String adapterName;

    if (block.isExternal()) {
      engineName = "trivial";
      adapterName = engineName;
    } else {
      engineName = block.getAttribute("engine", "default");
      adapterName = block.getAttribute("adapter", engineName);
    }

    final Engine<?> engine = GeneratorConfig.get().getEngine(engineName);
    InvariantChecks.checkNotNull(engine);

    final Adapter<?> adapter = GeneratorConfig.get().getAdapter(adapterName);
    InvariantChecks.checkNotNull(adapter);

    if (!adapter.getSolutionClass().isAssignableFrom(engine.getSolutionClass())) {
      throw new IllegalStateException("Mismatched solver/adapter pair");
    }

    final TestSequenceEngine testSequenceEngine = new TestSequenceEngine(engine, adapter);
    testSequenceEngine.configure(block.getAttributes());

    return testSequenceEngine;
  }

  private static List<Call> getSingleSequence(final Block block) {
    InvariantChecks.checkNotNull(block);

    final Iterator<List<Call>> iterator = block.getIterator();
    iterator.init();

    if (!iterator.hasValue()) {
      return Collections.emptyList();
    }

    final List<Call> result = iterator.value();

    iterator.next();
    InvariantChecks.checkFalse(iterator.hasValue(), "A single sequence is expected.");

    return result;
  }

  private static TestSequence getTestSequence(final AdapterResult adapterResult) {
    InvariantChecks.checkNotNull(adapterResult);

    if (adapterResult.getStatus() != AdapterResult.Status.OK) {
      throw new GenerationAbortedException(String.format(
         "Adapter Error: %s", adapterResult.getErrors()));
    }

    final TestSequence result = adapterResult.getResult();
    InvariantChecks.checkNotNull(result);

    return result;
  }

  private static TestSequence getSingleTestSequence(final Iterator<AdapterResult> iterator) {
    InvariantChecks.checkNotNull(iterator);

    iterator.init();
    InvariantChecks.checkTrue(iterator.hasValue());

    final TestSequence result = getTestSequence(iterator.value());

    iterator.next();
    InvariantChecks.checkFalse(iterator.hasValue(), "A single sequence is expected.");

    return result;
  }

  private static TestSequence makeTestSequenceForExceptionHandler(
      final EngineContext engineContext,
      final ExceptionHandler.Section section) throws ConfigurationException {
    InvariantChecks.checkNotNull(engineContext);
    InvariantChecks.checkNotNull(section);

    final List<ConcreteCall> concreteCalls =
        EngineUtils.makeConcreteCalls(engineContext, section.getCalls());

    final TestSequence.Builder concreteSequenceBuilder = new TestSequence.Builder();
    concreteSequenceBuilder.add(concreteCalls);

    final TestSequence result = concreteSequenceBuilder.build();
    final long address = section.getAddress().longValue();
    result.setAddress(address);

    return result;
  }

  private static void initSolverPaths(final String home) {
    final ru.ispras.fortress.solver.Solver z3Solver = SolverId.Z3_TEXT.getSolver(); 
    if (null == z3Solver.getSolverPath()) {
      // If the Z3_PATH environment variable is not set, we set up default solver path
      // in hope to find the the tool there.
      final String z3Path = (home != null ? home : ".") + "/tools/z3/";
      if (Environment.isUnix()) {
        z3Solver.setSolverPath(z3Path + "unix/z3/bin/z3");
      } else if (Environment.isWindows()) {
        z3Solver.setSolverPath(z3Path + "windows/z3/bin/z3.exe");
      } else if (Environment.isOSX()) {
        z3Solver.setSolverPath(z3Path + "osx/z3/bin/z3");
      } else {
        throw new UnsupportedOperationException(String.format(
            "Unsupported platform: %s.", Environment.getOSName()));
      }
    }

    final ru.ispras.fortress.solver.Solver cvc4Solver = SolverId.CVC4_TEXT.getSolver();
    if (null == cvc4Solver.getSolverPath()) {
      // If the CVC4_PATH environment variable is not set, we set up default solver path
      // in hope to find the the tool there.
      final String cvc4Path = (home != null ? home : ".") + "/tools/cvc4/";
      if (Environment.isUnix()) {
        cvc4Solver.setSolverPath(cvc4Path + "unix/cvc4");
      } else if (Environment.isWindows()) {
        cvc4Solver.setSolverPath(cvc4Path + "windows/cvc4.exe");
      } else if (Environment.isOSX()) {
        cvc4Solver.setSolverPath(cvc4Path + "osx/cvc4");
      } else {
        throw new UnsupportedOperationException(String.format(
            "Unsupported platform: %s.", Environment.getOSName()));
      }
    }
  }

  private static void reportAborted(final String format, final Object... args) {
    Logger.error(format, args);
    Logger.message("Generation Aborted");
  }

  public static void setRandomSeed(int seed) {
    Randomizer.get().setSeed(seed);
  }

  public static void setSolver(final String solverName) {
    if ("z3".equalsIgnoreCase(solverName)) {
      TestBase.setSolverId(SolverId.Z3_TEXT);
    } else if ("cvc4".equalsIgnoreCase(solverName)) {
      TestBase.setSolverId(SolverId.CVC4_TEXT);
    } else {
      Logger.warning("Unknown solver: %s. Default solver will be used.", solverName);
    }
  }
}
