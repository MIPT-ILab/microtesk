package ru.ispras.microtesk.tools.symexec;

import java.util.Collections;
import java.util.List;
import java.util.Queue;

import ru.ispras.microtesk.model.IsaPrimitive;

public class ControlFlowInspector {
  private final List<IsaPrimitive> insns;
  private final List<Range> ranges = new java.util.ArrayList<>();
  private final Queue<Integer> queue = new java.util.ArrayDeque<>();

  public ControlFlowInspector(final List<IsaPrimitive> insns) {
    this.insns = insns;
  }

  public List<Range> inspect() {
    queue.add(0);
    while (!queue.isEmpty()) {
      final int start = queue.remove();
      final Range range = rangeOf(start);
      if (range != null && range.start != start) {
        ranges.add(range.split(start));
      } else {
        int i = start;
        while (i < insns.size()) {
          final IsaPrimitive insn = insns.get(i);
          if (isBranch(insn)) {
            final int target = i + getBranchOffset(insn);
            // only local jumps considered for cfg
            if (target >= 0 && target < insns.size()) {
              final Range r = new Range(start, i + 1);
              r.nextTaken = target;
              r.nextOther = (isConditional(insn)) ? i + 1 : target;
              ranges.add(r);

              queue.add(target);
              queue.add(i + 1);
              break;
            }
          }
          ++i;
        }
        if (i >= insns.size()) {
          ranges.add(new Range(start, i));
        }
      }
    }
    Collections.sort(ranges);
    return Collections.unmodifiableList(ranges);
  }

  private Range rangeOf(final int index) {
    for (final Range range : ranges) {
      if (range.contains(index)) {
        return range;
      }
    }
    return null;
  }

  public static boolean isBranch(final IsaPrimitive insn) {
    return false;
  }

  public static int getBranchOffset(final IsaPrimitive insn) {
    return 1;
  }

  public static boolean isConditional(final IsaPrimitive insn) {
    return false;
  }

  public final static class Range implements Comparable<Range> {
    public int start;
    public int end;
    public int nextTaken;
    public int nextOther;

    public Range(final int start, final int end) {
      this.start = start;
      this.end = end;
      this.nextTaken = end;
      this.nextOther = end;
    }

    public int compareTo(final Range that) {
      return this.start - that.start;
    }

    public boolean contains(final int index) {
      return index >= start && index < end;
    }

    public Range split(final int index) {
      final Range tail = new Range(index, this.end);
      tail.nextTaken = this.nextTaken;
      tail.nextOther = this.nextOther;

      this.end = index;
      this.nextTaken = index;
      this.nextOther = index;

      return tail;
    }
  }
}