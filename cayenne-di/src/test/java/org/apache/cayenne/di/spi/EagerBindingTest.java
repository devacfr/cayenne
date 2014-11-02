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

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.mock.Service;
import org.apache.cayenne.di.mock.ServiceScope;
import org.apache.cayenne.di.testing.TestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test to eagerly singleton-scoped binding.
 *
 * @since 4.0
 */
public class EagerBindingTest extends TestCase {

    private static boolean instanceBInitialized = false;
    private static boolean instanceAInitialized = false;

    @Override
    public void setUp() throws Exception {
        instanceBInitialized = false;
        instanceAInitialized = false;
    };

    @Test
    public void defaultBinding() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(ClassB.class).to(ClassB.class);
            }
        };

        new DefaultInjector(module);
        // lazy binding
        Assert.assertEquals(false, instanceBInitialized);
    }

    @Test
    public void defaultDependencyBinding() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(ClassB.class).to(ClassB.class);
                binder.bind(ClassA.class).to(ClassA.class);
            }
        };

        new DefaultInjector(module);
        // lazy binding
        Assert.assertEquals(false, instanceAInitialized);
        Assert.assertEquals(false, instanceBInitialized);
    }

    @Test
    public void eagerBindingOutSingletonScope() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bindScope(Service.class, new ServiceScope());
                binder.bind(ClassB.class).to(ClassB.class).in(Service.class).asEagerSingleton();
            }
        };

        new DefaultInjector(module);
        // not initialize, out of scope
        Assert.assertEquals(false, instanceBInitialized);
    }

    @Test
    public void eagerBindingInDefaultScope() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(ClassB.class).to(ClassB.class).asEagerSingleton();
            }
        };

        new DefaultInjector(module);
        // initialize in default scope = singleton
        Assert.assertEquals(true, instanceBInitialized);
    }

    @Test
    public void eagerBindingInSingletonScope() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(ClassB.class).to(ClassB.class).inSingletonScope().asEagerSingleton();
            }
        };

        new DefaultInjector(module);
        // initialize in singleton scope
        Assert.assertEquals(true, instanceBInitialized);
    }

    @Test
    public void eagerBindingWithDependency() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(ClassB.class).to(ClassB.class);
                binder.bind(ClassA.class).to(ClassA.class).inSingletonScope().asEagerSingleton();
            }
        };

        new DefaultInjector(module);
        Assert.assertEquals(true, instanceAInitialized);
        Assert.assertEquals(true, instanceBInitialized);
    }

    public static class ClassA {
        @Inject
        public ClassB b;

        public ClassA() {
            instanceAInitialized = true;
        }
    }

    public static class ClassB {

        /**
         *
         */
        public ClassB() {
            instanceBInitialized = true;
        }
    }

}
