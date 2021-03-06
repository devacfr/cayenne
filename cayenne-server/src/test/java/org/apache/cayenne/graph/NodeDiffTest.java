/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.graph;

import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class NodeDiffTest {

    @Test
    public void testGetNodeId() {
        Object id = new Object();
        NodeDiff diff = new MockNodeDiff(id);
        assertSame(id, diff.getNodeId());
    }

    @Test
    public void testCompareTo() {
        NodeDiff d1 = new MockNodeDiff("x", 1);
        NodeDiff d2 = new MockNodeDiff("y", 2);
        NodeDiff d3 = new MockNodeDiff("z", 3);
        NodeDiff d4 = new MockNodeDiff("a", 2);

        assertTrue(d1.compareTo(d2) < 0);
        assertTrue(d2.compareTo(d1) > 0);
        assertTrue(d1.compareTo(d3) < 0);
        assertTrue(d2.compareTo(d4) == 0);
        assertTrue(d2.compareTo(d3) < 0);
    }
}
