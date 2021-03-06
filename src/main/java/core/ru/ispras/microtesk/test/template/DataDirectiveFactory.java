/*
 * Copyright 2016-2018 ISP RAS (http://www.ispras.ru)
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

package ru.ispras.microtesk.test.template;

import ru.ispras.castle.util.Logger;
import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.util.InvariantChecks;

import ru.ispras.microtesk.model.data.Data;
import ru.ispras.microtesk.model.data.Type;
import ru.ispras.microtesk.model.memory.MemoryAllocator;
import ru.ispras.microtesk.model.memory.Section;
import ru.ispras.microtesk.options.Option;
import ru.ispras.microtesk.options.Options;
import ru.ispras.microtesk.test.GenerationAbortedException;
import ru.ispras.microtesk.utils.FormatMarker;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link DataDirectiveFactory} class is a configurable factory
 * for creating data directives.
 *
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class DataDirectiveFactory {
  private final Options options;
  private final Map<String, TypeInfo> types;
  private final int maxTypeBitSize;
  private final String spaceText;
  private final BitVector spaceData;
  private final String ztermStrText;
  private final String nztermStrText;

  private DataDirectiveFactory(
      final Options options,
      final Map<String, TypeInfo> types,
      final int maxTypeBitSize,
      final String spaceText,
      final BitVector spaceData,
      final String ztermStrText,
      final String nztermStrText) {
    InvariantChecks.checkNotNull(options);
    InvariantChecks.checkNotNull(types);

    this.options = options;
    this.types = types;
    this.maxTypeBitSize = maxTypeBitSize;
    this.spaceText = spaceText;
    this.spaceData = spaceData;
    this.ztermStrText = ztermStrText;
    this.nztermStrText = nztermStrText;
  }

  public static final class Builder {
    private final Options options;
    private final boolean isDebugPrinting;
    private final int addressableUnitBitSize;

    private final Map<String, TypeInfo> types;
    private int maxTypeBitSize;
    private String spaceText;
    private BitVector spaceData;
    private String ztermStrText;
    private String nztermStrText;

    protected Builder(final Options options, final int addressableUnitBitSize) {
      InvariantChecks.checkNotNull(options);
      InvariantChecks.checkGreaterThanZero(addressableUnitBitSize);

      this.options = options;
      this.isDebugPrinting = options.getValueAsBoolean(Option.DEBUG_PRINT);
      this.addressableUnitBitSize = addressableUnitBitSize;

      this.types = new HashMap<>();
      this.maxTypeBitSize = 0;
      this.spaceText = null;
      this.spaceData = null;
      this.ztermStrText = null;
      this.nztermStrText = null;
    }

    public void defineType(
        final String id,
        final String text,
        final String typeName,
        final int[] typeArgs,
        final String format) {
      InvariantChecks.checkNotNull(id);
      InvariantChecks.checkNotNull(text);
      InvariantChecks.checkNotNull(typeName);
      InvariantChecks.checkNotNull(typeArgs);
      InvariantChecks.checkNotNull(format);

      final Type type = Type.typeOf(typeName, typeArgs);
      debug("Defining %s as %s ('%s%s')...", type, id, text, format.isEmpty() ? "" : " " + format);

      types.put(id, new TypeInfo(type, text, format));
      maxTypeBitSize = Math.max(maxTypeBitSize, type.getBitSize());
    }

    public void defineSpace(
        final String id,
        final String text,
        final BigInteger fillWith) {
      InvariantChecks.checkNotNull(id);
      InvariantChecks.checkNotNull(text);
      InvariantChecks.checkNotNull(fillWith);

      debug("Defining space as %s ('%s') filled with %x...", id, text, fillWith);

      spaceText = text;
      spaceData = BitVector.valueOf(fillWith, addressableUnitBitSize);
    }

    public void defineAsciiString(
        final String id,
        final String text,
        final boolean zeroTerm) {
      InvariantChecks.checkNotNull(id);
      InvariantChecks.checkNotNull(text);

      debug("Defining %snull-terminated ASCII string as %s ('%s')...",
          zeroTerm ? "" : "not ", id, text);

      if (zeroTerm) {
        ztermStrText = text;
      } else {
        nztermStrText = text;
      }
    }

    public DataDirectiveFactory build() {
      return new DataDirectiveFactory(
          options,
          types,
          maxTypeBitSize,
          spaceText,
          spaceData,
          ztermStrText,
          nztermStrText
          );
    }

    private void debug(final String format, final Object... args) {
      if (isDebugPrinting) {
        Logger.debug(format, args);
      }
    }
  }

  public static final class TypeInfo {
    final Type type;
    final String text;
    final String format;
    final FormatMarker formatMarker;

    private TypeInfo(
        final Type type,
        final String text,
        final String format) {
      InvariantChecks.checkNotNull(type);
      InvariantChecks.checkNotNull(text);
      InvariantChecks.checkNotNull(format);

      this.type = type;
      this.text = text;
      this.format = format;

      if (format.isEmpty()) {
        this.formatMarker = null;
      } else {
        final List<FormatMarker> formatMarkers = FormatMarker.extractMarkers(format);
        InvariantChecks.checkTrue(formatMarkers.size() == 1,
            "For a format string a single format marker is required.");
        this.formatMarker = formatMarkers.get(0);
      }
    }
  }

  private static class Text implements DataDirective {
    private final String text;

    private Text(final String text) {
      InvariantChecks.checkNotNull(text);
      this.text = text;
    }

    @Override
    public String getText() {
      return text;
    }

    @Override
    public boolean needsIndent() {
      return true;
    }

    @Override
    public void apply(final MemoryAllocator allocator) {
      // Nothing
    }

    @Override
    public DataDirective copy() {
      return this;
    }

    @Override
    public String toString() {
      return getText();
    }
  }

  private final class Comment extends Text {
    private Comment(final String text) {
      super(text);
    }

    @Override
    public String getText() {
      final String text = super.getText();
      final String commentToken = options.getValueAsString(Option.COMMENT_TOKEN);

      return String.format(
          "%s%s%s",
          commentToken,
          text.isEmpty() || commentToken.endsWith(" ") ? "" : " ",
          text
          );
    }
  }

  private static final class Label implements DataDirective {
    private final Section section;
    private final LabelValue label;

    private Label(final Section section, final LabelValue label) {
      InvariantChecks.checkNotNull(section);
      InvariantChecks.checkNotNull(label);
      InvariantChecks.checkNotNull(label.getLabel());

      this.section = section;
      this.label = label;
    }

    @Override
    public String getText() {
      return label.getLabel().getUniqueName() + ":";
    }

    @Override
    public boolean needsIndent() {
      return false;
    }

    @Override
    public void apply(final MemoryAllocator allocator) {
      final BigInteger virtuaAddress = section.physicalToVirtual(allocator.getCurrentAddress());
      label.setAddress(virtuaAddress);
    }

    @Override
    public DataDirective copy() {
      return new Label(section, label.sharedCopy());
    }

    @Override
    public String toString() {
      return String.format("%s (%s)", getText(), label);
    }
  }

  private static final class GlobalLabel implements DataDirective {
    private final LabelValue label;

    private GlobalLabel(final LabelValue label) {
      InvariantChecks.checkNotNull(label);
      InvariantChecks.checkNotNull(label.getLabel());

      this.label = label;
    }

    @Override
    public String getText() {
      return String.format(".globl %s", label.getLabel().getUniqueName());
    }

    @Override
    public boolean needsIndent() {
      return true;
    }

    @Override
    public void apply(final MemoryAllocator allocator) {
      // Nothing
    }

    @Override
    public DataDirective copy() {
      return new GlobalLabel(label.sharedCopy());
    }

    @Override
    public String toString() {
      return getText();
    }
  }

  private final class Origin implements DataDirective {
    private final BigInteger origin;

    private Origin(final BigInteger origin) {
      InvariantChecks.checkNotNull(origin);
      InvariantChecks.checkGreaterOrEq(origin, BigInteger.ZERO);
      this.origin = origin;
    }

    @Override
    public String getText() {
      return String.format(options.getValueAsString(Option.ORIGIN_FORMAT), origin);
    }

    @Override
    public boolean needsIndent() {
      return true;
    }

    @Override
    public void apply(final MemoryAllocator allocator) {
      allocator.setOrigin(origin);
    }

    @Override
    public DataDirective copy() {
      return this;
    }

    @Override
    public String toString() {
      return getText();
    }
  }

  private final class OriginRelative implements DataDirective {
    private final BigInteger delta;
    private BigInteger origin;

    private OriginRelative(final BigInteger delta) {
      this(delta, null);
    }

    private OriginRelative(final BigInteger delta, final BigInteger origin) {
      InvariantChecks.checkNotNull(delta);
      this.delta = delta;
      this.origin = origin;
    }

    @Override
    public String getText() {
      InvariantChecks.checkNotNull(origin, "Origin is not initialized.");
      return String.format(options.getValueAsString(Option.ORIGIN_FORMAT), origin);
    }

    @Override
    public boolean needsIndent() {
      return true;
    }

    @Override
    public void apply(final MemoryAllocator allocator) {
      final BigInteger address = allocator.getCurrentAddress().add(delta);
      allocator.setCurrentAddress(address);
      origin = address.subtract(allocator.getBaseAddress());
    }

    @Override
    public DataDirective copy() {
      return new OriginRelative(delta, origin);
    }

    @Override
    public String toString() {
      return origin != null
          ? getText()
          : String.format(options.getValueAsString(Option.ORIGIN_FORMAT) + " (relative)", delta);
    }
  }

  private final class Align implements DataDirective {
    private final BigInteger alignment;
    private final BigInteger alignmentInBytes;

    private Align(final BigInteger alignment, final BigInteger alignmentInBytes) {
      InvariantChecks.checkNotNull(alignment);
      InvariantChecks.checkNotNull(alignmentInBytes);

      this.alignment = alignment;
      this.alignmentInBytes = alignmentInBytes;
    }

    @Override
    public String getText() {
      return String.format(options.getValueAsString(Option.ALIGN_FORMAT), alignment);
    }

    @Override
    public boolean needsIndent() {
      return true;
    }

    @Override
    public void apply(final MemoryAllocator allocator) {
      allocator.align(alignmentInBytes);
    }

    @Override
    public DataDirective copy() {
      return this;
    }

    @Override
    public String toString() {
      return String.format("%s %s %d bytes",
          getText(), options.getValueAsString(Option.COMMENT_TOKEN), alignmentInBytes);
    }
  }

  private final class Space implements DataDirective {
    private final int length;

    private Space(final int length) {
      InvariantChecks.checkGreaterThanZero(length);
      this.length = length;
    }

    @Override
    public String getText() {
      return String.format("%s %d", spaceText, length);
    }

    @Override
    public boolean needsIndent() {
      return true;
    }

    @Override
    public void apply(final MemoryAllocator allocator) {
      allocator.allocate(spaceData, length);
    }

    @Override
    public DataDirective copy() {
      return this;
    }

    @Override
    public String toString() {
      return getText();
    }
  }

  private final class AsciiStrings implements DataDirective {
    private final boolean zeroTerm;
    private final String[] strings;

    private AsciiStrings(
        final boolean zeroTerm,
        final String[] strings) {
      InvariantChecks.checkNotEmpty(strings);

      this.zeroTerm = zeroTerm;
      this.strings = strings;
    }

    @Override
    public String getText() {
      final StringBuilder sb = new StringBuilder(zeroTerm ? ztermStrText : nztermStrText);
      for (int index = 0; index < strings.length; index++) {
        if (index > 0) {
          sb.append(',');
        }
        sb.append(String.format(" \"%s\"", strings[index]));
      }
      return sb.toString();
    }

    @Override
    public boolean needsIndent() {
      return true;
    }

    @Override
    public void apply(final MemoryAllocator allocator) {
      for (int index = 0; index < strings.length; index++) {
        allocator.allocateAsciiString(strings[index], zeroTerm);
      }
    }

    @Override
    public DataDirective copy() {
      return this;
    }

    @Override
    public String toString() {
      return getText();
    }
  }

  private static final class DataConst implements DataDirective {
    private final String typeText;
    private final List<BitVector> values;

    private DataConst(
        final String typeText,
        final List<BitVector> values) {
      InvariantChecks.checkNotNull(typeText);
      InvariantChecks.checkNotEmpty(values);

      this.typeText = typeText;
      this.values = values;
    }

    @Override
    public String getText() {
      final StringBuilder sb = new StringBuilder(typeText);

      boolean isFirst = true;
      for (final BitVector value : values) {
        if (isFirst) {
          isFirst = false;
        } else {
          sb.append(',');
        }

        sb.append(" 0x");
        sb.append(value.toHexString());
      }

      return sb.toString();
    }

    @Override
    public boolean needsIndent() {
      return true;
    }

    @Override
    public void apply(final MemoryAllocator allocator) {
      for (final BitVector value : values) {
        allocator.allocate(value);
      }
    }

    @Override
    public DataDirective copy() {
      return this;
    }

    @Override
    public String toString() {
      return getText();
    }
  }

  private static final class DataValue implements DataDirective {
    private final TypeInfo typeInfo;
    private final List<Value> values;

    private DataValue(
        final TypeInfo typeInfo,
        final List<Value> values) {
      InvariantChecks.checkNotNull(typeInfo);
      InvariantChecks.checkNotEmpty(values);

      this.typeInfo = typeInfo;
      this.values = values;
    }

    private BitVector toBitVector(final Value value) {
      return BitVector.valueOf(value.getValue(), typeInfo.type.getBitSize());
    }

    @Override
    public String getText() {
      final StringBuilder sb = new StringBuilder(typeInfo.text);

      boolean isFirst = true;
      for (final Value value : values) {
        if (isFirst) {
          isFirst = false;
        } else {
          sb.append(',');
        }
        sb.append(' ');

        final Data data = Data.valueOf(typeInfo.type, value.getValue());
        if (typeInfo.format.isEmpty()) {
          sb.append("0x");
          sb.append(data.toHexString());
        } else if (typeInfo.formatMarker.isKind(FormatMarker.Kind.STR)) {
          sb.append(String.format(typeInfo.format, data.toString()));
        } else if (typeInfo.formatMarker.isKind(FormatMarker.Kind.HEX)) {
          sb.append(String.format(typeInfo.format, data.bigIntegerValue(false)));
        } else {
          sb.append(String.format(typeInfo.format, data.bigIntegerValue()));
        }
      }

      return sb.toString();
    }

    @Override
    public boolean needsIndent() {
      return true;
    }

    @Override
    public void apply(final MemoryAllocator allocator) {
      for (final Value value : values) {
        allocator.allocate(toBitVector(value));
      }
    }

    @Override
    public DataDirective copy() {
      final List<Value> newValues = new ArrayList<>(values.size());
      for (final Value value : values) {
        newValues.add(value.copy());
      }
      return new DataValue(typeInfo, newValues);
    }

    @Override
    public String toString() {
      return getText();
    }
  }

  public DataDirective newText(final String text) {
    return new Text(text);
  }

  public DataDirective newComment(final String text) {
    return new Comment(text);
  }

  public DataDirective newLabel(final Section section, final LabelValue label) {
    return new Label(section, label);
  }

  public DataDirective newGlobalLabel(final LabelValue label) {
    InvariantChecks.checkNotNull(label);
    return new GlobalLabel(label);
  }

  public DataDirective newOrigin(final BigInteger origin) {
    return new Origin(origin);
  }

  public DataDirective newOriginRelative(final BigInteger delta) {
    return new OriginRelative(delta);
  }

  public DataDirective newAlign(final BigInteger alignment, final BigInteger alignmentInBytes) {
    return new Align(alignment, alignmentInBytes);
  }

  public DataDirective newSpace(final int length) {
    InvariantChecks.checkNotNull(spaceText);
    InvariantChecks.checkNotNull(spaceData);
    return new Space(length);
  }

  public DataDirective newAsciiStrings(final boolean zeroTerm, final String[] strings) {
    InvariantChecks.checkTrue(zeroTerm ? ztermStrText != null : nztermStrText != null);
    return new AsciiStrings(zeroTerm, strings);
  }

  public DataDirective newData(final String typeName, final BigInteger[] values) {
    final TypeInfo typeInfo = findTypeInfo(typeName);
    return newData(typeInfo, values);
  }

  public DataDirective newData(final TypeInfo typeInfo, final BigInteger[] values) {
    InvariantChecks.checkNotNull(typeInfo);
    InvariantChecks.checkNotEmpty(values);

    final List<BitVector> valueList = new ArrayList<>(values.length);
    for (final BigInteger value : values) {
      final BitVector data = BitVector.valueOf(value, typeInfo.type.getBitSize());
      valueList.add(data);
    }

    return new DataConst(typeInfo.text, valueList);
  }

  public DataDirective newData(
      final String typeName, final DataGenerator generator, final int count) {
    final TypeInfo typeInfo = findTypeInfo(typeName);
    return newData(typeInfo, generator, count);
  }

  public DataDirective newData(
      final TypeInfo typeInfo, final DataGenerator generator, final int count) {
    InvariantChecks.checkNotNull(typeInfo);
    InvariantChecks.checkNotNull(generator);
    InvariantChecks.checkGreaterThanZero(count);

    final List<BitVector> values = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      values.add(generator.nextData());
    }

    return new DataConst(typeInfo.text, values);
  }

  public DataDirective newDataValues(final String typeName, final List<Value> values) {
    final TypeInfo typeInfo = findTypeInfo(typeName);
    return newDataValues(typeInfo, values);
  }

  public DataDirective newDataValues(final TypeInfo typeInfo, final List<Value> values) {
    return new DataValue(typeInfo, values);
  }

  public int getMaxTypeBitSize() {
    return maxTypeBitSize;
  }

  public TypeInfo findTypeInfo(final String typeName) {
    InvariantChecks.checkNotNull(typeName);
    final TypeInfo typeInfo = types.get(typeName);

    if (null == typeInfo) {
      throw new GenerationAbortedException(
          String.format("The %s data type is not defined.", typeName));
    }

    return typeInfo;
  }

  public TypeInfo findTypeInfo(final int typeSizeInBits) {
    InvariantChecks.checkGreaterThanZero(typeSizeInBits);

    for (final TypeInfo typeInfo : types.values()) {
      if (typeSizeInBits == typeInfo.type.getBitSize()) {
        return typeInfo;
      }
    }

    throw new GenerationAbortedException(
        String.format("No %d-bit type is defined.", typeSizeInBits));
  }
}
