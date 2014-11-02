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

package org.apache.cayenne.testing.statement;

import org.apache.cayenne.testing.CayenneTestContextManager;
import org.junit.runners.model.Statement;

/**
 * {@code RunBeforeTestClassCallbacks} is a custom JUnit 4.5+ {@link Statement}
 * which allows the <em>Cayenne TestContext</em> to be plugged into the JUnit
 * execution chain by calling
 * {@link CayenneTestContextManager#beforeTestClass() beforeTestClass()} on the
 * supplied {@link CayenneTestContextManager}.
 *
 * @see #evaluate()
 * @see RunAfterTestMethodCallbacks
 * @since 4.0
 */
public class RunBeforeTestClassCallbacks extends Statement {

    /**
     * the chained statement to execute before.
     */
    private final Statement statement;

    /**
     * Test context manager to call after execution of chained statement.
     */
    private final CayenneTestContextManager testContextManager;

    /**
     * Constructs a new {@code RunBeforeTestClassCallbacks} statement.
     *
     * @param statement
     *            the next {@code Statement} in the execution chain
     * @param testContextManager
     *            the TestContextManager upon which to call
     *            <code>beforeTestClass()</code>
     */
    public RunBeforeTestClassCallbacks(Statement statement, CayenneTestContextManager testContextManager) {
        this.statement = statement;
        this.testContextManager = testContextManager;
    }

    /**
     * Calls {@link CayenneTestContextManager#beforeTestClass()} and then
     * invokes the next {@link Statement} in the execution chain (typically an
     * instance of {@link org.junit.internal.runners.statements.RunBefores
     * RunBefores}).
     */
    @Override
    public void evaluate() throws Throwable {
        this.testContextManager.beforeTestClass();
        this.statement.evaluate();
    }

}
