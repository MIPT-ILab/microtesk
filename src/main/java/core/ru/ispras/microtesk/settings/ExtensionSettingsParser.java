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

package ru.ispras.microtesk.settings;

import java.util.Map;

/**
 * {@link ExtensionSettingsParser} implements a parser of {@link ExtensionSettings}.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class ExtensionSettingsParser extends AbstractSettingsParser<ExtensionSettings> {
  public static final String ATTR_NAME = "name";
  public static final String ATTR_PATH = "path";

  public ExtensionSettingsParser() {
    super(ExtensionSettings.TAG);
  }

  @Override
  public ExtensionSettings createSettings(final Map<String, String> attributes) {
    final String name = AbstractSettingsParser.getString(attributes.get(ATTR_NAME));
    final String path = AbstractSettingsParser.getString(attributes.get(ATTR_PATH));

    return new ExtensionSettings(name, path);
  }
}

