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

import org.apache.cayenne.testing.CayenneTestContextManager;
import org.junit.runners.model.Statement;

/**
 * {@code RunBeforeTestMethodCallbacks} is a custom JUnit 4.5+ {@link Statement}
 * which allows the <em>Cayenne TestContext</em> to be plugged into the JUnit
 * execution chain by calling
 * {@link CayenneTestContextManager#beforeTestMethod(Object, Method)
 * beforeTestMethod()} on the supplied {@link CayenneTestContextManager}.
 *
 * @see #evaluate()
 * @see RunAfterTestMethodCallbacks
 * @since 3.2
 */
public class RunBeforeTestMethodCallbacks extends Statement {

    /**
     * the next {@code Statement} in the execution chain
     */
    private final Statement statement;

    /**
     * the current test instance (never <code>null</code>).
     */
    private final Object testInstance;

    /**
     * the test method which is about to be executed on the test instance.
     */
    private final Method testMethod;

    /**
     * the TestContextManager upon which to call {@code beforeTestMethod()}.
     */
    private final CayenneTestContextManager testContextManager;

    /**
     * Constructs a new {@code RunBeforeTestMethodCallbacks} statement.
     *
     * @param statement
     *            the next {@code Statement} in the execution chain.
     * @param testInstance
     *            the current test instance (never <code>null</code>).
     * @param testMethod
     *            the test method which is about to be executed on the test
     *            instance.
     * @param testContextManager
     *            the TestContextManager upon which to call
     *            {@code beforeTestMethod()}.
     */
    public RunBeforeTestMethodCallbacks(Statement statement, Object testInstance, Method testMethod,
            CayenneTestContextManager testContextManager) {
        this.statement = statement;
        this.testInstance = testInstance;
        this.testMethod = testMethod;
        this.testContextManager = testContextManager;
    }

    /**
     * Calls {@link CayenneTestContextManager#beforeTestMethod(Object, Method)}
     * and then invokes the next {@link Statement} in the execution chain
     * (typically an instance of
     * {@link org.junit.internal.runners.statements.RunBefores RunBefores}).
     */
    @Override
    public void evaluate() throws Throwable {
        this.testContextManager.beforeTestMethod(this.testInstance, this.testMethod);
        this.statement.evaluate();
    }

}
