/*
 * Copyright 2012-2016 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.translator.antlrex.log;

import static ru.ispras.fortress.util.InvariantChecks.checkNotNull;

/**
 * The LogWriter is a helper class that provides facilities to post messages to the log.
 * 
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public class LogWriter {
  private final SenderKind sender;
  private final String fileName;
  private final LogStore log;

  public LogWriter(final SenderKind sender, final String fileName, final LogStore log) {
    checkNotNull(sender);
    checkNotNull(fileName);
    checkNotNull(log);

    this.sender = sender;
    this.fileName = fileName;
    this.log = log;
  }

  private void report(final LogEntry.Kind kind, final String message) {
    log.append(new LogEntry(kind, sender, fileName, 0, 0, message));
  }

  /**
   * Reports an error message to the log.
   * 
   * @param message Error message.
   */
  public final void reportError(final String message) {
    report(LogEntry.Kind.ERROR, message);
  }

  /**
   * Reports an warning message to the log.
   * 
   * @param message Warning message.
   */
  public final void reportWarning(final String message) {
    report(LogEntry.Kind.WARNING, message);
  }
}
