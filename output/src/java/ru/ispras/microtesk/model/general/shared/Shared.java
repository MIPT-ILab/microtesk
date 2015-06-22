/*
 * Copyright (c) ISPRAS
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * This file was automatically generated by MicroTEST based on the
 * information contained in the 'general.nml' specification file.
 * 
 * N.B. PLEASE DO NOT MODIFY THIS FILE.
 */

package ru.ispras.microtesk.model.general.shared;

import java.math.BigInteger;
import ru.ispras.microtesk.model.api.type.TypeId;
import ru.ispras.microtesk.model.api.type.Type;
import ru.ispras.microtesk.model.api.memory.Memory;
import ru.ispras.microtesk.model.api.memory.Label;
import ru.ispras.microtesk.model.api.state.Status;
import ru.ispras.microtesk.model.api.state.Resetter;

public final class Shared {
  private Shared() {}

  public static final Type MYTYPE = Type.def("MYTYPE", Type.CARD(0xa));
  public static final Type BYTE = Type.def("BYTE", Type.CARD(0x8));
  public static final Type CHAR = Type.def("CHAR", Type.INT(0x8));
  public static final Type WORD = Type.def("WORD", Type.CARD(0x10));
  public static final Type SHORT = Type.def("SHORT", Type.INT(0x10));
  public static final Type DWORD = Type.def("DWORD", Type.CARD(0x20));
  public static final Type LONG = Type.def("LONG", Type.INT(0x20));
  public static final Type bit = Type.def("bit", Type.CARD(0x1));
  public static final Type byte1 = Type.def("byte1", Type.CARD(0x8));
  public static final Type byte2 = Type.def("byte2", byte1);

  public static final Memory TEST_MEM1 = Memory.def(Memory.Kind.MEM, "TEST_MEM1", Type.CARD(0x20), 0x20);
  public static final Memory TEST_REG1 = Memory.def(Memory.Kind.REG, "TEST_REG1", DWORD, 0x10);
  public static final Memory TEST_VAR1 = Memory.def(Memory.Kind.VAR, "TEST_VAR1", LONG, 0x10);
  public static final Memory TEST_MEM2 = Memory.def(Memory.Kind.MEM, "TEST_MEM2", Type.CARD(0x20), 0x20);
  public static final Memory TEST_REG2 = Memory.def(Memory.Kind.REG, "TEST_REG2", Type.CARD(0x100), 0x21);

  public static final Memory[] __REGISTERS = {TEST_REG1, TEST_REG2};
  public static final Memory[] __MEMORY = {TEST_MEM1, TEST_MEM2};
  public static final Memory[] __VARIABLES = {TEST_VAR1};
  public static final Label[] __LABELS = {};

  public static final Status __CTRL_TRANSFER = new Status("__CTRL_TRANSFER", 0);
  public static final Status[] __STATUSES = {__CTRL_TRANSFER};

  public static final Resetter __RESETTER = new Resetter(__VARIABLES, __STATUSES);
}
 