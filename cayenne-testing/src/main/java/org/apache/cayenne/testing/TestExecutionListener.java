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

package org.apache.cayenne.testing;

/**
 * <p>
 * {@code TestExecutionListener} defines a <em>listener</em> interface for
 * reacting to test execution events fired by the
 * {@link CayenneTestContextManager} with which the listener is registered.
 * </p>
 * <p>
 * Concrete implementations must provide a <code>public</code> no-args
 * constructor.
 * </p>
 *
 * @since 3.2
 */
public interface TestExecutionListener {

    /**
     * Called <em>before</em> execution of all tests within the test class.
     *
     * @param testContext
     *            the test context for the test; never {@code null}.
     * @throws Exception
     *             allows any exception to propagate
     */
    void beforeTestClass(CayenneTestContext testContext) throws Exception;

    /**
     * Called to prepares the {@link Object test instance} of the supplied
     * {@link CayenneTestContext test context}, for example by injecting
     * dependencies.
     *
     * @param testContext
     *            the test context for the test (never {@code null}).
     * @throws Exception
     *             allows any exception to propagate
     */
    void prepareTestInstance(CayenneTestContext testContext) throws Exception;

    /**
     * Called <em>before</em> execution of the {@link java.lang.reflect.Method
     * test method} in the supplied {@link CayenneTestContext test context}, for
     * example by setting up test fixtures.
     *
     * @param testContext
     *            the test context in which the test method will be executed
     *            (never {@code null})
     * @throws Exception
     *             allows any exception to propagate
     */
    void beforeTestMethod(CayenneTestContext testContext) throws Exception;

    /**
     * Called <em>after</em> execution of the {@link java.lang.reflect.Method
     * test method} in the supplied {@link CayenneTestContext test context}, for
     * example by tearing down test fixtures.
     *
     * @param testContext
     *            the test context in which the test method was executed (never
     *            {@code null}).
     * @throws Exception
     *             allows any exception to propagate
     */
    void afterTestMethod(CayenneTestContext testContext) throws Exception;

    /**
     * Called <em>after</em> execution of all tests within the class.
     *
     * @param testContext
     *            the test context for the test (never {@code null}).
     * @throws Exception
     *             allows any exception to propagate
     */
    void afterTestClass(CayenneTestContext testContext) throws Exception;

}
