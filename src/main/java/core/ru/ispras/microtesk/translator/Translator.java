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

package ru.ispras.microtesk.translator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.TokenSource;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.translator.antlrex.Preprocessor;
import ru.ispras.microtesk.translator.antlrex.TokenSourceStack;
import ru.ispras.microtesk.translator.antlrex.log.LogStore;
import ru.ispras.microtesk.translator.antlrex.log.LogStoreConsole;
import ru.ispras.microtesk.translator.antlrex.symbols.SymbolTable;
import ru.ispras.microtesk.translator.generation.PackageInfo;
import ru.ispras.microtesk.utils.FileUtils;

public abstract class Translator<Ir> {
  private final Set<String> fileExtFilter;
  private final List<TranslatorHandler<Ir>> handlers;
  private final SymbolTable symbols;

  private String outDir;
  private TranslatorContext context;
  private LogStore log;

  private final Preprocessor preprocessor;
  private TokenSourceStack source;

  public Translator(final Set<String> fileExtFilter) {
    InvariantChecks.checkNotNull(fileExtFilter);

    this.fileExtFilter = fileExtFilter;
    this.handlers = new ArrayList<>();
    this.symbols = new SymbolTable();

    this.outDir = PackageInfo.DEFAULT_OUTDIR;
    this.context = null;
    this.log = LogStoreConsole.INSTANCE;

    this.preprocessor = new Preprocessor(this);
    this.source = null;
  }

  public final void addHandler(final TranslatorHandler<Ir> handler) {
    InvariantChecks.checkNotNull(handler);
    handlers.add(handler);
  }

  protected final void processIr(final Ir ir) {
    InvariantChecks.checkNotNull(ir);
    for (final TranslatorHandler<Ir> handler : handlers) {
      handler.processIr(ir);
    }

    if (null != context) {
      context.addIr(ir);
    }
  }

  protected SymbolTable getSymbols() {
    return symbols;
  }

  public final String getOutDir() {
    return outDir;
  }

  public final void setOutDir(final String outDir) {
    InvariantChecks.checkNotNull(outDir);
    this.outDir = outDir;
  }

  public final TranslatorContext getContext() {
    return context;
  }

  public final void setContext(final TranslatorContext context) {
    InvariantChecks.checkNotNull(context);
    this.context = context;
  }

  public final LogStore getLog() {
    return log;
  }

  public final void setLog(final LogStore log) {
    InvariantChecks.checkNotNull(log);
    this.log = log;
  }

  protected final Preprocessor getPreprocessor() {
    return preprocessor;
  }

  public final void addPath(final String path) {
    preprocessor.addPath(path);
  }

  public final void startLexer(final CharStream stream) {
    source.push(newLexer(stream));
  }

  protected final TokenSource startLexer(final List<String> filenames) {
    ListIterator<String> iterator = filenames.listIterator(filenames.size());

    // Create a stack of lexers.
    source = new TokenSourceStack();

    // Process the files in reverse order (emulate inclusion).
    while (iterator.hasPrevious()) {
      preprocessor.includeTokensFromFile(iterator.previous());
    }

    return source;
  }

  public final boolean start(final String... fileNames) {
    final List<String> filteredFileNames = new ArrayList<>();

    for (final String fileName : fileNames) {
      final String fileExt = FileUtils.getFileExtension(fileName).toLowerCase();
      if (fileExtFilter.contains(fileExt)) {
        if (!new File(fileName).exists()) {
          Logger.error("FILE DOES NOT EXISTS: " + fileName);
          return false;
        }

        filteredFileNames.add(fileName);
      }
    }

    if (!filteredFileNames.isEmpty()) {
      start(filteredFileNames);
    }

    return true;
  }

  protected abstract TokenSource newLexer(CharStream stream);
  protected abstract void start(final List<String> fileNames);
}
