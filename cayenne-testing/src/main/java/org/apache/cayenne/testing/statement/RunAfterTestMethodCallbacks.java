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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.testing.CayenneTestContextManager;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

/**
 * {@code RunAfterTestMethodCallbacks} is a custom JUnit 4.5+ {@link Statement}
 * which allows the <em>Cayenne TestContext</em> to be plugged into the JUnit
 * execution chain by calling
 * {@link CayenneTestContextManager#afterTestMethod(Object, Method)
 * afterTestMethod()} on the supplied {@link CayenneTestContextManager}.
 *
 * @see #evaluate()
 * @see RunBeforeTestMethodCallbacks
 * @since 4.0
 */
public class RunAfterTestMethodCallbacks extends Statement {

    /**
     * the chained statement to execute before.
     */
    private final Statement statement;

    /**
     * the current test instance
     */
    private final Object testInstance;

    /**
     * the current test method
     */
    private final Method testMethod;

    /**
     * Test context manager to call after execution of chained statement.
     */
    private final CayenneTestContextManager testContextManager;

    /**
     * Constructs a new <code>RunAfterTestMethodCallbacks</code> statement.
     *
     * @param statement
     *            the next {@code Statement} in the execution chain
     * @param testInstance
     *            the current test instance (never {@code null})
     * @param testMethod
     *            the test method which has just been executed on the test
     *            instance
     * @param testContextManager
     *            the TestContextManager upon which to call
     *            <code>afterTestMethod()</code>
     */
    public RunAfterTestMethodCallbacks(Statement statement, Object testInstance, Method testMethod,
                                       CayenneTestContextManager testContextManager) {
        this.statement = statement;
        this.testInstance = testInstance;
        this.testMethod = testMethod;
        this.testContextManager = testContextManager;
    }

    /**
     * Invokes the next {@link Statement} in the execution chain (typically an
     * instance of {@link org.junit.internal.runners.statements.RunAfters
     * RunAfters}), catching any exceptions thrown, and then calls
     * {@link CayenneTestContextManager#afterTestMethod(Object, Method)} with
     * the first caught exception (if any). If the call to
     * {@code afterTestMethod()} throws an exception, it will also be tracked.
     * Multiple exceptions will be combined into a
     * {@link MultipleFailureException}.
     */
    @Override
    public void evaluate() throws Throwable {
        Throwable testException = null;
        List<Throwable> errors = new ArrayList<Throwable>();
        try {
            this.statement.evaluate();
        } catch (Throwable e) {
            testException = e;
            errors.add(e);
        }

        try {
            this.testContextManager.afterTestMethod(this.testInstance, this.testMethod, testException);
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