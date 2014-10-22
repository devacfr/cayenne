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
package org.apache.cayenne.di.spi;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.testing.TestCase;
import org.junit.Test;

/**
 * Test different methods to call a provider or a specific provider.
 *
 * @since 3.2
 */
public class ProviderInjectionTest extends TestCase {

    @Test
    public void callProviderTest() {

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(ClassB.class).to(ClassB.class);
                binder.bind(ClassA.class).toProvider(ProviderA.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        // access directly instance associate to provider
        ClassA instance = injector.getInstance(ClassA.class);
        assertNotNull(instance);
        assertNotNull(instance.b);

        // call using generic provider interface
        Provider<ClassA> p = injector.getProvider(ClassA.class);
        assertNotNull(p);
        assertEquals(ProviderA.class, p.getClass());

        // call using specific provider class
        ProviderA providerA = injector.getInstance(ProviderA.class);
        assertNotNull(providerA);
        assertEquals(ProviderA.class, providerA.getClass());

    }

    public static class ClassA {
        @Inject
        public ClassB b;
    }

    public static class ClassB {

    }

    public static class ProviderA implements Provider<ClassA> {

        private ClassA instance;

        @Inject
        public ProviderA(ClassB b) {
            instance = new ClassA();
            instance.b = b;
        }

        @Override
        public ClassA get() {
            return instance;
        }
    }

}
