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

parser grammar MmuParser;

//==================================================================================================
// Options
//==================================================================================================

options {
  language=Java;
  tokenVocab=MmuLexer;
  output=AST;
  superClass=ParserBase;
  backtrack=true;
}

import CommonParser;

//==================================================================================================
// Header for the Generated Java File
//==================================================================================================

@header {
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
 *
 * WARNING: THIS FILE IS AUTOMATICALLY GENERATED. PLEASE DO NOT MODIFY IT. 
 */

package ru.ispras.microtesk.translator.mmu.grammar;

import ru.ispras.microtesk.translator.antlrex.SemanticException;
import ru.ispras.microtesk.translator.antlrex.ParserBase;
import ru.ispras.microtesk.translator.simnml.ESymbolKind;
}

//==================================================================================================
// MMU Specification
//==================================================================================================

startRule 
    : bufferOrAddress* EOF!
    ;

bufferOrAddress
    : address
    | buffer
    ;

//==================================================================================================
// Address
//==================================================================================================

address
    : MMU_ADDRESS^ ID LEFT_BRACE!
        (addressParameter SEMI!)*
      RIGHT_BRACE!
    ;

addressParameter
    : width
    | format
    ;

//--------------------------------------------------------------------------------------------------

width
    : MMU_WIDTH! ASSIGN! expr
    ;

//==================================================================================================
// Buffer
//==================================================================================================

buffer
    : MMU_BUFFER^ ID LEFT_BRACE!
        (bufferParameter SEMI!)*
      RIGHT_BRACE!
    ;

bufferParameter
    : ways
    | sets
    | format
    | index
    | match
    | policy
    ;

//--------------------------------------------------------------------------------------------------

ways
    : MMU_WAYS^ ASSIGN! expr
    ;

//--------------------------------------------------------------------------------------------------

sets
    : MMU_SETS^ ASSIGN! expr
    ;

//--------------------------------------------------------------------------------------------------

format
    : MMU_FORMAT^ ASSIGN! LEFT_PARENTH!
        field (COMA! field)*
      RIGHT_PARENTH!
    ;

field
    : ID COLON! expr (ASSIGN! expr)?
    ;

//--------------------------------------------------------------------------------------------------

index
    : MMU_INDEX^ LEFT_PARENTH! addressArg=ID COLON! addressType=ID RIGHT_PARENTH!
        ASSIGN! expr
    ;

//--------------------------------------------------------------------------------------------------

match
    : MMU_MATCH^ LEFT_PARENTH! addressArg=ID COLON! addressType=ID RIGHT_PARENTH!
        ASSIGN! expr
    ;

//--------------------------------------------------------------------------------------------------

policy
    : MMU_POLICY^ ASSIGN! ID
    ;

//==================================================================================================
// The End
//==================================================================================================
