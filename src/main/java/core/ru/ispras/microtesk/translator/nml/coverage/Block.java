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

package ru.ispras.microtesk.translator.nml.coverage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ru.ispras.fortress.expression.NodeOperation;
import ru.ispras.fortress.expression.NodeVariable;
import ru.ispras.fortress.util.InvariantChecks;

public final class Block {
  private final List<NodeOperation> statements;
  private final Map<String, NodeVariable> inputs;
  private final Map<String, NodeVariable> outputs;
  private final List<NodeVariable> intermediates;
  private List<GuardedBlock> children;
  private Block successor;

  Block(
      List<NodeOperation> statements,
      Map<String, NodeVariable> inputs,
      Map<String, NodeVariable> outputs,
      List<NodeVariable> intermediates) {
    InvariantChecks.checkNotNull(statements);
    InvariantChecks.checkNotNull(inputs);
    InvariantChecks.checkNotNull(outputs);
    InvariantChecks.checkNotNull(intermediates);

    this.statements = statements;
    this.inputs = inputs;
    this.outputs = outputs;
    this.intermediates = intermediates;
    this.children = Collections.emptyList();
    this.successor = null;
  }

  Block(List<NodeOperation> statements) {
    this.statements = statements;
    this.inputs = Collections.emptyMap();
    this.outputs = Collections.emptyMap();
    this.intermediates = Collections.emptyList();
    this.children = Collections.emptyList();
    this.successor = null;
  }

  void setChildren(List<GuardedBlock> children) {
    this.children = children;
  }

  public List<GuardedBlock> getChildren() {
    return Collections.unmodifiableList(children);
  }

  void setSuccessor(Block block) {
    this.successor = block;
  }

  public Block getSuccessor() {
    return this.successor;
  }

  public List<NodeOperation> getStatements() {
    return Collections.unmodifiableList(statements);
  }

  public Map<String, NodeVariable> getInputs() {
    return Collections.unmodifiableMap(inputs);
  }

  public Map<String, NodeVariable> getOutputs() {
    return Collections.unmodifiableMap(outputs);
  }

  public List<NodeVariable> getIntermediates() {
    return Collections.unmodifiableList(intermediates);
  }
}

final class BlockBuilder {
  private static final List<NodeOperation> PHI_STATEMENTS =
    Collections.singletonList(new NodeOperation(SsaOperation.PHI));

  private List<NodeOperation> statements;
  private Map<String, NodeVariable> inputs;
  private Map<String, NodeVariable> outputs;
  private List<NodeVariable> intermediates;

  BlockBuilder() {
    this.statements = new ArrayList<>();
    this.inputs = new TreeMap<>();
    this.outputs = new TreeMap<>();
    this.intermediates = new ArrayList<>();
  }

  void add(NodeOperation s) {
    statements.add(s);
  }

  void addAll(Collection<NodeOperation> nodes) {
    statements.addAll(nodes);
  }

  public List<NodeOperation> getStatements() {
    return statements;
  }

  public Block build() {
    collectData(statements);
    return new Block(statements, inputs, outputs, intermediates);
  }

  private void collectData(List<NodeOperation> statements) {
    /* TODO populate input/output maps and intermediates list */
  }

  public static Block createSingleton(final NodeOperation node) {
    return new Block(Collections.singletonList(node));
  }

  public static Block createPhi() {
    return new Block(PHI_STATEMENTS);
  }

  public static Block createEmpty() {
    return new Block(Collections.<NodeOperation>emptyList());
  }
}
