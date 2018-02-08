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

package ru.ispras.microtesk.mmu.translator.ir;

import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.utils.FormatMarker;

import java.util.Collections;
import java.util.List;

public final class StmtTrace extends Stmt {
  private final String format;
  private final List<FormatMarker> markers;
  private final List<Node> args;

  public StmtTrace(final String format) {
    this(format, Collections.<FormatMarker>emptyList(), Collections.<Node>emptyList());
  }

  public StmtTrace(
      final String format,
      final List<FormatMarker> markers,
      final List<Node> args) {
    super(Kind.TRACE);

    InvariantChecks.checkNotNull(format);
    InvariantChecks.checkNotNull(markers);
    InvariantChecks.checkNotNull(args);

    this.format = format;
    this.markers = markers;
    this.args = args;
  }

  public String getFormat() {
    return format;
  }

  public List<FormatMarker> getMarkers() {
    return markers;
  }

  public List<Node> getArguments() {
    return args;
  }

  @Override
  public String toString() {
    return String.format("stmt trace[\"%s\", %s]", format, args);
  }
}
