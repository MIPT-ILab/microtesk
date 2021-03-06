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

import java.util.Collection;

/**
 * {@link AccessSettings} describes how to access a memory region.
 *
 * @author <a href="mailto:kamkin@ispras.ru">Alexander Kamkin</a>
 */
public final class AccessSettings extends AbstractSettings {
  public static final String TAG = "access";

  private final String segment;

  public AccessSettings(final String segment) {
    super(TAG);

    this.segment = segment;
  }

  public String getSegment() {
    return segment;
  }

  @Override
  public Collection<AbstractSettings> get(final String tag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final AbstractSettings section) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return String.format("%s={segment=%s}", TAG, segment);
  }
}
