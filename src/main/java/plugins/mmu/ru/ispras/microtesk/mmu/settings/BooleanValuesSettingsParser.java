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

package ru.ispras.microtesk.mmu.settings;

import java.util.Map;

import ru.ispras.microtesk.settings.AbstractSettingsParser;

/**
 * {@link BooleanValuesSettingsParser} implements a parser of {@link BooleanValuesSettings}.
 * 
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class BooleanValuesSettingsParser extends AbstractSettingsParser<BooleanValuesSettings> {
  public static final String ATTR_NAME = "name";
  public static final String ATTR_VALUES = "values";

  public BooleanValuesSettingsParser() {
    super(BooleanValuesSettings.TAG);
  }

  @Override
  public final BooleanValuesSettings createSettings(final Map<String, String> attributes) {
    final String name = AbstractSettingsParser.getString(attributes.get(ATTR_NAME));
    final BooleanValuesSettings.Values values = AbstractSettingsParser.getEnum(
        BooleanValuesSettings.Values.class, attributes.get(ATTR_VALUES));

    return new BooleanValuesSettings(name, values);
  }
}
