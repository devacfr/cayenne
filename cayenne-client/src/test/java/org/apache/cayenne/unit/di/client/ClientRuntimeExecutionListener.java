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

package org.apache.cayenne.unit.di.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.spi.DefaultScopeProvider;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.testing.CayenneTestContext;
import org.apache.cayenne.testing.support.AbstractTestExecutionListener;
import org.apache.cayenne.testing.utils.Assert;

public class ClientRuntimeExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void prepareTestInstance(CayenneTestContext testContext) throws Exception {
        Injector injector = testContext.getCayenneRuntime().getInjector();
        Provider<ClientCaseProperties> propertiesProvider = injector.getProvider(ClientCaseProperties.class);
        propertiesProvider.get().setRuntimeProperties(getClientRuntimeProperties(testContext.getTestClass()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterTestMethod(CayenneTestContext testContext) throws Exception {
        Injector injector = testContext.getCayenneRuntime().getInjector();
        // TODO [devacfr] replace direct call DefaultScopeProvider with specific
        // scope
        // example
        // TestScope testScope =
        // (TestScope)injector.getScopeBindings().get(TestScopeAnnotation.class);
        // testScope.dirty();
        // clear cached instance in all scope provider, because #shutdown() is
        // not same meaning

        scopedDirty(injector).dirty(ClientCaseProperties.class).dirty(ClientRuntime.class)
                .dirty(Key.get(ObjectContext.class, ClientCase.ROP_CLIENT_KEY)).dirty(CayenneContext.class)
                .dirty(ClientServerChannel.class).dirty(ClientConnection.class);

    }

    /**
     * @return
     */
    private Map<String, String> getClientRuntimeProperties(Class<?> testClass) {
        ClientRuntimeProperty properties = testClass.getAnnotation(ClientRuntimeProperty.class);

        Map<String, String> map = new HashMap<String, String>();
        if (properties != null) {
            String[] pairs = properties.value();
            if (pairs != null && pairs.length > 1) {

                String key = null;

                for (int i = 0; i < pairs.length; i++) {
                    if (i % 2 == 0) {
                        key = pairs[i];
                    } else {
                        map.put(key, pairs[i]);
                    }
                }
            }
        }
        return map;
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

        @SuppressWarnings("unchecked")
        public <T> ScopedDirtyBuilder dirty(Key<T> key) throws Exception {
            // [devacfr] temporary, just to facilitate new DI integration
            Object provider = injector.getProvider(key);
            if (provider instanceof DefaultScopeProvider) {
                ((DefaultScopeProvider<T>) provider).afterScopeEnd();
            }
            return this;
        }
    }
}
