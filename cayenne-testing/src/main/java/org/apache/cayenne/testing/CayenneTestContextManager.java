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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.testing.support.DependencyInjectionTestExecutionListener;
import org.apache.cayenne.testing.support.DirtiesRuntimeTestExecutionListener;
import org.apache.cayenne.testing.utils.Annotations;
import org.apache.cayenne.testing.utils.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @since 3.2
 *
 */
public class CayenneTestContextManager {

    private static final Class<?>[] DEFAULT_TEST_EXECUTION_LISTENER_CLASS_NAMES = {
            DependencyInjectionTestExecutionListener.class, DirtiesRuntimeTestExecutionListener.class };

    private static final Log logger = LogFactory.getLog(CayenneTestContextManager.class);

    static final ContextCache contextCache = new ContextCache();

    private final CayenneTestContext testContext;

    private final List<TestExecutionListener> testExecutionListeners = new ArrayList<TestExecutionListener>();

    public CayenneTestContextManager(Class<?> testClass) {
        this.testContext = new CayenneTestContext(testClass, contextCache);
        registerTestExecutionListeners(retrieveTestExecutionListeners(testClass));
    }

    protected final CayenneTestContext getTestContext() {
        return this.testContext;
    }

    public void registerTestExecutionListeners(TestExecutionListener... testExecutionListeners) {
        for (TestExecutionListener listener : testExecutionListeners) {
            if (logger.isTraceEnabled()) {
                logger.trace("Registering TestExecutionListener: " + listener);
            }
            this.testExecutionListeners.add(listener);
        }
    }

    public final List<TestExecutionListener> getTestExecutionListeners() {
        return this.testExecutionListeners;
    }

    private List<TestExecutionListener> getReversedTestExecutionListeners() {
        List<TestExecutionListener> listenersReversed = new ArrayList<TestExecutionListener>(
                getTestExecutionListeners());
        Collections.reverse(listenersReversed);
        return listenersReversed;
    }

    private TestExecutionListener[] retrieveTestExecutionListeners(Class<?> clazz) {
        Class<TestExecutionListeners> annotationType = TestExecutionListeners.class;
        List<Class<? extends TestExecutionListener>> classesList = new ArrayList<Class<? extends TestExecutionListener>>();
        Class<?> declaringClass = Annotations.findAnnotationDeclaringClass(annotationType, clazz);
        boolean defaultListeners = false;

        // Use defaults?
        if (declaringClass == null) {
            if (logger.isInfoEnabled()) {
                logger.info("@TestExecutionListeners is not present for class [" + clazz + "]: using defaults.");
            }
            classesList.addAll(getDefaultTestExecutionListenerClasses());
            defaultListeners = true;
        } else {
            // Traverse the class hierarchy...
            while (declaringClass != null) {
                TestExecutionListeners testExecutionListeners = declaringClass.getAnnotation(annotationType);
                if (logger.isTraceEnabled()) {
                    logger.trace("Retrieved @TestExecutionListeners [" + testExecutionListeners
                            + "] for declaring class [" + declaringClass + "].");
                }

                Class<? extends TestExecutionListener>[] valueListenerClasses = testExecutionListeners.value();
                Class<? extends TestExecutionListener>[] listenerClasses = testExecutionListeners.listeners();
                if (!isArrayEmpty(valueListenerClasses) && !isArrayEmpty(listenerClasses)) {
                    String msg = String.format(
                            "Test class [%s] has been configured with @TestExecutionListeners' 'value' [%s] "
                                    + "and 'listeners' [%s] attributes. Use one or the other, but not both.",
                            declaringClass, nullSafeToString(valueListenerClasses), nullSafeToString(listenerClasses));
                    logger.error(msg);
                    throw new IllegalStateException(msg);
                } else if (!isArrayEmpty(valueListenerClasses)) {
                    listenerClasses = valueListenerClasses;
                }

                if (listenerClasses != null) {
                    classesList.addAll(0, Arrays.<Class<? extends TestExecutionListener>> asList(listenerClasses));
                }
                declaringClass = (testExecutionListeners.inheritListeners() ? Annotations.findAnnotationDeclaringClass(
                        annotationType, declaringClass.getSuperclass()) : null);
            }
        }

        List<TestExecutionListener> listeners = new ArrayList<TestExecutionListener>(classesList.size());
        for (Class<? extends TestExecutionListener> listenerClass : classesList) {
            try {
                listeners.add(instantiateClass(listenerClass));
            } catch (NoClassDefFoundError err) {
                if (defaultListeners) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Could not instantiate default TestExecutionListener class ["
                                + listenerClass.getName()
                                + "]. Specify custom listener classes or make the default listener classes available.");
                    }
                } else {
                    throw err;
                }
            }
        }
        return listeners.toArray(new TestExecutionListener[listeners.size()]);
    }

    protected Set<Class<? extends TestExecutionListener>> getDefaultTestExecutionListenerClasses() {
        Set<Class<? extends TestExecutionListener>> defaultListenerClasses = new LinkedHashSet<Class<? extends TestExecutionListener>>();
        for (Class<?> clazz : DEFAULT_TEST_EXECUTION_LISTENER_CLASS_NAMES) {
            try {
                defaultListenerClasses.add((Class<? extends TestExecutionListener>) clazz);
            } catch (Throwable ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Could not load default TestExecutionListener class [" + clazz.getSimpleName()
                            + "]. Specify custom listener classes or make the default listener classes available.");
                }
            }
        }
        return defaultListenerClasses;
    }

    public void beforeTestClass() throws Exception {
        final Class<?> testClass = getTestContext().getTestClass();
        if (logger.isTraceEnabled()) {
            logger.trace("beforeTestClass(): class [" + testClass + "]");
        }
        getTestContext().updateState(null, null, null);

        for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
            try {
                testExecutionListener.beforeTestClass(getTestContext());
            } catch (Exception ex) {
                logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener
                        + "] to process 'before class' callback for test class [" + testClass + "]", ex);
                throw ex;
            }
        }
    }

    public void prepareTestInstance(Object testInstance) throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace("prepareTestInstance(): instance [" + testInstance + "]");
        }
        getTestContext().updateState(testInstance, null, null);

        for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace("prepareTestInstance(): instance [" + testInstance + "], executionListener ["
                            + testExecutionListener.toString() + "]");
                }
                testExecutionListener.prepareTestInstance(getTestContext());
            } catch (Exception ex) {
                logger.error("Caught exception while allowing TestExecutionListener [" + testExecutionListener
                        + "] to prepare test instance [" + testInstance + "]", ex);
                throw ex;
            }
        }
    }

    public void beforeTestMethod(Object testInstance, Method testMethod) throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace("beforeTestMethod(): instance [" + testInstance + "], method [" + testMethod + "]");
        }
        getTestContext().updateState(testInstance, testMethod, null);

        for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace("beforeTestMethod(): instance [" + testInstance + "], executionListener ["
                            + testExecutionListener.toString() + "]");
                }
                testExecutionListener.beforeTestMethod(getTestContext());
            } catch (Exception ex) {
                logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener
                        + "] to process 'before' execution of test method [" + testMethod + "] for test instance ["
                        + testInstance + "]", ex);
                throw ex;
            }
        }
    }

    public void afterTestMethod(Object testInstance, Method testMethod, Throwable exception) throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace("afterTestMethod(): instance [" + testInstance + "], method [" + testMethod + "], exception ["
                    + exception + "]");
        }
        getTestContext().updateState(testInstance, testMethod, exception);

        Exception afterTestMethodException = null;
        // Traverse the TestExecutionListeners in reverse order to ensure proper
        // "wrapper"-style execution of listeners.
        for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace("afterTestMethod(): instance [" + testInstance + "], executionListener ["
                            + testExecutionListener.toString() + "]");
                }
                testExecutionListener.afterTestMethod(getTestContext());
            } catch (Exception ex) {
                logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener
                        + "] to process 'after' execution for test: method [" + testMethod + "], instance ["
                        + testInstance + "], exception [" + exception + "]", ex);
                if (afterTestMethodException == null) {
                    afterTestMethodException = ex;
                }
            }
        }
        if (afterTestMethodException != null) {
            throw afterTestMethodException;
        }
    }

    public void afterTestClass() throws Exception {
        final Class<?> testClass = getTestContext().getTestClass();
        if (logger.isTraceEnabled()) {
            logger.trace("afterTestClass(): class [" + testClass + "]");
        }
        getTestContext().updateState(null, null, null);

        Exception afterTestClassException = null;
        // Traverse the TestExecutionListeners in reverse order to ensure proper
        // "wrapper"-style execution of listeners.
        for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
            try {
                testExecutionListener.afterTestClass(getTestContext());
            } catch (Exception ex) {
                logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener
                        + "] to process 'after class' callback for test class [" + testClass + "]", ex);
                if (afterTestClassException == null) {
                    afterTestClassException = ex;
                }
            }
        }
        if (afterTestClassException != null) {
            throw afterTestClassException;
        }
    }

    static <T> T instantiateClass(Class<T> clazz) throws RuntimeException {
        if (clazz.isInterface()) {
            throw new RuntimeException("Specified class is an interface");
        }
        try {
            return instantiateClass(clazz.getDeclaredConstructor());
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("No default constructor found", ex);
        }
    }

    static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws RuntimeException {
        Assert.notNull(ctor, "Constructor must not be null");
        try {
            makeAccessible(ctor);
            return ctor.newInstance(args);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    static boolean isArrayEmpty(Object[] array) {
        return (array == null || array.length == 0);
    }

    static String nullSafeToString(Object[] array) {
        if (array == null) {
            return "null";
        }
        int length = array.length;
        if (length == 0) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i == 0) {
                sb.append("{");
            } else {
                sb.append(", ");
            }
            sb.append(String.valueOf(array[i]));
        }
        sb.append("}");
        return sb.toString();
    }

    static void makeAccessible(Constructor<?> ctor) {
        if ((!Modifier.isPublic(ctor.getModifiers()) || !Modifier.isPublic(ctor.getDeclaringClass().getModifiers()))
                && !ctor.isAccessible()) {
            ctor.setAccessible(true);
        }
    }

}