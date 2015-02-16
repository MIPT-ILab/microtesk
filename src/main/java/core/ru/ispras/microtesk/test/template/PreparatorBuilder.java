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

import java.util.ArrayList;
import java.util.List;

import ru.ispras.microtesk.test.template.Call;
import ru.ispras.microtesk.test.template.LazyData;
import ru.ispras.microtesk.test.template.LazyValue;

public final class PreparatorBuilder {
  private final LazyPrimitive target;
  private final LazyData data;
  private final List<Call> calls;

  PreparatorBuilder(String targetName) {
    checkNotNull(targetName);

    this.target = new LazyPrimitive(Primitive.Kind.MODE, targetName, targetName);
    this.data = new LazyData();
    this.calls = new ArrayList<Call>();
  }

  public String getTargetName() {
    return target.getName();
  }

  public LazyValue newValue() {
    return new LazyValue(data);
  }

  public LazyValue newValue(int start, int end) {
    return new LazyValue(data, start, end);
  }

  public Primitive getTarget() {
    return target;
  }

  public void addCall(Call call) {
    checkNotNull(call);
    calls.add(call);
  }

  public Preparator build() {
    return new Preparator(target, data, calls);
  }
}
