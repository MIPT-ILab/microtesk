/*
 * Copyright 2015-2018 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk;

import ru.ispras.microtesk.test.engine.Engine;
import ru.ispras.microtesk.test.engine.InitializerMaker;
import ru.ispras.microtesk.translator.Translator;
import ru.ispras.testbase.generator.DataGenerator;

import java.util.Map;

/**
 * {@link Plugin} is an interface of the MicroTESK tool plugins.
 *
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public interface Plugin {
  /**
   * Returns the plugin's translator (or {@code null}).
   *
   * @return the translator.
   */
  Translator<?> getTranslator();

  /**
   * Returns the plugin's engines with their names.
   *
   * @return the engines.
   */
  Map<String, Engine> getEngines();

  /**
   * Returns the plugin's initializer makers with their names.
   *
   * @return the initializer makers.
   */
  Map<String, InitializerMaker> getInitializerMakers();

  /**
   * Returns the plugin's data generators with their names.
   *
   * @return the data generators.
   */
  Map<String, DataGenerator> getDataGenerators();

  /**
   * Performs all required initialization before generation is started
   * (e.g. integrated different parts of the microprocessor model).
   */
  void initializeGenerationEnvironment();
}
