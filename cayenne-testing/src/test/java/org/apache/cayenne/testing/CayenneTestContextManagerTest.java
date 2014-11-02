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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cayenne.testing.support.AbstractTestExecutionListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.style.ToStringCreator;

/**
 * unit test verifies proper <em>execution order</em> of registered
 * {@link TestExecutionListener TestExecutionListeners}.
 *
 * @since 4.0
 *
 */
public class CayenneTestContextManagerTest extends TestCase {

    private static final String FIRST = "first";

    private static final String SECOND = "second";

    private static final String THIRD = "third";

    private static final List<String> afterTestMethodCalls = new ArrayList<String>();

    private static final List<String> beforeTestMethodCalls = new ArrayList<String>();

    protected static final Log logger = LogFactory.getLog(CayenneTestContextManagerTest.class);

    private final CayenneTestContextManager testContextManager = new CayenneTestContextManager(ExampleTestCase.class);

    private Method getTestMethod() throws NoSuchMethodException {
        return ExampleTestCase.class.getDeclaredMethod("exampleTestMethod", (Class<?>[]) null);
    }

    /**
     * Asserts the <em>execution order</em> of 'before' and 'after' test method
     * calls on {@link TestExecutionListener listeners} registered for the
     * configured {@link TestContextManager}.
     *
     * @see #beforeTestMethodCalls
     * @see #afterTestMethodCalls
     */
    private static void assertExecutionOrder(List<String> expectedBeforeTestMethodCalls,
                                             List<String> expectedAfterTestMethodCalls, final String usageContext) {

        if (expectedBeforeTestMethodCalls == null) {
            expectedBeforeTestMethodCalls = new ArrayList<String>();
        }
        if (expectedAfterTestMethodCalls == null) {
            expectedAfterTestMethodCalls = new ArrayList<String>();
        }

        if (logger.isDebugEnabled()) {
            for (String listenerName : beforeTestMethodCalls) {
                logger.debug("'before' listener [" + listenerName + "] (" + usageContext + ").");
            }
            for (String listenerName : afterTestMethodCalls) {
                logger.debug("'after' listener [" + listenerName + "] (" + usageContext + ").");
            }
        }

        assertTrue("Verifying execution order of 'before' listeners' (" + usageContext + ").",
            expectedBeforeTestMethodCalls.equals(beforeTestMethodCalls));
        assertTrue("Verifying execution order of 'after' listeners' (" + usageContext + ").",
            expectedAfterTestMethodCalls.equals(afterTestMethodCalls));
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        beforeTestMethodCalls.clear();
        afterTestMethodCalls.clear();
        assertExecutionOrder(null, null, "BeforeClass");
    }

    /**
     * Verifies the expected {@link TestExecutionListener}
     * <em>execution order</em> after all test methods have completed.
     */
    @AfterClass
    public static void verifyListenerExecutionOrderAfterClass() throws Exception {
        assertExecutionOrder(Arrays.<String> asList(FIRST, SECOND, THIRD),
            Arrays.<String> asList(THIRD, SECOND, FIRST),
            "AfterClass");
    }

    @Before
    public void setUpTestContextManager() throws Throwable {
        assertEquals("Verifying the number of registered TestExecutionListeners.",
            3,
            this.testContextManager.getTestExecutionListeners().size());

        this.testContextManager.beforeTestMethod(new ExampleTestCase(), getTestMethod());
    }

    /**
     * Verifies the expected {@link TestExecutionListener}
     * <em>execution order</em> within a test method.
     *
     * @see #verifyListenerExecutionOrderAfterClass()
     */
    @Test
    public void verifyListenerExecutionOrderWithinTestMethod() {
        assertExecutionOrder(Arrays.<String> asList(FIRST, SECOND, THIRD), null, "Test");
    }

    @After
    public void tearDownTestContextManager() throws Throwable {
        this.testContextManager.afterTestMethod(new ExampleTestCase(), getTestMethod(), null);
    }

    @TestExecutionListeners({ FirstListener.class, SecondListener.class, ThirdListener.class })
    private static class ExampleTestCase {

        @SuppressWarnings("unused")
        public void exampleTestMethod() {
            assertTrue(true);
        }
    }

    private static class BaseTestExecutionListener extends AbstractTestExecutionListener {

        private final String name;

        public BaseTestExecutionListener(final String name) {
            this.name = name;
        }

        @Override
        public void beforeTestMethod(final CayenneTestContext testContext) {
            beforeTestMethodCalls.add(this.name);
        }

        @Override
        public void afterTestMethod(final CayenneTestContext testContext) {
            afterTestMethodCalls.add(this.name);
        }

        @Override
        public String toString() {
            return new ToStringCreator(this).append("name", this.name).toString();
        }
    }

    private static class FirstListener extends BaseTestExecutionListener {

        public FirstListener() {
            super(FIRST);
        }
    }

    private static class SecondListener extends BaseTestExecutionListener {

        public SecondListener() {
            super(SECOND);
        }
    }

    private static class ThirdListener extends BaseTestExecutionListener {

        public ThirdListener() {
            super(THIRD);
        }
    }

}