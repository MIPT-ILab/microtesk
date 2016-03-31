/*
 * Copyright 2014-2015 ISP RAS (http://www.ispras.ru)
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

import static ru.ispras.fortress.util.InvariantChecks.checkNotNull;
import static ru.ispras.fortress.util.InvariantChecks.checkTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.ispras.fortress.data.types.bitvector.BitVector;
import ru.ispras.fortress.randomizer.Variate;
import ru.ispras.fortress.randomizer.VariateBuilder;
import ru.ispras.fortress.randomizer.VariateSingleValue;
import ru.ispras.microtesk.Logger;
import ru.ispras.microtesk.model.api.metadata.MetaAddressingMode;
import ru.ispras.microtesk.model.api.metadata.MetaData;
import ru.ispras.microtesk.model.api.metadata.MetaGroup;
import ru.ispras.microtesk.model.api.metadata.MetaModel;
import ru.ispras.microtesk.model.api.metadata.MetaOperation;
import ru.ispras.microtesk.test.GenerationAbortedException;
import ru.ispras.microtesk.test.LabelManager;
import ru.ispras.microtesk.test.sequence.engine.EngineContext;
import ru.ispras.microtesk.test.sequence.engine.allocator.Allocator;

public final class Template {

  public static enum Section {
    PRE,
    POST,
    MAIN
  }

  public interface Processor {
    void defineExceptionHandler(ExceptionHandler handler);
    void process(Section section, Block block);
    void finish();
  }

  private final EngineContext context;

  private final MetaModel metaModel;
  private final DataManager dataManager;
  private final PreparatorStore preparators;
  private final BufferPreparatorStore bufferPreparators;
  private final StreamStore streams;
  private final Processor processor;

  // Variates for mode and operation groups 
  private final Map<String, Variate<String>> groupVariates;

  // Default situations for instructions and groups
  private final Map<String, Variate<Situation>> defaultSituations;

  private PreparatorBuilder preparatorBuilder;
  private BufferPreparatorBuilder bufferPreparatorBuilder;
  private StreamPreparatorBuilder streamPreparatorBuilder;
  private ExceptionHandlerBuilder exceptionHandlerBuilder;
  private boolean isExceptionHandlerDefined;

  private Deque<BlockBuilder> blockBuilders;
  private CallBuilder callBuilder;

  private boolean isMainSection;
  private int openBlockCount;

  // Test case level prologue and epilogue
  private List<Call> prologueBuilder;
  private List<Call> epilogueBuilder;

  private List<Call> globalPrologue;
  private List<Call> globalEpilogue;

  private final Set<Block> unusedBlocks;

  public Template(
      final EngineContext context,
      final MetaModel metaModel,
      final DataManager dataManager,
      final PreparatorStore preparators,
      final BufferPreparatorStore bufferPreparators,
      final StreamStore streams,
      final Processor processor) {

    Logger.debugHeader("Started Processing Template");

    checkNotNull(context);
    checkNotNull(metaModel);
    checkNotNull(dataManager);
    checkNotNull(preparators);
    checkNotNull(bufferPreparators);
    checkNotNull(streams);
    checkNotNull(processor);

    this.context = context;

    this.metaModel = metaModel;
    this.dataManager = dataManager;
    this.preparators = preparators;
    this.bufferPreparators = bufferPreparators;
    this.streams = streams;
    this.processor = processor;

    this.preparatorBuilder = null;
    this.bufferPreparatorBuilder = null;
    this.streamPreparatorBuilder = null;
    this.exceptionHandlerBuilder = null;
    this.isExceptionHandlerDefined = false;

    this.blockBuilders = null;
    this.callBuilder = null;

    this.isMainSection = false;
    this.openBlockCount = 0;

    this.groupVariates = newVariatesForGroups(metaModel);
    this.defaultSituations = new HashMap<>();

    this.prologueBuilder = null;
    this.epilogueBuilder = null;

    this.globalPrologue = null;
    this.globalEpilogue = null;

    this.unusedBlocks = new LinkedHashSet<>();
  }

  private void processBlock(final Section section, final Block block) {
    processor.process(section, block);
  }

  public DataManager getDataManager() {
    return dataManager;
  }

  public Processor getProcessor() {
    return processor;
  }

  public Set<Block> getUnusedBlocks() {
    return Collections.unmodifiableSet(unusedBlocks);
  }

  public BigInteger getAddressForLabel(final String label) {
    final LabelManager.Target target =
        dataManager.getGlobalLabels().resolve(new Label(label, getCurrentBlockId()));

    if (null == target) {
      throw new GenerationAbortedException(
          String.format("The %s label is not defined.", label));
    }

    return BigInteger.valueOf(target.getAddress());
  }

  public void beginPreSection() {
    Logger.debugHeader("Started Processing Initialization Section");
    beginNewSection();

    isMainSection = false;
    openBlockCount = 0;
  }

  public void endPreSection() {
    final Block rootBlock = endCurrentSection().build();
    processBlock(Section.PRE, rootBlock);
    Logger.debugHeader("Ended Processing Initialization Section");
  }

  public void beginPostSection() {
    Logger.debugHeader("Started Processing Finalization Section");
    beginNewSection();

    isMainSection = false;
    openBlockCount = 0;
  }

  public void endPostSection() {
    final Block rootBlock = endCurrentSection().build();
    processBlock(Section.POST, rootBlock);
    Logger.debugHeader("Ended Processing Finalization Section");
  }

  public void beginMainSection() {
    Logger.debugHeader("Started Processing Main Section");
    beginNewSection();

    isMainSection = true;
    openBlockCount = 0;
  }

  public void endMainSection() {
    final BlockBuilder rootBlockBuilder = endCurrentSection();
    if (!rootBlockBuilder.isEmpty()) {
      final Block rootBlock = rootBlockBuilder.build(globalPrologue, globalEpilogue);
      processBlock(Section.MAIN, rootBlock);
    }

    Logger.debugHeader("Ended Processing Main Section");
    isMainSection = false;
  }

  private void beginNewSection() {
    final BlockBuilder rootBlockBuilder = new BlockBuilder(false);
    rootBlockBuilder.setSequence(true);

    this.blockBuilders = new LinkedList<>();
    this.blockBuilders.push(rootBlockBuilder);
    this.callBuilder = new CallBuilder(getCurrentBlockId());
  }

  private BlockBuilder endCurrentSection() {
    endBuildingCall();

    if (blockBuilders.size() != 1) {
      throw new IllegalStateException();
    }

    final BlockBuilder rootBuilder = blockBuilders.peek();

    blockBuilders = null;
    callBuilder = null;

    return rootBuilder;
  }

  private BlockId getCurrentBlockId() {
    return blockBuilders.peek().getBlockId();
  }

  public BlockBuilder beginBlock() {
    if (blockBuilders.isEmpty()) {
      throw new IllegalStateException();
    }

    if (openBlockCount < 0) {
      throw new IllegalStateException();
    }

    endBuildingCall();

    final boolean isRoot = openBlockCount == 0;
    final BlockBuilder parent = blockBuilders.peek();
    final BlockBuilder current;

    if (isMainSection && isRoot) {
      if (!parent.isEmpty()) {
        processBlock(Section.MAIN, parent.build(globalPrologue, globalEpilogue));
      }

      blockBuilders.pop();
      current = new BlockBuilder(true);
      blockBuilders.push(current);
    } else {
      current = new BlockBuilder(parent);
      blockBuilders.push(current);
    }

    Logger.debug("Begin block: " + getCurrentBlockId());
    ++openBlockCount;

    return current;
  }

  public BlockHolder endBlock() {
    if (blockBuilders.isEmpty()) {
      throw new IllegalStateException();
    }

    endBuildingCall();
    Logger.debug("End block: " + getCurrentBlockId());

    final boolean isRoot = openBlockCount == 1;

    final BlockBuilder builder = blockBuilders.pop();
    final Block block;

    if (isRoot) {
      // A root block is just returned to the caller.
      // Then a new root block builder is created and pushed to the stack.
      block = builder.build(globalPrologue, globalEpilogue);

      final BlockBuilder newBuilder = new BlockBuilder(false);
      newBuilder.setSequence(true);
      blockBuilders.push(newBuilder);
    } else {
      // A non-root block is added to its parent.
      block = builder.build();
      blockBuilders.peek().addBlock(block);
    }

    --openBlockCount;
    return new BlockHolder(block);
  }

  public final class BlockHolder {
    private final Block block;
    private boolean isAddedToUnused;

    private BlockHolder(final Block block) {
      checkNotNull(block);
      this.block = block;

      if (block.getBlockId().isRoot()) {
        unusedBlocks.add(block);
        this.isAddedToUnused = true;
      } else {
        this.isAddedToUnused = false;
      }
    }

    public BlockHolder add() {
      blockBuilders.peek().addBlock(block);

      if (isAddedToUnused) {
        unusedBlocks.remove(block);
        isAddedToUnused = false;
      }

      return this;
    }

    public BlockHolder add(final int times) {
      for (int index = 0; index < times; index++) {
        add();
      }
      return this;
    }

    public BlockHolder run() {
      if (!isMainSection) {
        throw new GenerationAbortedException(
            "A block can be run only in the main section of a test template.");
      }

      if (!block.getBlockId().isRoot()) {
        throw new GenerationAbortedException(String.format(
            "Running nested blocks is not allowed. At: %s", block.getWhere()));
      }

      processBlock(Section.MAIN, block);

      if (isAddedToUnused) {
        unusedBlocks.remove(block);
        isAddedToUnused = false;
      }

      return this;
    }

    public BlockHolder run(final int times) {
      for (int index = 0; index < times; index++) {
        run();
      }
      return this;
    }
  }

  public void addLabel(final String name) {
    final Label label = new Label(name, getCurrentBlockId());
    Logger.debug("Label: " + label.toString());
    callBuilder.addLabel(label);
  }

  public void addOutput(final Output output) {
    Logger.debug(output.toString());
    callBuilder.addOutput(output);
  }

  public void setCallText(final String text) {
    callBuilder.setText(text);
  }

  public void setRootOperation(final Primitive rootOperation) {
    callBuilder.setRootOperation(rootOperation);
  }

  public void endBuildingCall() {
    final Call call = callBuilder.build();
    Logger.debug("Ended building a call (empty = %b, executable = %b)",
        call.isEmpty(), call.isExecutable());

    addCall(call);
    this.callBuilder = new CallBuilder(getCurrentBlockId());
  }

  private void addCall(final Call call) {
    checkNotNull(call);

    if (!call.isEmpty()) {
      if (null != preparatorBuilder) {
        preparatorBuilder.addCall(call);
      } else if (null != bufferPreparatorBuilder) {
        bufferPreparatorBuilder.addCall(call);
      } else if (null != streamPreparatorBuilder) {
        streamPreparatorBuilder.addCall(call);
      } else if (null != exceptionHandlerBuilder) {
        exceptionHandlerBuilder.addCall(call);
      } else if (null != prologueBuilder) {
        prologueBuilder.add(call);
      } else if (null != epilogueBuilder) {
        epilogueBuilder.add(call);
      } else {
        blockBuilders.peek().addCall(call);
      }
    }
  }

  public PrimitiveBuilder newOperationBuilder(final String name) {
    Logger.debug("Operation: " + name);
    checkNotNull(name);

    return new PrimitiveBuilderOperation(name, metaModel, callBuilder);
  }

  public PrimitiveBuilder newAddressingModeBuilder(final String name) {
    Logger.debug("Addressing mode: " + name);
    checkNotNull(name);

    final MetaAddressingMode metaData = metaModel.getAddressingMode(name);
    if (null == metaData) {
      throw new IllegalArgumentException("No such addressing mode: " + name);
    }

    return new PrimitiveBuilderCommon(metaModel, callBuilder, metaData);
  }

  public RandomValue newRandom(final BigInteger from, final BigInteger to) {
    return new RandomValue(from, to);
  }

  public VariateBuilder<?> newVariateBuilder() {
    return new VariateBuilder<>();
  }

  public AllocatorBuilder newAllocatorBuilder(final String strategy) {
    return new AllocatorBuilder(strategy);
  }

  public void freeAllocatedMode(final Primitive mode) {
    if (null == mode) {
      return;
    }

    if (mode.getKind() != Primitive.Kind.MODE) {
      throw new GenerationAbortedException(
          mode.getName() + " is not an addressing mode.");
    }

    addCall(Call.newFreeAllocatedMode(mode));
  }

  public UnknownImmediateValue newUnknownImmediate(final Allocator allocator) {
    return new UnknownImmediateValue(allocator);
  }

  public OutputBuilder newOutput(boolean isRuntime, boolean isComment, String format) {
    return new OutputBuilder(isRuntime, isComment, format);
  }

  public Situation.Builder newSituation(String name) {
    return new Situation.Builder(name);
  }

  public void setDefaultSituation(final String name, final Situation situation) {
    checkNotNull(situation);
    setDefaultSituation(name, new VariateSingleValue<>(situation));
  }

  public void setDefaultSituation(final String name, final Variate<Situation> situation) {
    checkNotNull(name);
    checkNotNull(situation);
    defaultSituations.put(name, situation);
  }

  public Variate<Situation> getDefaultSituation(final String name) {
    checkNotNull(name);
    return defaultSituations.get(name);
  }

  public PreparatorBuilder beginPreparator(
      final String targetName, final boolean isComparator) {
    endBuildingCall();

    Logger.debug("Begin preparator: %s", targetName);
    checkNotNull(targetName);

    if (null != preparatorBuilder) {
      throw new IllegalStateException(String.format(
          "Nesting is not allowed: The %s block cannot be nested into the %s block.",
          targetName, preparatorBuilder.getTargetName()));
    }

    checkTrue(null == preparatorBuilder);
    checkTrue(null == bufferPreparatorBuilder);
    checkTrue(null == streamPreparatorBuilder);
    checkTrue(null == exceptionHandlerBuilder);

    final MetaAddressingMode targetMode = metaModel.getAddressingMode(targetName);
    if (null == targetMode) {
      throw new IllegalArgumentException(String.format(
          "%s is not an addressing mode and cannot be a target for a preparator.", targetName));
    }

    preparatorBuilder = new PreparatorBuilder(targetMode, isComparator);
    return preparatorBuilder; 
  }

  public void endPreparator() {
    endBuildingCall();
    Logger.debug("End preparator: %s", preparatorBuilder.getTargetName());

    final Preparator preparator = preparatorBuilder.build();
    Logger.debug("Registering preparator: %s", preparator);

    final Preparator oldPreparator = preparators.addPreparator(preparator);
    if (null != oldPreparator) {
      Logger.warning("%s defined at %s is redefined at %s", 
          oldPreparator, oldPreparator.getWhere(), preparator.getWhere());
    }

    preparatorBuilder = null;
  }

  public void beginPreparatorVariant(final String name, final BigInteger bias) {
    checkPreparatorBlock();
    if (null != bias) {
      preparatorBuilder.beginVariant(name, bias.intValue());
    } else {
      preparatorBuilder.beginVariant(name);
    }
  }

  public void endPreparatorVariant() {
    checkPreparatorBlock();
    preparatorBuilder.endVariant();
  }

  public LazyValue newLazy() {
    checkPreparatorBlock();
    return preparatorBuilder.newValue();
  }

  public LazyValue newLazy(int start, int end) {
    checkPreparatorBlock();
    return preparatorBuilder.newValue(start, end);
  }

  public Primitive getPreparatorTarget() {
    checkPreparatorBlock();
    return preparatorBuilder.getTarget();
  }

  private void checkPreparatorBlock() {
    if (null == preparatorBuilder) {
      throw new IllegalStateException(
          "The construct cannot be used outside a preparator block.");
    }
  }

  public void addPreparatorCall(
      final Primitive targetMode,
      final BigInteger value,
      final String preparatorName,
      final String variantName) {
    checkNotNull(targetMode);
    checkNotNull(value);

    endBuildingCall();
    Logger.debug("Preparator invocation: %s = 0x%x", targetMode.getName(), value); 

    final MetaAddressingMode metaTargetMode =
        metaModel.getAddressingMode(targetMode.getName());

    checkNotNull(
        metaTargetMode, "No such addressing mode: " + targetMode.getName());

    final BitVector data =
        BitVector.valueOf(value, metaTargetMode.getDataType().getBitSize());

    final Preparator preparator =
        preparators.getPreparator(targetMode, data, preparatorName);

    if (null == preparator) {
      throw new GenerationAbortedException(
          String.format("No suitable preparator is found for %s.", targetMode.getSignature()));
    }

    final List<Call> initializer =
        preparator.makeInitializer(preparators, targetMode, data, variantName);

    for (final Call call : initializer) {
      addCall(call);
    }
  }

  public void addPreparatorCall(
      final Primitive targetMode,
      final RandomValue value,
      final String preparatorName,
      final String variantName) {
    checkNotNull(targetMode);
    checkNotNull(value);
    addPreparatorCall(targetMode, value.getValue(), preparatorName, variantName);
  }

  public void addPreparatorCall(
      final Primitive targetMode,
      final LazyValue value,
      final String preparatorName,
      final String variantName) {
    if (null == preparatorBuilder && null == bufferPreparatorBuilder) {
      throw new IllegalStateException(
          "A preparator with a lazy value can be invoked only inside " + 
          "a preparator or buffer_preparator block.");
    }

    checkNotNull(targetMode);
    checkNotNull(value);

    endBuildingCall();
    Logger.debug("Preparator reference: %s", targetMode.getName());

    callBuilder.setPreparatorReference(
        new PreparatorReference(targetMode, value, preparatorName, variantName));

    endBuildingCall();
  }

  public StreamPreparatorBuilder beginStreamPreparator(
      final String dataModeName, final String indexModeName) {

    endBuildingCall();

    Logger.debug("Begin stream preparator(data_source: %s, index_source: %s)",
        dataModeName, indexModeName);

    checkTrue(null == preparatorBuilder);
    checkTrue(null == bufferPreparatorBuilder);
    checkTrue(null == streamPreparatorBuilder);
    checkTrue(null == exceptionHandlerBuilder);

    final MetaAddressingMode dataMode = metaModel.getAddressingMode(dataModeName);
    if (null == dataMode) {
      throw new IllegalArgumentException(String.format(
          "%s is not an addressing mode.", dataModeName));
    }

    final MetaAddressingMode indexMode = metaModel.getAddressingMode(indexModeName);
    if (null == indexMode) {
      throw new IllegalArgumentException(String.format(
          "%s is not an addressing mode.", indexModeName));
    }

    streamPreparatorBuilder = new StreamPreparatorBuilder(
        dataManager.getGlobalLabels(), dataMode, indexMode);

    return streamPreparatorBuilder;
  }

  public void endStreamPreparator() {
    endBuildingCall();

    Logger.debug("End stream preparator");

    final StreamPreparator streamPreparator = streamPreparatorBuilder.build();
    streams.addPreparator(streamPreparator);

    streamPreparatorBuilder = null;
  }

  public Primitive getDataSource() {
    checkStreamPreparatorBlock("data_source");
    return streamPreparatorBuilder.getDataSource();
  }

  public Primitive getIndexSource() {
    checkStreamPreparatorBlock("index_source");
    return streamPreparatorBuilder.getIndexSource();
  }

  public LabelValue getStartLabel() {
    checkStreamPreparatorBlock("start_label");
    return streamPreparatorBuilder.getStartLabel();
  }

  private void checkStreamPreparatorBlock(final String keyword) {
    if (null == streamPreparatorBuilder) {
      throw new IllegalStateException(String.format(
          "The %s keyword cannot be used outside a stream preparator block.", keyword));
    }
  }
  
  public void beginStreamInitMethod() {
    Logger.debug("Begin Stream Method: init");
    streamPreparatorBuilder.beginInitMethod();
  }

  public void beginStreamReadMethod() {
    Logger.debug("Begin Stream Method: read");
    streamPreparatorBuilder.beginReadMethod();
  }

  public void beginStreamWriteMethod() {
    Logger.debug("Begin Stream Method: write");
    streamPreparatorBuilder.beginWriteMethod();
  }

  public void endStreamMethod() {
    Logger.debug("End Stream Method");
    endBuildingCall();
    streamPreparatorBuilder.endMethod(); 
  }

  public void addStream(
      final String startLabelName,
      final Primitive dataSource,
      final Primitive indexSource,
      final int length) {

    Logger.debug("Stream: label=%s, data=%s, source=%s, length=%s",
        startLabelName, dataSource.getName(), indexSource.getName(), length);

    streams.addStream(
        new Label(startLabelName, getCurrentBlockId()),
        dataSource,
        indexSource,
        length
        );

    /*
    // THIS IS CODE TO TEST DATA STREAMS. IT ADDS CALLS FROM DATA STREAMS
    // TO THE CURRENT TEST SEQUENCE.
    final Stream stream = streams.addStream(startLabelName, dataSource, indexSource, length);
    for (final Call call : stream.getInit()) {
      blockBuilders.peek().addCall(call);
    }

    int index = 0;
    while (index < length) {
      for (final Call call : stream.getRead()) {
        blockBuilders.peek().addCall(call);
      }
      index++;

      if (index < length) {
        for (final Call call : stream.getWrite()) {
          blockBuilders.peek().addCall(call);
        }
        index++;
      }
    }
    */
  }

  public BufferPreparatorBuilder beginBufferPreparator(final String bufferId) {
    endBuildingCall();

    Logger.debug("Begin buffer preparator: %s", bufferId);
    checkNotNull(bufferId);

    checkTrue(null == preparatorBuilder);
    checkTrue(null == bufferPreparatorBuilder);
    checkTrue(null == streamPreparatorBuilder);
    checkTrue(null == exceptionHandlerBuilder);

    bufferPreparatorBuilder = new BufferPreparatorBuilder(bufferId);
    return bufferPreparatorBuilder;
  }

  public void endBufferPreparator() {
    endBuildingCall();
    Logger.debug("End buffer preparator: %s", bufferPreparatorBuilder.getBufferId());

    final BufferPreparator bufferPreparator = bufferPreparatorBuilder.build();
    bufferPreparators.addPreparator(bufferPreparator);

    bufferPreparatorBuilder = null;
  }

  public LazyValue newAddressReference() {
    checkBufferPreparatorBlock();
    return bufferPreparatorBuilder.newAddressReference();
  }

  public LazyValue newAddressReference(final int start, final int end) {
    checkBufferPreparatorBlock();
    return bufferPreparatorBuilder.newAddressReference(start, end);
  }

  public LazyValue newEntryReference(final int start, final int end) {
    checkBufferPreparatorBlock();
    return bufferPreparatorBuilder.newEntryReference(start, end);
  }

  public LazyValue newEntryFieldReference(final String fieldId) {
    checkBufferPreparatorBlock();
    return bufferPreparatorBuilder.newEntryFieldReference(fieldId);
  }

  public LazyValue newEntryFieldReference(
      final String fieldId, final int start, final int end) {
    checkBufferPreparatorBlock();
    return bufferPreparatorBuilder.newEntryFieldReference(fieldId, start, end);
  }

  private void checkBufferPreparatorBlock() {
    if (null == bufferPreparatorBuilder) {
      throw new IllegalStateException(
          "The construct cannot be used outside a buffer_preparator block.");
    }
  }

  public PrimitiveBuilder newAddressingModeBuilderForGroup(final String name) {
    final Variate<String> variate = getGroupVariate(name);
    return newAddressingModeBuilder(variate.value());
  }

  public MetaOperation chooseMetaOperationFromGroup(final String name) {
    final Variate<String> variate = getGroupVariate(name);
    final String opName = variate.value();

    final MetaOperation metaOperation = metaModel.getOperation(opName);
    if (null == metaOperation) {
      throw new IllegalStateException("No such operation defined: " + opName);
    }

    return metaOperation;
  }

  private static Map<String, Variate<String>> newVariatesForGroups(final MetaModel model) {
    checkNotNull(model);

    final Map<String, Variate<String>> result = new HashMap<>();
    for (final MetaGroup group : model.getAddressingModeGroups()) {
      result.put(group.getName(), newVariateForGroup(group));
    }

    for (final MetaGroup group : model.getOperationGroups()) {
      result.put(group.getName(), newVariateForGroup(group));
    }

    return result;
  }

  private static Variate<String> newVariateForGroup(final MetaGroup group) {
    checkNotNull(group);

    final VariateBuilder<String> builder = new VariateBuilder<>();
    for (final MetaData item : group.getItems()) {
      if (item instanceof MetaGroup) {
        builder.addVariate(newVariateForGroup((MetaGroup) item));
      } else {
        builder.addValue(item.getName());
      }
    }

    return builder.build();
  }

  private Variate<String> getGroupVariate(final String name) {
    checkNotNull(name);

    final Variate<String> variate = groupVariates.get(name);
    if (null == variate) {
      throw new IllegalArgumentException(String.format("The %s group is not defined.", name));
    }

    return variate;
  }

  public void defineGroup(final String name, final Variate<String> variate) {
    checkNotNull(name);
    checkNotNull(variate);

    if (groupVariates.containsKey(name)) {
      throw new IllegalStateException(String.format("%s group is already defined", name));
    }

    groupVariates.put(name, variate);
  }

  public ExceptionHandlerBuilder beginExceptionHandler() {
    endBuildingCall();
    Logger.debug("Begin exception handler");

    if (isExceptionHandlerDefined) {
      throw new IllegalStateException("Exception handler is already defined.");
    }

    checkTrue(null == preparatorBuilder);
    checkTrue(null == bufferPreparatorBuilder);
    checkTrue(null == streamPreparatorBuilder);
    checkTrue(null == exceptionHandlerBuilder);

    exceptionHandlerBuilder = new ExceptionHandlerBuilder();
    return exceptionHandlerBuilder;
  }

  public void endExceptionHandler() {
    endBuildingCall();
    Logger.debug("End exception handler");

    final ExceptionHandler handler = exceptionHandlerBuilder.build();
    exceptionHandlerBuilder = null;

    processor.defineExceptionHandler(handler);
    isExceptionHandlerDefined = true;
  }

  public DataSectionBuilder beginData(final boolean isGlobalArgument, final boolean isSeparateFile) {
    endBuildingCall();

    final boolean isGlobalContext =
        openBlockCount == 0 &&
        preparatorBuilder == null &&
        bufferPreparatorBuilder == null &&
        streamPreparatorBuilder == null &&
        exceptionHandlerBuilder == null;

    final boolean isGlobal = isGlobalContext || isGlobalArgument;
    Logger.debug("Begin Data (isGlobal=%b, isSeparateFile=%b)", isGlobal, isSeparateFile);
    return dataManager.beginData(getCurrentBlockId(), isGlobal, isSeparateFile);
  }

  public void endData() {
    Logger.debug("End Data");

    final DataSection data = dataManager.endData();
    if (data.isGlobal()) {
      dataManager.processData(data);
      context.setAddress(dataManager.getAddress().longValue());
    } else {
      endBuildingCall();
      addCall(Call.newData(data));
    }
  }

  public void setOrigin(final BigInteger origin) {
    Logger.debug("Set Origin to 0x%x", origin);
    callBuilder.setOrigin(origin, false);
  }

  public void setRelativeOrigin(final BigInteger delta) {
    Logger.debug("Set Relative Origin to 0x%x", delta);
    callBuilder.setOrigin(delta, true);
  }

  public void setAlignment(final BigInteger value, final BigInteger valueInBytes) {
    Logger.debug("Align %d (%d bytes)", value, valueInBytes);
    callBuilder.setAlignment(value, valueInBytes);
  }

  public void beginPrologue() {
    endBuildingCall();
    Logger.debug("Begin Test Case Level Prologue");

    checkTrue(null == preparatorBuilder);
    checkTrue(null == bufferPreparatorBuilder);
    checkTrue(null == streamPreparatorBuilder);
    checkTrue(null == exceptionHandlerBuilder);
    checkTrue(null == prologueBuilder);
    checkTrue(null == epilogueBuilder);

    prologueBuilder = new ArrayList<>();
  }

  public void endPrologue() {
    endBuildingCall();
    Logger.debug("End Test Case Level Prologue");

    if (openBlockCount > 0) {
      final BlockBuilder currentBlockBuilder = blockBuilders.peek();
      currentBlockBuilder.setPrologue(prologueBuilder);
    } else {
      checkTrue(null == globalPrologue, "Global test case level prologue is redefined");
      globalPrologue = Collections.unmodifiableList(prologueBuilder);
    }

    prologueBuilder = null;
  }

  public void beginEpilogue() {
    endBuildingCall();
    Logger.debug("Begin Test Case Level Epilogue");

    checkTrue(null == preparatorBuilder);
    checkTrue(null == bufferPreparatorBuilder);
    checkTrue(null == streamPreparatorBuilder);
    checkTrue(null == exceptionHandlerBuilder);
    checkTrue(null == prologueBuilder);
    checkTrue(null == epilogueBuilder);

    epilogueBuilder = new ArrayList<>();
  }

  public void endEpilogue() {
    endBuildingCall();
    Logger.debug("End Test Case Level Epilogue");

    if (openBlockCount > 0) {
      final BlockBuilder currentBlockBuilder = blockBuilders.peek();
      currentBlockBuilder.setEpilogue(epilogueBuilder);
    } else {
      checkTrue(null == globalEpilogue, "Global test case level epilogue is redefined");
      globalEpilogue = Collections.unmodifiableList(epilogueBuilder);
    }

    epilogueBuilder = null;
  }

  public MemoryObjectBuilder newMemoryObjectBuilder(final int size) {
    return new MemoryObjectBuilder(
        size,
        dataManager.getGlobalLabels(),
        context.getSettings()
        );
  }

  public Where where(final String file, final int line) {
    return new Where(file, line);
  }
}
