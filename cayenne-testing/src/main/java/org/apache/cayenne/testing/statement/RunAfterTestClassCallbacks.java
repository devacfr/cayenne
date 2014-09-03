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

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.testing.CayenneTestContextManager;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

/**
 * {@code RunAfterTestClassCallbacks} is a custom JUnit 4.5+ {@link Statement}
 * which allows the <em>Cayenne TestContext</em> to be plugged into the JUnit
 * execution chain by calling {@link CayenneTestContextManager#afterTestClass()
 * afterTestClass()} on the supplied {@link CayenneTestContextManager}.
 *
 * @see #evaluate()
 * @see RunBeforeTestMethodCallbacks
 * @since 3.2
 */
public class RunAfterTestClassCallbacks extends Statement {

    /**
     * the chained statement to execute before.
     */
    private final Statement statement;

    /**
     * Test context manager to call after execution of chained statement.
     */
    private final CayenneTestContextManager testContextManager;

    /**
     * Constructs a new {@code RunAfterTestClassCallbacks} statement.
     *
     * @param statement
     *            the next {@code Statement} in the execution chain
     * @param testContextManager
     *            the TestContextManager upon which to call
     *            <code>afterTestClass()</code>
     */
    public RunAfterTestClassCallbacks(Statement statement, CayenneTestContextManager testContextManager) {
        this.statement = statement;
        this.testContextManager = testContextManager;
    }

    /**
     * Invokes the next {@link Statement} in the execution chain (typically an
     * instance of {@link org.junit.internal.runners.statements.RunAfters
     * RunAfters}), catching any exceptions thrown, and then calls
     * {@link CayenneTestContextManager#afterTestClass()}. If the call to
     * {@code afterTestClass()} throws an exception, it will also be tracked.
     * Multiple exceptions will be combined into a
     * {@link MultipleFailureException}.
     */
    @Override
    public void evaluate() throws Throwable {
        List<Throwable> errors = new ArrayList<Throwable>();
        try {
            this.statement.evaluate();
        } catch (Throwable e) {
            errors.add(e);
        }

        try {
            this.testContextManager.afterTestClass();
        } catch (Exception e) {
            errors.add(e);
        }

        if (errors.isEmpty()) {
            return;
        }
        if (errors.size() == 1) {
            throw errors.get(0);
        }
        throw new MultipleFailureException(errors);
    }
}
