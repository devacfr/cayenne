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

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.testing.support.CayenneRuntimeInvoker;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for verifying proper behavior of the {@link ContextCache}
 * in conjunction with cache keys used in {@link CayenneTestContext}.
 *
 */
public class ContextCacheTest extends TestCase {

    private ContextCache contextCache = new ContextCache();

    @Before
    public void initialCacheState() {
        assertContextCacheStatistics(contextCache, "initial state", 0, 0, 0);
    }

    private CayenneRuntimeInvoker loadContext(Class<?> testClass) {
        CayenneTestContext testContext = new CayenneTestContext(testClass, contextCache) {
            /**
             * {@inheritDoc}
             */
            @Override
            protected CayenneRuntimeInvoker createCayenneRuntime() throws Exception {
                List<Module> modules = new ArrayList<Module>();

                modules.add(new Module() {

                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    @Override
                    public void configure(Binder binder) {
                        Class clCayenne = CayenneRuntimeStub.class;
                        binder.bind(clCayenne).toProviderInstance(createCayenneRuntimeProvider()).withoutScope();

                    }
                });
                return new CayenneRuntimeInvoker(new CayenneRuntimeStub(modules));
            }
        };
        return testContext.getCayenneRuntime();
    }

    private void loadCtxAndAssertStats(Class<?> testClass, int expectedSize, int expectedHitCount, int expectedMissCount) {
        assertNotNull(loadContext(testClass));
        assertContextCacheStatistics(contextCache, testClass.getName(), expectedSize, expectedHitCount,
                expectedMissCount);
    }

    @Test
    public void verifyCacheKey() {
        loadCtxAndAssertStats(Foo.class, 1, 0, 1);
        loadCtxAndAssertStats(Foo.class, 1, 1, 1);
        loadCtxAndAssertStats(Bar.class, 2, 1, 2);
        loadCtxAndAssertStats(Bar.class, 2, 2, 2);
        loadCtxAndAssertStats(Foo.class, 2, 3, 2);
        loadCtxAndAssertStats(Bar.class, 2, 4, 2);
    }

    @CayenneConfiguration("foo-map-file.xml")
    static class Foo {

    }

    @CayenneConfiguration("bar-map-file.xml")
    static class Bar {

    }

    static class CayenneRuntimeStub {

        protected Injector injector;
        protected Module[] modules;

        public CayenneRuntimeStub(List<Module> modules) {

            if (modules == null) {
                this.modules = new Module[0];
            } else {
                this.modules = modules.toArray(new Module[modules.size()]);
            }

            this.injector = DIBootstrap.createInjector(this.modules);
        }

        public Injector getInjector() {
            return injector;
        }

        public void shutdown() {
            injector.shutdown();
        }
    }

    /**
     * Asserts the statistics of the supplied context cache.
     *
     * @param contextCache
     *            the cache to assert against
     * @param usageScenario
     *            the scenario in which the statistics are used
     * @param expectedSize
     *            the expected number of contexts in the cache
     * @param expectedHitCount
     *            the expected hit count
     * @param expectedMissCount
     *            the expected miss count
     */
    public static final void assertContextCacheStatistics(ContextCache contextCache, String usageScenario,
            int expectedSize, int expectedHitCount, int expectedMissCount) {

        assertEquals("Verifying number of contexts in cache (" + usageScenario + ").", expectedSize,
                contextCache.size());
        assertEquals("Verifying number of cache hits (" + usageScenario + ").", expectedHitCount,
                contextCache.getHitCount());
        assertEquals("Verifying number of cache misses (" + usageScenario + ").", expectedMissCount,
                contextCache.getMissCount());
    }

}