/*
 * Copyright (c) 2014 ISPRAS
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * NodeInfo.java, Jan 27, 2014 2:09:43 PM Andrei Tatarnikov
 */

package ru.ispras.microtesk.translator.simnml.ir.expression2;

import java.util.Collections;
import java.util.List;

import ru.ispras.microtesk.translator.simnml.ir.location.Location;
import ru.ispras.microtesk.translator.simnml.ir.shared.LetConstant;
import ru.ispras.microtesk.translator.simnml.ir.valueinfo.ValueInfo;

/*

Design notes:

  Kind (kind of the node, determines the set of maintained attributes).
  Source (Location, NamedConstant, Constant, Operator (including conditions), depending on Kind).
  ValueInfo (current, resulting, top-level, final value).

  Coercions (coercion (explicit cast) can be applied zero or more times to all element kinds):
    ValueInfoBeforeCoercion, array of ValueInfo: first is initial value before first coercion,
    last is value before final coercion. Value after the final coercion is ValueInfo.

  # For operators (goes to the 'source' object):
  #   CastValueInfo (operands are cast to a common type (implicit cast), if their types are different).
  
  Question?
  
  Mapping of MicroTESK data types to SMT-LIB data types? 

*/

public final class NodeInfo
{
    public static enum Kind
    {
        LOCATION       (Location.class),
        NAMED_CONST (LetConstant.class),
        CONST       (SourceConst.class),
        OPERATOR (SourceOperator.class);

        private final Class<?> sourceClass;

        private Kind(Class<?> sourceClass)
            { this.sourceClass = sourceClass; }

        boolean isCompatibleSource(Object source)
            { return this.sourceClass == source.getClass(); }
    }

    public static NodeInfo newLocation(Location location)
    {
        if (null == location)
            throw new NullPointerException();

        return new NodeInfo(
            NodeInfo.Kind.LOCATION, location, ValueInfo.createModel(location.getType()));
    }

    public static NodeInfo newNamedConst(LetConstant constant)
    {
        if (null == constant)
            throw new NullPointerException();

        return new NodeInfo(
            NodeInfo.Kind.NAMED_CONST, constant, constant.getExpr().getValueInfo());
    }

    private final Kind            kind;
    private final Object          source;
    private final ValueInfo       valueInfo;
    private final List<ValueInfo> coercionChain;

    private NodeInfo(
        Kind            kind,
        Object          source,
        ValueInfo       valueInfo,
        List<ValueInfo> coercionChain
        )
    {
        this.kind          = kind;
        this.source        = source;
        this.valueInfo     = valueInfo;
        this.coercionChain = coercionChain;
    }

    private NodeInfo(Kind kind, Object source, ValueInfo valueInfo)
    {
        this(kind, source, valueInfo, Collections.<ValueInfo>emptyList());
    }

    public Kind getKind()
    {
        return kind;
    }

    public Object getSource()
    {
        return source;
    }

    public ValueInfo getValueInfo()
    {
        return valueInfo;
    }

    public List<ValueInfo> getCoercionChain()
    {
        return coercionChain;
    }
}
