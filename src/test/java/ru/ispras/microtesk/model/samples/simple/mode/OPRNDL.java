/*
 * Copyright (c) 2012 ISPRAS
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * OPRNDL.java, Nov 20, 2012 1:20:44 PM Andrei Tatarnikov
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package ru.ispras.microtesk.model.samples.simple.mode;

import ru.ispras.microtesk.model.api.instruction.AddressingMode;

/*
    mode OPRNDL = MEM | REG | IREG
*/

public abstract class OPRNDL extends AddressingMode 
{
    public static final String NAME = "OPRNDL";
    
    public static final IInfo INFO = new InfoOrRule(NAME,
        MEM.INFO,
        REG.INFO,
        IREG.INFO
    );     
}
