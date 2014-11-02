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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.List;

import org.apache.cayenne.testing.statement.RunAfterTestClassCallbacks;
import org.apache.cayenne.testing.statement.RunAfterTestMethodCallbacks;
import org.apache.cayenne.testing.statement.RunBeforeTestClassCallbacks;
import org.apache.cayenne.testing.statement.RunBeforeTestMethodCallbacks;
import org.apache.cayenne.testing.utils.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.mockito.asm.tree.analysis.Value;

/**
 *
 * @since 4.0
 *
 */
public class CayenneBlockJUnit4ClassRunner extends BlockJUnit4ClassRunner {

    private static final Log logger = LogFactory.getLog(CayenneBlockJUnit4ClassRunner.class);

    private final CayenneTestContextManager testContextManager;

    public CayenneBlockJUnit4ClassRunner(final Class<?> clazz) throws InitializationError {
        this(clazz, null);
    }

    protected CayenneBlockJUnit4ClassRunner(final Class<?> clazz, final CayenneTestContextManager testContextManager)
            throws InitializationError {
        super(clazz);
        if (logger.isDebugEnabled()) {
            logger.debug("CayenneBlockJUnit4ClassRunner constructor called with [" + clazz + "].");
        }
        if (testContextManager == null) {
            this.testContextManager = createTestContextManager(clazz);
        } else {
            this.testContextManager = testContextManager;
        }
    }

    protected CayenneTestContextManager createTestContextManager(Class<?> clazz) {
        return new CayenneTestContextManager(clazz);
    }

    protected final CayenneTestContextManager getTestContextManager() {
        return this.testContextManager;
    }

    @Override
    protected Statement withBeforeClasses(Statement statement) {
        Statement junitBeforeClasses = super.withBeforeClasses(statement);
        return new RunBeforeTestClassCallbacks(junitBeforeClasses, getTestContextManager());
    }

    @Override
    protected Statement withAfterClasses(Statement statement) {
        Statement junitAfterClasses = super.withAfterClasses(statement);
        return new RunAfterTestClassCallbacks(junitAfterClasses, getTestContextManager());
    }

    @Override
    protected Object createTest() throws Exception {
        Object testInstance = super.createTest();
        getTestContextManager().prepareTestInstance(testInstance);
        return testInstance;
    }

    /**
     * Performs the same logic as
     * {@link BlockJUnit4ClassRunner#runChild(FrameworkMethod, RunNotifier)},
     * except that tests are determined to be <em>ignored</em> by
     * {@link #isTestMethodIgnored(FrameworkMethod)}.
     */
    @Override
    protected void runChild(FrameworkMethod frameworkMethod, RunNotifier notifier) {
        EachTestNotifier eachNotifier = makeNotifier(frameworkMethod, notifier);
        if (isTestMethodIgnored(frameworkMethod)) {
            eachNotifier.fireTestIgnored();
            return;
        }

        eachNotifier.fireTestStarted();
        try {
            methodBlock(frameworkMethod).evaluate();
        } catch (AssumptionViolatedException e) {
            eachNotifier.addFailedAssumption(e);
        } catch (Throwable e) {
            eachNotifier.addFailure(e);
        } finally {
            eachNotifier.fireTestFinished();
        }
    }

    private EachTestNotifier makeNotifier(FrameworkMethod method, RunNotifier notifier) {
        Description description = describeChild(method);
        return new EachTestNotifier(notifier, description);
    }

    @Override
    protected Statement methodBlock(FrameworkMethod frameworkMethod) {
        Object testInstance;
        try {
            testInstance = new ReflectiveCallable() {

                @Override
                protected Object runReflectiveCall() throws Throwable {
                    return createTest();
                }
            }.run();
        } catch (Throwable ex) {
            return new Fail(ex);
        }

        Statement statement = methodInvoker(frameworkMethod, testInstance);
        statement = possiblyExpectingExceptions(frameworkMethod, testInstance, statement);
        statement = withBefores(frameworkMethod, testInstance, statement);
        statement = withAfters(frameworkMethod, testInstance, statement);
        statement = withRules(frameworkMethod, testInstance, statement);
        statement = withPotentialTimeout(frameworkMethod, testInstance, statement);

        return statement;
    }

    private Statement withRules(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
        List<TestRule> testRules = getTestRules(testInstance);
        Statement result = statement;
        result = withMethodRules(frameworkMethod, testRules, testInstance, result);
        result = withTestRules(frameworkMethod, testRules, result);

        return result;
    }

    private Statement
            withMethodRules(FrameworkMethod method, List<TestRule> testRules, Object target, Statement result) {
        for (org.junit.rules.MethodRule each : rules(target)) {
            if (!testRules.contains(each)) {
                result = each.apply(result, method, target);
            }
        }
        return result;
    }

    /**
     * Returns a {@link Statement}: apply all non-static {@link Value} fields
     * annotated with {@link Rule}.
     *
     * @param statement
     *            The base statement
     * @return a RunRules statement if any class-level {@link Rule}s are found,
     *         or the base statement
     */
    private Statement withTestRules(FrameworkMethod method, List<TestRule> testRules, Statement statement) {
        return testRules.isEmpty() ? statement : new RunRules(statement, testRules, describeChild(method));
    }

    protected boolean isTestMethodIgnored(FrameworkMethod frameworkMethod) {
        Method method = frameworkMethod.getMethod();
        return method.isAnnotationPresent(Ignore.class);
    }

    @Override
    protected Statement withBefores(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
        Statement junitBefores = super.withBefores(frameworkMethod, testInstance, statement);
        return new RunBeforeTestMethodCallbacks(junitBefores, testInstance, frameworkMethod.getMethod(),
            getTestContextManager());
    }

    @Override
    protected Statement withAfters(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
        Statement junitAfters = super.withAfters(frameworkMethod, testInstance, statement);
        return new RunAfterTestMethodCallbacks(junitAfters, testInstance, frameworkMethod.getMethod(),
            getTestContextManager());
    }

    static Method findMethod(Class<?> clazz, String name) {
        return findMethod(clazz, name, new Class[0]);
    }

    static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Assert.notNull(clazz, "Class must not be null");
        Assert.notNull(name, "Method name must not be null");
        Class<?> searchType = clazz;
        while (searchType != null) {
            Method[] methods = searchType.isInterface() ? searchType.getMethods() : searchType.getDeclaredMethods();
            for (Method method : methods) {
                if (name.equals(method.getName())
                        && (paramTypes == null || Arrays.equals(paramTypes, method.getParameterTypes()))) {
                    return method;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    static void makeAccessible(Method method) {
        if ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers()))
                && !method.isAccessible()) {
            method.setAccessible(true);
        }
    }

    static Object invokeMethod(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (Exception ex) {
            handleReflectionException(ex);
        }
        throw new IllegalStateException("Should never get here");
    }

    static void handleReflectionException(Exception ex) {
        if (ex instanceof NoSuchMethodException) {
            throw new IllegalStateException("Method not found: " + ex.getMessage());
        }
        if (ex instanceof IllegalAccessException) {
            throw new IllegalStateException("Could not access method: " + ex.getMessage());
        }
        if (ex instanceof InvocationTargetException) {
            handleInvocationTargetException((InvocationTargetException) ex);
        }
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        throw new UndeclaredThrowableException(ex);
    }

    static void handleInvocationTargetException(InvocationTargetException exception) {
        Throwable ex = exception.getTargetException();
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        if (ex instanceof Error) {
            throw (Error) ex;
        }
        throw new UndeclaredThrowableException(ex);

    }

}