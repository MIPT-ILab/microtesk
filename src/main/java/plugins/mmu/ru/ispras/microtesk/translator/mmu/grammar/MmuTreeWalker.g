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

tree grammar MmuTreeWalker;

//==================================================================================================
// Options
//==================================================================================================

options {
  language=Java;
  tokenVocab=MmuParser;
  ASTLabelType=CommonTree;
  superClass=MmuTreeWalkerBase;
}

@rulecatch {
catch (RecognitionException re) {
    reportError(re);
    recover(input,re);
}
}

//==================================================================================================
// Header for the Generated Java File
//==================================================================================================

@header {
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
 *
 * WARNING: THIS FILE IS AUTOMATICALLY GENERATED. PLEASE DO NOT MODIFY IT. 
 */

package ru.ispras.microtesk.translator.mmu.grammar;

import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeValue;

import ru.ispras.microtesk.translator.antlrex.Where;

import ru.ispras.microtesk.translator.mmu.MmuTreeWalkerBase;
import ru.ispras.microtesk.translator.mmu.MmuSymbolKind;
import ru.ispras.microtesk.translator.mmu.ir.Type;
import ru.ispras.microtesk.translator.mmu.ir.Stmt;
}

//==================================================================================================
// MMU Specification
//==================================================================================================

startRule 
    : declaration*
    ;

declaration
    : address
    | segment
    | buffer
    | mmu
    ;

//==================================================================================================
// Address
//==================================================================================================

address
    : ^(MMU_ADDRESS addressId=ID {declare($addressId, MmuSymbolKind.ADDRESS, false);} width=expr[0])
      {newAddress($addressId, $width.res);}
    ;

//==================================================================================================
// Segment
//==================================================================================================

segment
    : ^(MMU_SEGMENT segmentId=ID {declareAndPushSymbolScope($segmentId, MmuSymbolKind.SEGMENT);} 
        addressArgId=ID {declare($addressArgId, MmuSymbolKind.ARGUMENT, false);} addressArgType=ID
        ^(MMU_RANGE from=expr[0] to=expr[0])
      ) {newSegment($segmentId, $addressArgId, $addressArgType, $from.res, $to.res);}
    ; finally {popSymbolScope();}

//==================================================================================================
// Buffer
//==================================================================================================

buffer
    : ^(MMU_BUFFER bufferId=ID {declareAndPushSymbolScope($bufferId, MmuSymbolKind.BUFFER);}
        addressArgId=ID {declare($addressArgId, MmuSymbolKind.ARGUMENT, false);} addressArgType=ID
        (parentBufferId=ID)?
        {final BufferBuilder builder = new BufferBuilder(
             $bufferId, $addressArgId, $addressArgType, $parentBufferId);}
        (
            ^(w=MMU_WAYS ways=expr[0])   {builder.setWays($w, $ways.res);}
          | ^(w=MMU_SETS sets=expr[0])   {builder.setSets($w, $sets.res);}
          | ^(w=MMU_ENTRY e=entry)       {builder.setEntry($w, $e.res);}
          | ^(w=MMU_INDEX index=expr[0]) {builder.setIndex($w, $index.res);}
          | ^(w=MMU_MATCH match=expr[0]) {builder.setMatch($w, $match.res);}
          | ^(w=MMU_POLICY policyId=ID)  {builder.setPolicyId($w, $policyId);}
        )*
        {builder.build();}
      )
    ; finally {popSymbolScope(); resetContext();}

entry returns [Type res]
@init {final TypeBuilder builder = new TypeBuilder();}
@after {$res = builder.build();} 
    : (fieldId=ID {declare($fieldId, MmuSymbolKind.FIELD, false);}
      size=expr[0] value=expr[0]? {builder.addField($fieldId, $size.res, $value.res);})+
    ;

//==================================================================================================
// Memory
//==================================================================================================

mmu
    : ^(MMU memoryId=ID {declareAndPushSymbolScope($memoryId, MmuSymbolKind.MEMORY);}
        addressArgId=ID {declare($addressArgId, MmuSymbolKind.ARGUMENT, false);} addressArgType=ID
        dataArgId=ID {declare($dataArgId, MmuSymbolKind.DATA, false);} dataArgSize=expr[0]
        {final MemoryBuilder builder = newMemoryBuilder($memoryId, $addressArgId, $addressArgType, $dataArgId, $dataArgSize.res);}
        (^(MMU_VAR varId=ID  {declare($varId, MmuSymbolKind.VAR, false);}(
             bufferId=ID     {builder.addVariable($varId, $bufferId);}
           | varSize=expr[0] {builder.addVariable($varId, $varSize.res);})
        ))*
        (attrId=ID {declare($attrId, MmuSymbolKind.ATTRIBUTE, false);}
         stmts=sequence {builder.addAttribute($attrId, $stmts.res);})*
        {builder.build();}
      )
    ; finally {popSymbolScope(); resetContext();}

//==================================================================================================
// Statements
//==================================================================================================

sequence returns [List<Stmt> res]
@init  {final List<Stmt> stmts = new ArrayList<>();}
@after {$res = stmts;}
    : ^(SEQUENCE (stmt=statement {
checkNotNull($stmt.start, $stmt.res); 
stmts.add($stmt.res);
})*)
    ;

statement returns [Stmt res]
@after {$res = $stmt.res;}
    : stmt=attributeCallStmt
    | stmt=assignmentStmt
    | stmt=conditionalStmt
    | stmt=functionCallStmt
    ;

attributeCallStmt returns [Stmt res]
    : ID
    | ^(DOT ID ID)
    | attributeRef[false]
    ;

assignmentStmt returns [Stmt res]
    : ^(w=ASSIGN lhs=variable[true] rhs=expr[0])
       {$res = newAssignment($w, $lhs.res, $rhs.res);}
    ;

conditionalStmt returns [Stmt res]
@after {$res = $stmt.res;}
    : stmt=ifStmt
    ;

ifStmt returns [Stmt res]
    : ^(wif=IF cif=expr[0] sif=sequence 
       {final IfBuilder builder = new IfBuilder($wif, $cif.res, $sif.res);}
       (^(welif=ELSEIF celif=expr[0] selif=sequence)
       {builder.addElseIf($welif, $celif.res, $selif.res);})* 
       (^(welse=ELSE selse=sequence) {builder.setElse($welse, $selse.res);})? )
       {$res=builder.build();}
    ;

functionCallStmt returns [Stmt res]
    :  ^(TRACE fs=STRING_CONST {final List<Node> fargs = new ArrayList<>();}
        (farg=expr[0] {checkNotNull($farg.start, $farg.res); fargs.add($farg.res);})*)
        {$res = newTrace($fs, fargs);}
    |  ^(EXCEPTION s=STRING_CONST) {$res = newException($s);}
    ;

//==================================================================================================
// Attribute Reference
//==================================================================================================

attributeRef [boolean isLhs] returns [Node res]
@init {final List<Node> args = new ArrayList<>();}
    : ^(INSTANCE_CALL ^(INSTANCE id=ID (arg=expr[0]{args.add($arg.res);})*) attrId=ID?)	
      {$res = newAttributeRef($id, isLhs, args, $attrId);}
    ;

//==================================================================================================
// Expressions
//==================================================================================================

expr [int depth] returns [Node res]
@after {
$res = n;
}
    : n=atom
    | n=binaryExpr[depth+1]
    | n=unaryExpr[depth+1]
//  | ifExpr[depth+1]
    ;

ifExpr [int depth] returns [Node res]
    : ^(IF expr[depth] expr[depth] elseIfExpr[depth]* elseExpr[depth]?)
    ;

elseIfExpr [int depth] returns [Node res]
    : ^(ELSEIF expr[depth] expr[depth])
    ;

elseExpr [int depth] returns [Node res]
    : ^(ELSE expr[depth])
    ;

binaryExpr [int depth] returns [Node res]
@after {
$res = newExpression($op, $e1.res, $e2.res);
}
    : ^(op=OR            e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=AND           e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=VERT_BAR      e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=UP_ARROW      e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=AMPER         e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=EQ            e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=NEQ           e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=LEQ           e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=GEQ           e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=LEFT_BROCKET  e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=RIGHT_BROCKET e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=LEFT_SHIFT    e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=RIGHT_SHIFT   e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=ROTATE_LEFT   e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=ROTATE_RIGHT  e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=PLUS          e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=MINUS         e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=MUL           e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=DIV           e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=REM           e1=expr[depth+1] e2=expr[depth+1])
    | ^(op=DOUBLE_STAR   e1=expr[depth+1] e2=expr[depth+1])
    ;

unaryExpr [int depth] returns [Node res]
@after {
$res = newExpression($op, $e.res);
}
    : ^(op=UPLUS  e=expr[depth+1])
    | ^(op=UMINUS e=expr[depth+1])
    | ^(op=TILDE  e=expr[depth+1])
    | ^(op=NOT    e=expr[depth+1])
    ;

atom returns [Node res]
@after {
$res = n;
}
    : n=constant 
    | n=variable[false]
    ;

//==================================================================================================
// Constant    
//==================================================================================================

constant returns [Node res]
@init {
int radix = 10;
}
@after {
$res = NodeValue.newInteger($t.text, radix);
}
    : t=CARD_CONST   { radix = 10; }
    | t=BINARY_CONST { radix = 2; }
    | t=HEX_CONST    { radix = 16; }
    ;

//==================================================================================================
// Variable
//==================================================================================================

variable [boolean isLhs] returns [Node res]
    : ^(LOCATION v=variableConcat[isLhs, 0]) {$res=$v.res;}
    ;

variableConcat [boolean isLhs, int depth] returns [Node res]
    : ^(DOUBLE_COLON l=variableBitfield[isLhs] r=variableConcat[isLhs, depth+1])
      {$res=newConcat($l.start, $l.res, $r.res);}
    | vb=variableBitfield[isLhs] {$res=$vb.res;}
    ;

variableBitfield [boolean isLhs] returns [Node res]
    : ^(LOCATION_BITFIELD va=variableAtom[isLhs] from=expr[0] to=expr[0]?)
      {$res = newBitfield($va.start, $va.res, $from.res, $to.res);}
    | va=variableAtom[isLhs] {$res=$va.res;}
    ;

variableAtom [boolean isLhs] returns [Node res]
    : varId=ID {$res = newVariable($varId);}
    | ^(DOT objId=ID attrId=ID) {$res=newAttributeCall($objId, $attrId);}
    | ^(LOCATION_INDEX varId=ID index=expr[0]) {$res = newIndexedVariable($varId, $index.res);}
    | atr=attributeRef[isLhs] {$res = $atr.res;}
    ;

//==================================================================================================
// The End
//==================================================================================================
