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

package ru.ispras.microtesk.translator.antlrex;

import ru.ispras.fortress.util.InvariantChecks;

/**
 * The {@code SemanticError} class describes a semantic error in the specification detected
 * by a translator. This is the most trivial implementation that just encapsulates a string.
 *
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public class SemanticError implements ISemanticError {
  private final String message;

  public SemanticError(String message) {
    InvariantChecks.checkNotNull(message);
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
