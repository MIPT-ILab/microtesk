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

package ru.ispras.microtesk.model.api.tarmac;

import java.math.BigInteger;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.test.template.ConcreteCall;

public abstract class Record {
  private static long instructionId = -1;

  public static void resetInstructionCount() {
    instructionId = -1;
  }

  private final RecordKind kind;
  private final long time;

  public Record(final RecordKind kind, final long time) {
    this.kind = kind;
    this.time = time;
  }

  public RecordKind getKind() {
    return kind;
  }

  public long getTime() {
    return time;
  }

  @Override
  public String toString() {
    return String.format("%d clk", time);
  }

  public static Record newInstruction(final ConcreteCall call) {
    InvariantChecks.checkNotNull(call);
    return new Instruction(call);
  }

  private static class Instruction extends Record {
    private long instrId;
    private long addr;
    private String disasm;

    private Instruction(final ConcreteCall call) {
      super(RecordKind.INSTRUCT, ++instructionId);

      this.addr = call.getAddress();
      this.disasm = call.getText();

      try {
        final BigInteger image = new BigInteger(call.getImage(), 2);
        this.instrId = image.longValue();
      } catch (final NumberFormatException e) {
        Logger.error("Failed to parse image for instruction call %s: '%s'. Reason: %s.",
            disasm, call.getImage(), e.getMessage());
        this.addr = 0;
      }
    }

    @Override
    public String toString() {
      return String.format("%s IT (%d) %08x %08x A svc_ns : %s",
          super.toString(), getTime(), addr, instrId, disasm);
    }
  }
}
