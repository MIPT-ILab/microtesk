/*
 * Copyright (c) 2014 ISPRAS (www.ispras.ru)
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * Call.java, Aug 27, 2014 12:04:59 PM Andrei Tatarnikov
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

package ru.ispras.microtesk.test.template;

import java.util.Collections;
import java.util.List;

public final class Call
{
    private final Primitive rootOperation;
    private final List<Label> labels;
    private final List<LabelReference> labelRefs;
    private final List<Output> outputs;

    Call(
        Primitive rootOperation,
        List<Label> labels,
        List<LabelReference> labelRefs,
        List<Output> outputs
        )
    {
        this.rootOperation = rootOperation;
        this.labels = Collections.unmodifiableList(labels);
        this.labelRefs = Collections.unmodifiableList(labelRefs);
        this.outputs = Collections.unmodifiableList(outputs);
    }

    public boolean isExecutable()
    {
        return null != rootOperation; 
    }

    public boolean isEmpty()
    {
        return !isExecutable() && labels.isEmpty() && outputs.isEmpty();
    }

    public Primitive getRootOperation()
    {
        return rootOperation;
    }

    public List<Label> getLabels()
    {
        return labels;
    }

    public List<LabelReference> getLabelReferences()
    {
        return labelRefs;
    }

    public List<Output> getOutputs()
    {
        return outputs;
    }

    public String getText()
    {
        return String.format("instruction call (root: %s)",
            isExecutable() ? rootOperation.getName() : "null");
    }
}
