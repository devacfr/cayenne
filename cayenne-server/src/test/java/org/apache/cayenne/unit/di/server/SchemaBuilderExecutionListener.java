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

package org.apache.cayenne.unit.di.server;

import java.lang.reflect.Method;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.spi.DefaultInjector;
import org.apache.cayenne.di.spi.DefaultScopeProvider;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.testing.CayenneTestContext;
import org.apache.cayenne.testing.support.AbstractTestExecutionListener;
import org.apache.cayenne.testing.support.CayenneRuntimeInvoker;
import org.apache.cayenne.testing.utils.Assert;

/**
 * <code>SchemaBuilderExecutionListener</code> allows rebuilding the database
 * schema before execution test class and restore the cayenne runtime context
 * with initial value.
 *
 * @since 3.2
 */
public class SchemaBuilderExecutionListener extends AbstractTestExecutionListener {

    /**
     * rebuild the database schema before execution each test class.
     */
    @Override
    public void beforeTestClass(CayenneTestContext testContext) throws Exception {
        CayenneRuntimeInvoker invoker = testContext.getCayenneRuntime();
        SchemaBuilder schemaBuilder = invoker.getInjector().getInstance(SchemaBuilder.class);
        schemaBuilder.rebuildSchema();
    }

    /**
     * Forces the restore of context with initial value after test call.
     */
    @Override
    public void afterTestMethod(CayenneTestContext testContext) throws Exception {

        Injector injector = testContext.getCayenneRuntime().getInjector();
        // TODO [devacfr] replace direct call DefaultScopeProvider with specific
        // scope
        // example
        // TestScope testScope =
        // (TestScope)injector.getScopeBindings().get(TestScopeAnnotation.class);
        // testScope.dirty(); // clear cached instance in all scope provider

        scopedDirty(injector).dirty(EntityResolver.class).dirty(ObjectContextFactory.class).dirty(DataDomain.class)
                .dirty(DataChannel.class);

        injector.getInstance(QueryCache.class).clear();
    }

    public ScopedDirtyBuilder scopedDirty(Injector injector) {
        return new ScopedDirtyBuilder(injector);
    }

    static class ScopedDirtyBuilder {

        private final Injector injector;

        public ScopedDirtyBuilder(final Injector injector) {
            this.injector = Assert.notNull(injector);
        }

        public <T> ScopedDirtyBuilder dirty(Class<T> instanceClass) throws Exception {
            return dirty(Key.get(instanceClass));
        }

        public <T> ScopedDirtyBuilder dirty(Key<T> key) throws Exception {
            // [devacfr] temporary, just to facilitate new DI integration
            Object provider = injector.getProvider(key);
            if (provider instanceof DefaultScopeProvider) {
                try {
                    Method afterScopeMethod = provider.getClass().getMethod("afterScopeEnd");
                    afterScopeMethod.invoke(provider);
                } catch (NoSuchMethodException ex) {
                    Method afterEndMethod = provider.getClass().getMethod("afterEndScope", new Class[] {DefaultInjector.class});
                    afterEndMethod.invoke(provider, injector);
                }
            }
            return this;
        }
    }

}
