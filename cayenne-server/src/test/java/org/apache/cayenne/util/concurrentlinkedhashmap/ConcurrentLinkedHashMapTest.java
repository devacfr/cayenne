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
package org.apache.cayenne.util.concurrentlinkedhashmap;

import org.apache.cayenne.testing.TestCase;
import org.junit.Test;


public class ConcurrentLinkedHashMapTest extends TestCase {

	@Test
    public void testPutGet() {
        ConcurrentLinkedHashMap<String, Object> m = new ConcurrentLinkedHashMap.Builder<String, Object>()
                .maximumWeightedCapacity(10)
                .build();

        assertEquals(0, m.size());
        m.put("k1", 100);
        assertEquals(1, m.size());
        assertNull(m.get("nosuchkey"));
        assertEquals(100, m.get("k1"));

        m.put("k2", 200);
        assertEquals(2, m.size());
        assertEquals(200, m.get("k2"));
    }

	@Test
    public void testLRU() {
        ConcurrentLinkedHashMap<String, Object> m = new ConcurrentLinkedHashMap.Builder<String, Object>()
                .maximumWeightedCapacity(5)
                .build();

        assertEquals(0, m.size());
        m.put("k1", 100);
        assertEquals(1, m.size());
        m.put("k2", 101);
        assertEquals(2, m.size());
        m.put("k3", 102);
        assertEquals(3, m.size());
        m.put("k4", 103);
        assertEquals(4, m.size());
        m.put("k5", 104);
        assertEquals(5, m.size());
        m.put("k6", 105);
        assertEquals(5, m.size());
        m.put("k7", 106);
        assertEquals(5, m.size());
        m.put("k8", 107);
        assertEquals(5, m.size());

        m.remove("k6");
        assertEquals(4, m.size());

    }
}