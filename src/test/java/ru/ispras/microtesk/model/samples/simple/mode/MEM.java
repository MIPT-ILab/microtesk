/*
 * Copyright (c) 2012 ISPRAS
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * MEM.java, Nov 20, 2012 12:22:59 PM Andrei Tatarnikov
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

import java.util.Map;
import ru.ispras.microtesk.model.api.instruction.AddressingMode;
import ru.ispras.microtesk.model.api.instruction.IAddressingMode;
import ru.ispras.microtesk.model.api.memory.Location;
import ru.ispras.microtesk.model.api.data.Data;
import ru.ispras.microtesk.model.api.data.DataEngine;
import ru.ispras.microtesk.model.api.type.Type;

import static ru.ispras.microtesk.model.samples.simple.shared.Shared.*;

/*
mode MEM(i: index)=M[i]
syntax = format("(%d)", i)
image  = format("%6b", i)
*/

public class MEM extends AddressingMode
{
    public static final String NAME = "MEM";

    public static final Map<String, Type> DECLS = new ParamDeclBuilder()
        .declareParam("i", index).build();

    public static final IFactory FACTORY = new IFactory()
    {
        @Override
        public IAddressingMode create(Map<String, Data> args)
        {
            return new MEM(args);
        }
    };

    public static final IInfo INFO = new Info(MEM.class, NAME, FACTORY, DECLS);

    private final Location i;

    public MEM(Map<String, Data> args)
    {
        this.i = getArgument("i", DECLS, args);
    }
 
    @Override
    public String syntax()
    { 
        return String.format("(%d)", DataEngine.intValue(i.getDataCopy()));
    }

    @Override
    public String image()
    { 
        // TODO: NOT SUPPORTED
        // image  = format("%6b", i)
        return null;
    }

    @Override
    public void action()
    {
    }

    @Override
    public Location access()
    {
        return M.access(DataEngine.intValue(i.load()));
    }
}
