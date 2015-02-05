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

package ru.ispras.microtesk.translator.mmu;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.TreeNodeStream;

import ru.ispras.fortress.data.Data;
import ru.ispras.fortress.data.DataType;
import ru.ispras.fortress.data.DataTypeId;
import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.expression.Node;
import ru.ispras.fortress.expression.NodeOperation;
import ru.ispras.fortress.expression.NodeValue;
import ru.ispras.fortress.expression.StandardOperation;
import ru.ispras.fortress.transformer.ReduceOptions;
import ru.ispras.fortress.transformer.Transformer;

import ru.ispras.microtesk.model.api.mmu.PolicyId;
import ru.ispras.microtesk.translator.antlrex.SemanticException;
import ru.ispras.microtesk.translator.antlrex.TreeParserBase;
import ru.ispras.microtesk.translator.antlrex.Where;
import ru.ispras.microtesk.translator.mmu.ir.Address;
import ru.ispras.microtesk.translator.mmu.ir.Buffer;
import ru.ispras.microtesk.translator.mmu.ir.Entry;
import ru.ispras.microtesk.translator.mmu.ir.Field;
import ru.ispras.microtesk.translator.mmu.ir.Ir;
import ru.ispras.microtesk.translator.mmu.ir.Memory;
import ru.ispras.microtesk.translator.mmu.ir.Segment;

public abstract class MmuTreeWalkerBase extends TreeParserBase {
  private Ir ir;

  public MmuTreeWalkerBase(TreeNodeStream input, RecognizerSharedState state) {
    super(input, state);
    this.ir = null;
  }

  public final void assignIR(Ir ir) {
    this.ir = ir;
  }

  public final Ir getIR() {
    return ir;
  }

  /**
   * Creates an Address IR object and adds it to the MMU IR.
   * 
   * @param addressId Address identifier.
   * @param widthExpr Address width expression.
   * @return New Address IR object.
   * @throws SemanticException (1) if the width expression is {@code null}; (2) if the width
   * expression cannot be reduced to a constant integer value; (3) if the width value is beyond
   * the Java Integer allowed range; (4) if the width value is less or equal 0. 
   */

  protected final Address newAddress(
      CommonTree addressId, Node widthExpr) throws SemanticException {

    checkNotNull(addressId, widthExpr);

    final Where w = where(addressId);
    final int value = extractPositiveInt(w, widthExpr, "Address width");

    final Address address = new Address(addressId.getText(), value);
    ir.addAddress(address);

    return address;
  }

  /**
   * Creates a segment IR object and adds it to the MMU IR.
   * 
   * @param segmentId Segment identifier.
   * @param addressArgId Address argument identifier. 
   * @param addressArgType Address argument type (identifier).
   * @param rangeStartExpr Range start expression.
   * @param rangeEndExpr Range and expression.
   * @return New Segment IR object.
   * @throws SemanticException (1) if the specified address type is not defined;
   * (2) if the range expressions equal to {@code null}, (3) if the range expressions
   * cannot be reduced to constant integer values; (4) if the range start
   * value is greater than the range end value.
   */

  protected final Segment newSegment(
      CommonTree segmentId,
      CommonTree addressArgId,
      CommonTree addressArgType,
      Node rangeStartExpr,
      Node rangeEndExpr) throws SemanticException {

    checkNotNull(segmentId, rangeStartExpr);
    checkNotNull(segmentId, rangeEndExpr);

    final Where w = where(segmentId);
    final Address address = getAddress(w, addressArgType.getText());

    final BigInteger rangeStart = extractBigInteger(w, rangeStartExpr, "Range start");
    final BigInteger rangeEnd = extractBigInteger(w, rangeEndExpr, "Range end");

    if (rangeStart.compareTo(rangeEnd) > 0) {
      raiseError(w, String.format(
          "Range start (%d) is greater than range end (%d).", rangeStart, rangeEnd));
    }

    final Segment segment = new Segment(
        segmentId.getText(),
        addressArgId.getText(),
        address,
        BitVector.valueOf(rangeStart, address.getWidth()),
        BitVector.valueOf(rangeEnd, address.getWidth())
        );

    ir.addSegment(segment);
    return segment;
  }

  /**
   * Creates a builder for an Entry object.
   * @return Entry builder.
   */

  protected final EntryBuilder newEntryBuilder() {
    return new EntryBuilder();
  }

  /**
   * Builder for an Entry. Helps create an Entry from a sequence of Fields. 
   */

  protected final class EntryBuilder {
    private int currentPos;
    private Map<String, Field> fields;

    private EntryBuilder() {
      this.currentPos = 0;
      this.fields = new LinkedHashMap<>();
    }

    /**
     * Adds a field to Entry to be created.
     * 
     * @param fieldId Field identifier.
     * @param sizeExpr Field size expression.
     * @param valueExpr Field default value expression (optional, can be {@code null}).
     * @throws SemanticException (1) if the size expression is {@code null}, (2) if
     * the size expression cannot be evaluated to a positive integer value (Java int).
     */

    public void addField(CommonTree fieldId, Node sizeExpr, Node valueExpr) throws SemanticException {
      checkNotNull(fieldId, sizeExpr);
      
      final Where w = where(fieldId);
      final String id = fieldId.getText();
 
      final int bitSize = extractPositiveInt(w, sizeExpr, id + " field size");

      BitVector defValue = null;
      if (null != valueExpr) {
        final BigInteger value = extractBigInteger(w, valueExpr, id + " field value");
        defValue = BitVector.valueOf(value, bitSize);
      }

      final Field field = new Field(id, currentPos, bitSize, defValue);
      currentPos += bitSize;

      fields.put(field.getId(), field);
    }

    /**
     * Builds an Entry from the collection of fields.
     * @return New Entry.
     */

    public Entry build() {
      return fields.isEmpty() ? Entry.EMPTY : new Entry(fields);
    }
  }

  /**
   * Creates a builder for a Buffer object.
   *    
   * @param bufferId Buffer identifier.
   * @param addressArgId Address argument identifier. 
   * @param addressArgType Address argument type (identifier).
   * @return New BufferBulder object.
   * @throws SemanticException if the specified address type is not defined.
   */

  protected final BufferBuilder newBufferBuilder(
      CommonTree bufferId,
      CommonTree addressArgId,
      CommonTree addressArgType) throws SemanticException {

    final Where w = where(bufferId);
    final Address address = getAddress(w, addressArgType.getText());
    return new BufferBuilder(w, bufferId.getText(), addressArgId.getText(), address);
  }

  //////////////////////////////////////////////////////////////////////////////
  // TODO: Review + comments are needed

  /**
   * Builder for Builder objects. Helps create a Buffer from attributes.
   */

  protected final class BufferBuilder {
    private final Where where;
    
    private final String id;
    private final String addressArgId;
    private final Address addressArgType;

    private int ways;
    private int sets;
    private Entry entry;
    private Node index;
    private Node match;
    private PolicyId policy;

    private BufferBuilder(Where where, String id, String addressArgId, Address addressArgType) {
      this.where = where;

      this.id = id;
      this.addressArgId = addressArgId;
      this.addressArgType = addressArgType;
      
      this.ways = 0;
      this.sets = 0;
      this.entry = null;
      this.index = null;
      this.match = null;
      this.policy = null;
    }

    private void checkRedefined(CommonTree attrId, boolean isRedefined) throws SemanticException {
      if (isRedefined) {
        raiseError(where(attrId),
            String.format("The %s attribute is redefined.", attrId.getText()));
      }
    }

    private void checkUndefined(String attrId, boolean isUndefined) throws SemanticException {
      if (isUndefined) {
        raiseError(where, String.format("The %s attribute is undefined.", attrId));
      }
    }

    public void setWays(CommonTree attrId, Node attr) throws SemanticException {
      checkNotNull(attrId, attr);
      checkRedefined(attrId, ways != 0);
      ways = extractPositiveInt(where(attrId), attr, attrId.getText());
    }

    public void setSets(CommonTree attrId, Node attr) throws SemanticException {
      checkNotNull(attrId, attr);
      checkRedefined(attrId, sets != 0);
      sets = extractPositiveInt(where(attrId), attr, attrId.getText());
    }

    public void setEntry(CommonTree attrId, Entry attr) throws SemanticException {
      checkNotNull(attrId, attr);
      checkRedefined(attrId, entry != null);
      entry = attr;
    }

    public void setIndex(Node node) {

    }

    public void setMatch(Node node) {

    }

    public void setPolicyId(CommonTree attrId, CommonTree attr) throws SemanticException {
      checkRedefined(attrId, policy != null);
      try {
        final PolicyId value = PolicyId.valueOf(attr.getText());
        policy = value;
      } catch (Exception e) {
        raiseError(where(attr), "Unknown policy: " + attr.getText()); 
      }
    }

    public Buffer build() throws SemanticException {
      checkUndefined("ways", ways == 0);
      checkUndefined("sets", sets == 0);
      checkUndefined("entry", entry == null); 

      //checkUndefined("index", index == null);
      //checkUndefined("match", match == null);

      if (null == policy) {
        policy = PolicyId.NONE;
      }

      final Buffer buffer = new Buffer(
          id, addressArgId, addressArgType, ways, sets, entry, index, match, policy);

      ir.addBuffer(buffer);
      return buffer;
    }
  }

  protected final MemoryBuilder newMemoryBuilder(
      CommonTree memoryId,
      CommonTree addressArgId,
      CommonTree addressArgType,
      CommonTree dataArgId) throws SemanticException {
    
    final Where w = where(memoryId);
    final Address address = getAddress(w, addressArgType.getText());

    return new MemoryBuilder(w, memoryId.getText(), addressArgId.getText(), address);
  }

  protected final class MemoryBuilder {
    private final Where where;

    private final String id;
    private final String addressArgId;
    private final Address addressArgType;

    private MemoryBuilder(
        Where where, String id, String addressArgId, Address addressArgType) {
      this.where = where;
      this.id = id;
      this.addressArgId = addressArgId;
      this.addressArgType = addressArgType;
    }

    public Memory build() {
      final Memory memory = new Memory(id, addressArgId, addressArgType);
      ir.addMemory(memory);
      return memory;
    }
  }
 
  /**
   * Creates a new operator-based expression. Works in the following steps:
   * 
   * <ol><li>Find Fortress operator</li>
   * <li>Reduce all operands</li>
   * <li>Cast all value operands to common type (bit vector) if required</li>
   * <li>Make NodeOperation and return</li></ol>
   * 
   * @param operatorId Operator identifier.
   * @param operands Array of operands. 
   * @return
   * @throws RecognitionException
   */

  public Node newExpression(CommonTree operatorId, Node ... operands) throws RecognitionException {
    final String ERR_NO_OPERATOR = "The %s operator is not supported.";
    final String ERR_NO_OPERATOR_FOR_TYPE = "The %s operator is not supported for the %s type.";

    final Operator op = Operator.fromText(operatorId.getText());
    final Where w = where(operatorId);
    
    if (null == op) {
      raiseError(w, String.format(ERR_NO_OPERATOR, operatorId.getText()));
    }

    final DataType firstOpType = operands[0].getDataType();
    DataType type = firstOpType;

    final Node[] reducedOperands = new Node[operands.length];
    for (int i = 0; i < operands.length; i++) {
      final Node operand = Transformer.reduce(ReduceOptions.NEW_INSTANCE, operands[i]);
      final DataType currentType = operand.getDataType(); 

      // Size is always greater for bit vectors.
      if (currentType.getSize() > type.getSize()) { 
        type = currentType;
      }

      reducedOperands[i] = operand;
    }

    if (type != firstOpType && type.getTypeId() == DataTypeId.BIT_VECTOR) {
      for (int i = 0; i < reducedOperands.length; i++) {
        final Node operand = reducedOperands[i];
        if ((operand instanceof NodeValue) && !type.equals(operand.getDataType())) {
          final BigInteger value = ((NodeValue) operand).getInteger();
          reducedOperands[i] = new NodeValue(Data.newBitVector(value, type.getSize()));
        }
      }
    }

    final StandardOperation fortressOp = op.toFortressFor(type.getTypeId());
    if (null == fortressOp) {
      raiseError(w, String.format(ERR_NO_OPERATOR_FOR_TYPE, operatorId.getText(), type));
    }

    return new NodeOperation(fortressOp, reducedOperands);
  }

  public Node newVariable(String id) {
    return null;
  }

  public Node newIndexedVariable(String id, Node index) {
    return null;
  }
  
  public Node newAttributeCall(String id, String attributeId) {
    return null;
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Utility Methods
  //////////////////////////////////////////////////////////////////////////////////////////////////

  private Address getAddress(Where w, String addressId) throws SemanticException {
    final Address address = ir.getAddresses().get(addressId);
    if (null == address) {
      raiseError(w, String.format("%s is not defined or is not an address.", addressId));
    }

    return address;
  }

  private BigInteger extractBigInteger(
      Where w, Node expr, String exprDesc) throws SemanticException {

    if (expr.getKind() != Node.Kind.VALUE || !expr.isType(DataTypeId.LOGIC_INTEGER)) {
      raiseError(w, String.format("%s is not a constant integer expression.", exprDesc)); 
    }

    final NodeValue nodeValue = (NodeValue) expr;
    return nodeValue.getInteger();
  }

  private int extractInt(
      Where w, Node expr, String exprDesc) throws SemanticException {

    final BigInteger value = extractBigInteger(w, expr, exprDesc);
    if (value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0 ||
        value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
      raiseError(w, String.format(
          "%s (=%d) is beyond the allowed integer value range.", exprDesc, value)); 
    }

    return value.intValue();
  }

  private int extractPositiveInt(
      Where w, Node expr, String nodeName) throws SemanticException {

    final int value = extractInt(w, expr, nodeName);
    if (value <= 0) {
      raiseError(w, String.format("%s (%d) must be > 0.", nodeName, value));
    }

    return value;
  }
}
