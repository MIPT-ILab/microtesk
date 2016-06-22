/*
 * Copyright 2012-2015 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.model.samples.simple.op;

import static ru.ispras.microtesk.model.samples.simple.shared.Shared.DEST;
import static ru.ispras.microtesk.model.samples.simple.shared.Shared.SRC1;
import static ru.ispras.microtesk.model.samples.simple.shared.Shared.SRC2;

import java.util.Map;

import ru.ispras.microtesk.model.api.ArgumentMode;
import ru.ispras.microtesk.model.api.instruction.ArgumentDecls;
import ru.ispras.microtesk.model.api.instruction.AddressingMode;
import ru.ispras.microtesk.model.api.instruction.Operation;
import ru.ispras.microtesk.model.samples.simple.mode.OPRNDL;
import ru.ispras.microtesk.model.samples.simple.mode.OPRNDR;

/*
 * op Add() syntax = "add" image = "00" action = { DEST = SRC1 + SRC2; }
 */

public final class Add extends Operation {
  private static class Info extends InfoAndRule {
    Info() {
      super(
          Add.class,
          Add.class.getSimpleName(),
          false,
          new ArgumentDecls(),
          false,
          false,
          false,
          false,
          false,
          0,
          new Shortcuts()
             .addShortcut(new Info_Instruction(), "#root")
          );
    }

    @Override
    public Operation create(final Map<String, Object> args) {
      return new Add();
    }
  }

  // A short way to instantiate the operation with together with parent operations.
  private static class Info_Instruction extends InfoAndRule {
    Info_Instruction() {
      super(
          Instruction.class,
          "Add",
          true,
          new ArgumentDecls()
              .add("op1", ArgumentMode.INOUT, OPRNDL.INFO)
              .add("op2", ArgumentMode.IN, OPRNDR.INFO),
          false,
          false,
          false,
          false,
          false,
          0
          );
    }

    @Override
    public Operation create(final Map<String, Object> args) {
      final AddressingMode op1 = (AddressingMode) getArgument("op1", args);
      final AddressingMode op2 = (AddressingMode) getArgument("op2", args);

      return new Instruction(new Arith_Mem_Inst(new Add(), op1, op2));
    }
  }

  public static final IInfo INFO = new Info();

  public Add() {}

  @Override
  public String syntax() {
    return "add";
  }

  @Override
  public String image() {
    return "00";
  }

  @Override
  public void action() {
    DEST.access().store(
        SRC1.access().load().add(SRC2.access().load()));
  }
}
