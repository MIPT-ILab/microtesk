/*
 * Copyright (c) 2014 ISPRAS (www.ispras.ru)
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * TemplateBuilder.java, Aug 5, 2014 4:41:17 PM Andrei Tatarnikov
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

import java.util.Deque;
import java.util.LinkedList;

import ru.ispras.microtesk.model.api.metadata.MetaModel;

import ru.ispras.microtesk.test.sequence.Sequence;
import ru.ispras.microtesk.test.sequence.iterator.IIterator;

public final class Template
{
    private final MetaModel metaModel;

    private final Deque<BlockBuilder> blockBuilders;
    private CallBuilder callBuilder;

    private IIterator<Sequence<Call>> sequences;

    public Template(MetaModel metaModel)
    {
        _header("Begin Template");

        if (null == metaModel)
            throw new NullPointerException();

        this.metaModel = metaModel;

        this.blockBuilders = new LinkedList<BlockBuilder>();
        this.blockBuilders.push(new BlockBuilder());
        this.callBuilder = new CallBuilder();

        this.sequences = null;
    }

    public IIterator<Sequence<Call>> build()
    {
        _header("End Template");

        if (null != sequences)
            throw new IllegalStateException("The template is already built.");

        final BlockBuilder rootBuilder = blockBuilders.getLast();
        final Block rootBlock = rootBuilder.build();

        sequences = rootBlock.getIterator();
        return sequences;
    }

    public IIterator<Sequence<Call>> getSequences()
    {
        return sequences;
    }

    public BlockId getCurrentBlockId()
    {
        return blockBuilders.peek().getBlockId();
    }

    public BlockBuilder beginBlock()
    {
        endBuildingCall();
        
        final BlockBuilder parent = blockBuilders.peek();
        final BlockBuilder current = new BlockBuilder(parent);

        _trace("Begin block: " + current.getBlockId());

        blockBuilders.push(current);
        return current;
    }

    public void endBlock()
    {
        endBuildingCall();

        _trace("End block: " + getCurrentBlockId());

        final BlockBuilder builder = blockBuilders.pop();
        final Block block = builder.build();

        blockBuilders.peek().addBlock(block);
    }

    public Label addLabel(String name)
    {
        if (null == name)
            throw new NullPointerException();

        final Label label = new Label(name, getCurrentBlockId());
        _trace("Label: " + label.toString()); 

        callBuilder.addLabel(label);
        return label;
    }

    public void addOutput(Output output)
    {
        _trace(output.toString());
        callBuilder.addOutput(output);
    }

    public CallBuilder getCurrentCallBuilder()
    {
        return callBuilder;
    }

    public void endBuildingCall()
    {
        final Call call = callBuilder.build();
        blockBuilders.peek().addCall(call);

        _trace(String.format("Ended building a call (Empty = %b)", call.isEmpty()));
        this.callBuilder = new CallBuilder();
    }

    public PrimitiveBuilder getInstructionBuilder()
    {
        return new PrimitiveBuilder();
    }

    public PrimitiveBuilder getOperationBuilder()
    {
        return new PrimitiveBuilder();
    }

    public PrimitiveBuilder getModeBuilder()
    {
        return new PrimitiveBuilder();
    }

    private static void _trace(String s)
    {
        System.out.println(s);
    }

    private static void _header(String text)
    {
        final int LINE_WIDTH = 80;

        final int  prefixWidth = (LINE_WIDTH - text.length()) / 2;
        final int postfixWidth = LINE_WIDTH - prefixWidth - text.length();

        final StringBuilder sb = new StringBuilder();

        sb.append("\r\n");
        for(int i = 0; i < prefixWidth - 1; ++i) sb.append('-');
        sb.append(' ');

        sb.append(text);

        sb.append(' ');
        for(int i = 0; i < postfixWidth - 1; ++i) sb.append('-');
        sb.append("\r\n");

        _trace(sb.toString());
    }
}
