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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.testing.support.CayenneRuntimeInvoker;
import org.apache.cayenne.testing.utils.Annotations;
import org.apache.cayenne.testing.utils.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>TestContext</code> contains all information in which a test is
 * executed.
 *
 * @since 4.0
 */
public class CayenneTestContext {

    public final static String CLASS_CAYENNE_RUNTIME_NAME = "org.apache.cayenne.configuration.CayenneRuntime";

    public final static String CLASS_SERVER_RUNTIME_NAME = "org.apache.cayenne.configuration.server.ServerRuntime";

    /**
     * Log instance
     */
    private static final Log logger = LogFactory.getLog(CayenneTestContext.class);

    /**
     * cache all cayenne runtime in relation with xml context file.
     */
    private final ContextCache contextCache;

    /**
     * the current configuration location.
     */
    private String configurationLocation;

    /**
     * All DI modules to load.
     */
    private Class<? extends Module>[] modules = null;

    /**
     * the current executed test class.
     */
    private final Class<?> testClass;

    /**
     * the instance of current executing test
     */
    private Object testInstance;

    /**
     * the current executing test method.
     */
    private Method testMethod;

    /**
     * current exception that was thrown during test execution.
     */
    private Throwable testException;

    /**
     * Map with String keys and Object values.
     */
    private final Map<String, Object> attributes = new LinkedHashMap<String, Object>(0);

    /**
     * Construct a new test context for the supplied {@link Class test class}
     * and {@link ContextCache context cache} and parse the corresponding
     * {@link CayenneConfiguration &#064;CayenneContextConfiguration}
     * annotation, if present.
     *
     * @param testClass
     *            the test class for which the test context should be
     *            constructed (must not be <code>null</code>)
     * @param contextCache
     *            the context cache from which the constructed test context
     *            should retrieve application contexts (must not be
     *            <code>null</code>)
     */
    CayenneTestContext(Class<?> testClass, ContextCache contextCache) {
        Assert.notNull(testClass, "TestClass is required");
        Assert.notNull(contextCache, "contextCache is required");
        String annotationConfigName = CayenneConfiguration.class.getSimpleName();
        CayenneConfiguration runtimeConfiguration = testClass.getAnnotation(CayenneConfiguration.class);

        if (runtimeConfiguration == null) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("@%s not found for class [%s]", annotationConfigName, testClass));
            }
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Retrieved @%s [%s] for class [%s]",
                    annotationConfigName,
                    runtimeConfiguration,
                    testClass));
            }
            this.configurationLocation = runtimeConfiguration.value();
            Assert.notNull(configurationLocation, "Can not load an CayenneRuntime with a NULL 'locations'. "
                    + "Consider annotating your test class with @CayenneContextConfiguration.");

            Modules moduleContext = Annotations.findAnnotation(testClass, Modules.class);
            if (moduleContext != null) {
                this.modules = moduleContext.value();
            }
        }

        this.contextCache = contextCache;
        this.testClass = testClass;
    }

    /**
     * Create an <code>CayenneRuntime</code> for this test context using the
     * configuration file.
     *
     * @throws Exception
     *             if an error occurs while creating the Cayenne runtime.
     */
    protected CayenneRuntimeInvoker createCayenneRuntime() throws Exception {
        List<Module> modules = new ArrayList<Module>();
        if (this.modules != null) {
            for (Class<? extends Module> module : this.modules) {
                Module m = CayenneTestContextManager.instantiateClass(module);
                modules.add(m);
            }
        }

        modules.add(new Module() {

            @Override
            public void configure(Binder binder) {
                try {
                    // TODO [devacfr] try with only one provider
                    Class clCayenne = Class.forName(CLASS_CAYENNE_RUNTIME_NAME);
                    binder.bind(clCayenne).toProviderInstance(createCayenneRuntimeProvider()).withoutScope();
                    Class clServer = Class.forName(CLASS_SERVER_RUNTIME_NAME);
                    binder.bind(clServer).toProviderInstance(createCayenneRuntimeProvider()).withoutScope();
                } catch (ClassNotFoundException ex) {
                    throw new RuntimeException(ex.getMessage(), ex);
                }

            }
        });
        Class<?> cl = Class.forName(CLASS_SERVER_RUNTIME_NAME);
        Constructor<?> ctr = cl.getConstructor(String.class, Module[].class);

        CayenneRuntimeInvoker cayenneRuntime =
                new CayenneRuntimeInvoker(ctr.newInstance(configurationLocation,
                    modules.toArray(new Module[modules.size()])));
        return cayenneRuntime;
    }

    /**
     * create new instance of {@link CayenneRuntimeProvider} associated to
     * {@link ContextCache} and the current configuration file
     *
     * @return Returns new instance of {@link CayenneRuntimeProvider};
     */
    protected CayenneRuntimeProvider<CayenneRuntimeInvoker> createCayenneRuntimeProvider() {
        return new CayenneRuntimeProvider<CayenneRuntimeInvoker>(CayenneTestContext.this.contextCache,
                CayenneTestContext.this.configurationLocation);
    }

    /**
     * Gets the {@link CayenneRuntime Cayenne runtime} for this test context,
     * possibly cached.
     *
     * @return Returns the cayenne runtime.
     * @throws IllegalStateException
     *             if an error occurs while retrieving the cayenne runtime
     */
    public CayenneRuntimeInvoker getCayenneRuntime() {
        synchronized (contextCache) {
            CayenneRuntimeInvoker runtime = contextCache.get(configurationLocation);
            if (runtime == null) {
                try {
                    runtime = createCayenneRuntime();
                    if (runtime == null) {
                        throw new IllegalStateException("Failed to create CayenneRuntime");
                    }
                    contextCache.put(configurationLocation, runtime);
                } catch (Exception ex) {
                    throw new IllegalStateException("Failed to create CayenneRuntime", ex);
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Retrieved CayenneRuntime for test class [%s] from cache with key [%s].",
                        testClass,
                        configurationLocation));
                }
            }
            return runtime;
        }
    }

    /**
     * Gets the {@link Class test class} for this test context.
     *
     * @return Returns the test class (never <code>null</code>)
     */
    public final Class<?> getTestClass() {
        return testClass;
    }

    /**
     * Gets the current {@link Object test instance} for this test context.
     * <p>
     * Note: this is a mutable property.
     * </p>
     *
     * @return Returns the current test instance (may be <code>null</code>)
     * @see #updateState(Object,Method,Throwable)
     */
    public final Object getTestInstance() {
        return testInstance;
    }

    /**
     * Gets the current {@link Method test method} for this test context.
     * <p>
     * Note: this is a mutable property.
     * </p>
     *
     * @return the current test method (may be <code>null</code>)
     * @see #updateState(Object, Method, Throwable)
     */
    public final Method getTestMethod() {
        return testMethod;
    }

    /**
     * Gets the {@link Throwable exception} that was thrown during execution of
     * the {@link #getTestMethod() test method}.
     *
     * @return Returns the exception that was thrown, or <code>null</code> if no
     *         exception was thrown
     * @see #updateState(Object, Method, Throwable)
     */
    public final Throwable getTestException() {
        return testException;
    }

    /**
     * This method allows invalidating the current Cayenne runtime associated
     * with this test context is <em>dirty</em> and should be reloaded. Do this
     * if a test has modified the context (for example, by reverting the default
     * configuration).
     */
    public void markCayenneRuntimeDirty() {
        synchronized (contextCache) {
            contextCache.setDirty(this.configurationLocation);
        }
    }

    /**
     * Update this test context to reflect the state of the currently executing
     * test.
     *
     * @param testInstance
     *            the current test instance (may be <code>null</code>)
     * @param testMethod
     *            the current test method (may be <code>null</code>)
     * @param testException
     *            the exception that was thrown in the test method, or
     *            <code>null</code> if no exception was thrown
     */
    void updateState(Object testInstance, Method testMethod, Throwable testException) {
        this.testInstance = testInstance;
        this.testMethod = testMethod;
        this.testException = testException;
    }

    /**
     * Sets the value for named attribute.
     *
     * @param name
     *            the name of attribute (must not be <code>null</code> or empty)
     * @param value
     *            the value associated (may be <code>null</code>)
     */
    public void setAttribute(String name, Object value) {
        Assert.hasText(name, "name is required");
        if (value != null) {
            this.attributes.put(name, value);
        } else {
            removeAttribute(name);
        }
    }

    /**
     * Gets the value associated to name.
     *
     * @param name
     *            the name of attribute (must not be <code>null</code> or empty)
     * @return Returns the value associated to name (may be <code>null</code>)
     */
    public Object getAttribute(String name) {
        return this.attributes.get(Assert.hasText(name, "name is required"));
    }

    /**
     * Removes the attribute with name if it is present.
     *
     * @param name
     * @return Returns the value to which associated name, or <code>null</code>
     *         if the attribute doesn't exist.
     */
    public Object removeAttribute(String name) {
        return this.attributes.remove(name);
    }

    /**
     * Gets indicating whether contains attribute for the specified name.
     *
     * @param name
     *            name of attribute to test existing.
     * @return Returns <code>true</code> whether contains attribute for the
     *         specified name, otherwise <code>false</code>.
     */
    public boolean hasAttribute(String name) {
        return this.attributes.containsKey(name);
    }

    /**
     * Gets all attribute names
     *
     * @return Returns a {@link Array array} containing all attribute names.
     */
    public String[] attributeNames() {
        return this.attributes.keySet().toArray(new String[this.attributes.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return new StringBuilder(this.getClass().getName()).append(':')
                .append("testClass[" + testClass)
                .append("],")
                .append("testInstance[" + testInstance)
                .append("],")
                .append("testMethod[" + testMethod)
                .append("],")
                .append("testException[" + testException)
                .append("],")
                .append("configurationLocation[" + configurationLocation)
                .append("]")
                .toString();
    }

    /**
     * An object capable of providing instance of Cayenne Runtime form
     * {@link ContextCache}. This provider allows to inject a instance of
     * current cayenne runtime for a specific test
     *
     * @param <T>
     *            the type of Cayenne runtime supported
     * @see CayenneTestContext
     */
    private static class CayenneRuntimeProvider<T> implements org.apache.cayenne.di.Provider<T> {

        private final ContextCache contextCache;

        private final String location;

        public CayenneRuntimeProvider(final ContextCache contextCache, final String location) {
            this.contextCache = contextCache;
            this.location = location;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T get() {
            return (T) contextCache.get(location).getInternalRuntime();
        }
    }

}
