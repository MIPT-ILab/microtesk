/*
 * Copyright 2014-2018 ISP RAS (http://www.ispras.ru)
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BlockIdTestCase {
  @Test
  public void testEquals() {
    final BlockId root1 = new BlockId();
    final BlockId root2 = new BlockId();

    assertEquals(root1, root2);

    final BlockId child11 = root1.nextChildId();
    final BlockId child21 = root2.nextChildId();

    assertEquals(child11, child21);
    assertNotSame(child11, child21);

    assertFalse(root1.equals(child11));
    assertFalse(root1.equals(child21));
    assertFalse(root2.equals(child11));
    assertFalse(root2.equals(child21));

    assertEquals(child11.parentId(), child21.parentId());
    assertNotSame(child11.parentId(), child21.parentId());

    final BlockId child12 = root1.nextChildId();
    final BlockId child22 = root2.nextChildId();

    assertEquals(child12, child22);
    assertEquals(child12.parentId(), child22.parentId());

    assertEquals(child12.parentId(), child11.parentId());
    assertEquals(child22.parentId(), child21.parentId());

    assertFalse(child12.equals(child11));
    assertFalse(child12.equals(child21));
    assertFalse(child22.equals(child11));
    assertFalse(child22.equals(child21));

    final BlockId child121 = child12.nextChildId();
    final BlockId child221 = child22.nextChildId();

    assertEquals(child121, child221);
    assertEquals(child121.parentId().parentId(), child221.parentId().parentId());
    assertEquals(child121.parentId().parentId(), root2);
    assertEquals(child221.parentId().parentId(), root1);

    assertEquals(root1.getDepth(), root2.getDepth());
    assertEquals(child11.getDepth(), child12.getDepth());
    assertEquals(child11.getDepth(), child21.getDepth());
    assertEquals(child11.getDepth(), child22.getDepth());

    assertEquals(child121.getDepth(), child221.getDepth());
  }

  @Test
  public void testParentChild() {
    final BlockId root1 = new BlockId();
    final BlockId root2 = new BlockId();

    assertFalse(root1.isParent(root1));
    assertFalse(root1.isParent(root2));
    assertFalse(root2.isParent(root2));
    assertFalse(root2.isParent(root1));

    final BlockId child11 = root1.nextChildId();
    final BlockId child21 = root2.nextChildId();

    assertTrue(child11.isParent(root1));
    assertTrue(root1.isChild(child11));
    assertTrue(child11.isParent(root2));
    assertTrue(root1.isChild(child21));

    final BlockId child12 = root1.nextChildId();
    final BlockId child22 = root2.nextChildId();

    assertTrue(child12.isParent(root1));
    assertTrue(child12.isParent(root2));
    assertTrue(child22.isParent(root1));
    assertTrue(child22.isParent(root2));

    assertTrue(root1.isChild(child11));
    assertTrue(root1.isChild(child12));
    assertTrue(root1.isChild(child21));
    assertTrue(root1.isChild(child22));

    assertTrue(root2.isChild(child11));
    assertTrue(root2.isChild(child12));
    assertTrue(root2.isChild(child21));
    assertTrue(root2.isChild(child22));

    assertFalse(child11.isChild(child12));
    assertFalse(child21.isParent(child11));
    assertFalse(child11.isChild(child21));
    assertFalse(child11.isParent(child22));

    final BlockId child121 = child12.nextChildId();
    final BlockId child221 = child22.nextChildId();

    assertTrue(child121.isParent(root1));
    assertTrue(child121.isParent(root2));
    assertTrue(child221.isParent(root1));
    assertTrue(child221.isParent(root2));

    assertTrue(root1.isChild(child121));
    assertTrue(root2.isChild(child121));
    assertTrue(root1.isChild(child221));
    assertTrue(root2.isChild(child221));

    assertTrue(child121.isParent(child12));
    assertTrue(child221.isParent(child12));
    assertTrue(child121.isParent(child22));
    assertTrue(child221.isParent(child22));

    assertTrue(child12.isChild(child121));
    assertTrue(child12.isChild(child221));
    assertTrue(child22.isChild(child121));
    assertTrue(child22.isChild(child221));

    assertFalse(child121.isParent(child11));
    assertFalse(child221.isParent(child11));
    assertFalse(child121.isParent(child21));
    assertFalse(child221.isParent(child21));

    assertFalse(child11.isChild(child121));
    assertFalse(child11.isChild(child221));
    assertFalse(child21.isChild(child121));
    assertFalse(child21.isChild(child221));
  }

  @Test
  public void testDistance() {
    final BlockId root1 = new BlockId();
    final BlockId root2 = new BlockId();

    final BlockId.Distance ZERO = new BlockId.Distance(0, 0);

    assertEquals(ZERO, root1.getDistance(root1));
    assertEquals(ZERO, root1.getDistance(root2));
    assertEquals(ZERO, root2.getDistance(root1));
    assertEquals(ZERO, root2.getDistance(root2));

    final BlockId child11 = root1.nextChildId();
    final BlockId child21 = root2.nextChildId();

    final BlockId.Distance ONE_UP = new BlockId.Distance(1, 0);
    final BlockId.Distance ONE_DOWN = new BlockId.Distance(0, 1);

    assertEquals(ONE_DOWN, root1.getDistance(child11));
    assertEquals(ONE_DOWN, root1.getDistance(child21));

    assertEquals(ONE_UP, child11.getDistance(root1));
    assertEquals(ONE_UP, child21.getDistance(root1));

    final BlockId child12 = root1.nextChildId();
    final BlockId child22 = root2.nextChildId();

    final BlockId.Distance ONE_ONE = new BlockId.Distance(1, 1);

    assertEquals(ONE_ONE, child11.getDistance(child12));
    assertEquals(ONE_ONE, child11.getDistance(child22));
    assertEquals(ONE_ONE, child21.getDistance(child12));
    assertEquals(ONE_ONE, child21.getDistance(child22));

    final BlockId child111 = child11.nextChildId();
    final BlockId child211 = child21.nextChildId();
    final BlockId child121 = child12.nextChildId();
    final BlockId child221 = child22.nextChildId();

    final BlockId.Distance TWO_TWO = new BlockId.Distance(2, 2);

    assertEquals(ZERO, child111.getDistance(child211));
    assertEquals(TWO_TWO, child111.getDistance(child121));
    assertEquals(TWO_TWO, child111.getDistance(child221));

    assertEquals(new BlockId.Distance(2, 1), child111.getDistance(child12));
    assertEquals(new BlockId.Distance(2, 1), child111.getDistance(child22));

    assertEquals(new BlockId.Distance(1, 2), child12.getDistance(child111));
    assertEquals(new BlockId.Distance(1, 2), child22.getDistance(child111));
  }
}
