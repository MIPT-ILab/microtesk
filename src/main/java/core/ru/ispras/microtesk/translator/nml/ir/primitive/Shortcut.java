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

package ru.ispras.microtesk.translator.nml.ir.primitive;

import static ru.ispras.fortress.util.InvariantChecks.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.ispras.fortress.util.Pair;
import ru.ispras.microtesk.translator.nml.ir.analysis.MemoryAccessStatus;
import ru.ispras.microtesk.utils.StringUtils;

/**
 * The Shortcut class describes a shortcut (a short way) to address a group of operations within the
 * operation tree (some specific path) that describes a composite operation. In most specifications,
 * there are paths in the operation tree can be built in an unambiguous way (without a need for
 * external information or a client decision). To simplify describing composite instructions calls
 * in test templates, all such paths are saved as shortcuts associated with their target operations
 * (the point there the path ends, the most important operation that distinguishes a specific path
 * from other similar paths).
 * 
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class Shortcut {
  /**
   * The Argument class describes shortcut arguments (arguments of a composite operation).
   * 
   * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
   */
  public static final class Argument {
    private final String uniqueName;
    private final Primitive type;
    private final String name;
    private final PrimitiveAND source;

    public Argument(
        final String uniqueName,
        final Primitive type,
        final String name,
        final PrimitiveAND source) {
      this.uniqueName = uniqueName;
      this.type = type;
      this.name = name;
      this.source = source;
    }

    /**
     * Returns a name the uniquely identifies the given argument in the set of shortcut arguments.
     * 
     * @return Unique argument name.
     */
    public String getUniqueName() {
      return uniqueName;
    }

    /**
     * Returns the primitive describing the argument type.
     * 
     * @return Argument type.
     */
    public Primitive getType() {
      return type;
    }

    /**
     * Returns the argument name as it is defined in the signature of the operation it will be
     * actually passed to.
     * 
     * @return Argument name from the source operation signature.
     */
    public String getName() {
      return name;
    }

    /**
     * Returns the operation on the shortcut path the argument is actually passed to.
     * 
     * @return Source operation.
     */
    public PrimitiveAND getSource() {
      return source;
    }

    @Override
    public String toString() {
      return String.format("%s: %s (%s in %s)", uniqueName, type.getName(), name, source.getName());
    }
  }

  private final PrimitiveAND entry;
  private final PrimitiveAND target;
  private final List<String> contextNames;
  private final Map<String, Argument> arguments;
  private final PrimitiveInfo info;

  /**
   * Constructs a shortcut object. The shortcut object describes how to create the target operation
   * and all other operations it requires from the starting point called entry. The context is the
   * name of the operation that accepts as an argument the composite operation built with the help
   * of the shortcut. The map of arPrimitiveguments is built by traversing the path.
   * 
   * @param entry The entry point where the path starts (the top point).
   * @param target The target operation of the shortcut.
   * @param contextNames The list of names that identify the contexts in which the shortcut can be
   *        called.
   * 
   * @throws IllegalArgumentException if any of the parameters equals null.
   * @throws IllegalArgumentException if target or entry is not an operation; if an operation on the
   *         shortcut path is an OR rule (all OR rules must be resolved at this point).
   */
  public Shortcut(
      final PrimitiveAND entry,
      final PrimitiveAND target,
      final List<String> contextNames) {
    checkNotNull(entry);
    checkNotNull(target);
    checkNotNull(contextNames);

    opCheck(entry);
    opCheck(target);

    this.entry = entry;
    this.target = target;
    this.contextNames = contextNames;

    this.arguments = new LinkedHashMap<>();
    addArguments(entry, false);

    this.info = new PrimitiveInfo();

    final Pair<Boolean, Boolean> branchInfo = getBranchInfo(entry, false);
    this.info.setBranch(branchInfo.first);
    this.info.setConditionalBranch(branchInfo.second);

    final MemoryAccessStatus memAccessStatus = getMemoryAccessStatus(entry, false);
    this.info.setLoad(memAccessStatus.isLoad());
    this.info.setStore(memAccessStatus.isStore());
    this.info.setBlockSize(memAccessStatus.getBlockSize());
  }

  /**
   * Constructor than uses one context name.
   */
  public Shortcut(
      final PrimitiveAND entry,
      final PrimitiveAND target,
      final String contextName) {
    this(entry, target, Collections.singletonList(contextName));
    checkNotNull(contextName);
  }

  /**
   * Adds arguments of the specified operation and of all its child operations to the argument map.
   * The method is called recursively until the target operation is reached.
   * 
   * @param root The operation to be processed.
   * @param reachedTarget Specifies whether the target operation is reached. It it is, the recursion
   *        stops and all child operations are added as arguments.
   */
  private void addArguments(final PrimitiveAND root, final boolean reachedTarget) {
    for (final Map.Entry<String, Primitive> e : root.getArguments().entrySet()) {
      final String argName = e.getKey();
      final Primitive argType = e.getValue();

      if ((argType.getKind() == Primitive.Kind.OP) && !reachedTarget) {
        notOrRuleCheck(argType);
        addArguments((PrimitiveAND) argType, argType == target);
      } else {
        final String uniqueArgName = createUniqueArgumentName(argName);
        final Argument arg = new Argument(uniqueArgName, argType, argName, root);
        arguments.put(uniqueArgName, arg);
      }
    }
  }

  private Pair<Boolean, Boolean> getBranchInfo(final PrimitiveAND root, final boolean reachedTarget) {
    if (root.getInfo().isBranch()) {
      return new Pair<>(root.getInfo().isBranch(), root.getInfo().isConditionalBranch());
    }

    for (final Primitive arg : root.getArguments().values()) {
      if ((arg.getKind() == Primitive.Kind.OP) && !reachedTarget) {
        notOrRuleCheck(arg);
        final PrimitiveAND primitive = (PrimitiveAND) arg;
        final Pair<Boolean, Boolean> branchInfo = getBranchInfo(primitive, primitive == target);

        if (branchInfo.first) {
          return branchInfo;
        }
      }
    }

    return new Pair<>(false, false);
  }

  private MemoryAccessStatus getMemoryAccessStatus(
      final PrimitiveAND root, final boolean reachedTarget) {

    MemoryAccessStatus result = MemoryAccessStatus.NO;
    for (final Primitive arg : root.getArguments().values()) {
      if ((arg.getKind() == Primitive.Kind.OP) && !reachedTarget) {
        notOrRuleCheck(arg);
        final PrimitiveAND primitive = (PrimitiveAND) arg;
        result = result.merge(new MemoryAccessStatus(
            primitive.getInfo().isLoad(), primitive.getInfo().isStore(), primitive.getInfo().getBlockSize()));
        result = result.merge(
            getMemoryAccessStatus(primitive, primitive == target));
      }
    }

    return result;
  }

  /**
   * Creates a name that uniquely identifies an argument of a composite operation. The name is based
   * on the name of the argument of the operation on the shortcut path which takes the given
   * argument. When there are arguments that use the same name (different operation on the shortcut
   * path use the same name) a unique name is created by adding an index to the original name.
   * 
   * @param name Original name.
   * @return A unique argument name.
   */
  private String createUniqueArgumentName(final String name) {
    String result = name;

    int index = 0;
    while (arguments.containsKey(result)) {
      result = String.format("%s_%d", name, ++index);
    }

    return result;
  }

  /**
   * Returns the name of the shortcut. Corresponds to the name of the target operation.
   * 
   * @return Shortcut name.
   */
  public String getName() {
    return target.getName();
  }

  /**
   * Returns the entry operation.
   * 
   * @return Entry operation.
   */
  public PrimitiveAND getEntry() {
    return entry;
  }

  /**
   * Returns the target operation.
   * 
   * @return Target operation.
   */
  public PrimitiveAND getTarget() {
    return target;
  }

  /**
   * Returns the list of context identifiers (names of operations that accept the composite object
   * created by the shortcut as an argument).
   * 
   * @return List of context identifiers.
   */
  public List<String> getContextName() {
    return contextNames;
  }

  /**
   * Returns a collection of shortcut arguments.
   * 
   * @return Shortcut arguments.
   */
  public Collection<Argument> getArguments() {
    return arguments.values();
  }

  public PrimitiveInfo getInfo() {
    return info;
  }

  @Override
  public String toString() {
    return String.format(
        "%s: Entry = %s, Target = %s, Contexts = [%s], Args = [%s]",
        getName(),
        entry.getName(),
        target.getName(),
        StringUtils.toString(contextNames, ", "),
        StringUtils.toString(arguments.values(), ", ")
        );
  }

  private static void opCheck(final Primitive p) {
    if (p.getKind() != Primitive.Kind.OP) {
      throw new IllegalArgumentException(String.format(
          "The %s primitive is not an operation.", p.getName()));
    }
  }

  private static void notOrRuleCheck(final Primitive p) {
    if (p.isOrRule()) {
      throw new IllegalArgumentException(String.format(
          "The %s primitive is an OR rule.", p.getName()));
    }
  }
}
