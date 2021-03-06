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

package ru.ispras.microtesk.translator.antlrex.log;

/**
 * The simplest implementation of the {@link LogStore} interface, which posts messages to the
 * console (the "standard" error output stream).
 *
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class LogStoreConsole implements LogStore {
  public static LogStore INSTANCE = new LogStoreConsole();

  private LogStoreConsole() {}

  @Override
  public void append(final LogEntry entry) {
    System.err.println(entry);
  }
}
