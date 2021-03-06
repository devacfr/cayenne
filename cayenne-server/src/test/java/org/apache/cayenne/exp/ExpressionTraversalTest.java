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

package org.apache.cayenne.exp;

import org.apache.cayenne.testing.TestCase;
import org.junit.Test;

public class ExpressionTraversalTest extends TestCase {

    private TstTraversalHandler handler;

    @Override
    public void setUp() throws Exception {
        handler = new TstTraversalHandler();
    }

    @Test
    public void testUnary() throws Exception {
        doExpressionTest(new TstUnaryExpSuite());
    }

    @Test
    public void testBinary() throws Exception {
        doExpressionTest(new TstBinaryExpSuite());
    }

    @Test
    public void testTernary() throws Exception {
        doExpressionTest(new TstTernaryExpSuite());
    }

    private void doExpressionTest(TstExpressionSuite suite) throws Exception {
        TstExpressionCase[] cases = suite.cases();

        int len = cases.length;
        for (int i = 0; i < len; i++) {
            handler.reset();
            cases[i].getCayenneExp().traverse(handler);

            // assert statistics
            handler.assertConsistency();
            cases[i].assertParsedWell(handler.getNodeCount(), handler.getLeafs());
        }
    }
}
