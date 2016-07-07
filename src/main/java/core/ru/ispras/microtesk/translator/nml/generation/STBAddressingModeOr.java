/*
 * Copyright 2012-2014 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.translator.nml.generation;

import static ru.ispras.microtesk.translator.generation.PackageInfo.MODE_PACKAGE_FORMAT;

import java.util.ArrayList;
import java.util.List;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import ru.ispras.microtesk.model.api.instruction.AddressingMode;
import ru.ispras.microtesk.translator.generation.STBuilder;
import ru.ispras.microtesk.translator.nml.ir.primitive.Primitive;
import ru.ispras.microtesk.translator.nml.ir.primitive.PrimitiveOR;

final class STBAddressingModeOr implements STBuilder {
  private final String modelName;
  private final PrimitiveOR mode;

  public STBAddressingModeOr(String modelName, PrimitiveOR mode) {
    assert mode.getKind() == Primitive.Kind.MODE;

    this.modelName = modelName;
    this.mode = mode;
  }

  @Override
  public ST build(STGroup group) {
    final ST t = group.getInstanceOf("modeor");

    t.add("name", mode.getName());
    t.add("pack", String.format(MODE_PACKAGE_FORMAT, modelName));

    t.add("imps", AddressingMode.class.getName());
    t.add("base", AddressingMode.class.getSimpleName());

    final List<String> modeNames = new ArrayList<String>(mode.getORs().size());
    for (Primitive p : mode.getORs())
      modeNames.add(p.getName());

    t.add("modes", modeNames);

    return t;
  }
}
