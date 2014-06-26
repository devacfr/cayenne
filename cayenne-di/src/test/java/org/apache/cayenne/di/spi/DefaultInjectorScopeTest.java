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

import javax.inject.Singleton;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.mock.MockImplementation1;
import org.apache.cayenne.di.mock.MockImplementation1_EventAnnotations;
import org.apache.cayenne.di.mock.MockImplementation1_Provider;
import org.apache.cayenne.di.mock.MockImplementation1_ServiceScope;
import org.apache.cayenne.di.mock.MockInterface1;
import org.apache.cayenne.di.mock.Mock_JSR330_Implementation1_JSR250_Provider;
import org.apache.cayenne.di.mock.Mock_JSR330_Implementation1_JSR250_ScopeEvent;
import org.apache.cayenne.di.mock.Mock_JSR330_Implementation1_PostConstruct;
import org.apache.cayenne.di.mock.Mock_JSR330_Implementation1_Provider;
import org.apache.cayenne.di.mock.Service;
import org.apache.cayenne.di.mock.ServiceScope;
import org.apache.cayenne.di.testing.TestCase;
import org.junit.Test;

public class DefaultInjectorScopeTest extends TestCase {

    @Test
    public void testDefaultScope_IsSingleton() {

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(MockImplementation1.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance2 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance3 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }

    @Test
    public void testNoScope() {

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(MockImplementation1.class).withoutScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance2 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance3 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertNotSame(instance1, instance2);
        assertNotSame(instance2, instance3);
        assertNotSame(instance3, instance1);
    }

    @Test
    public void testSingletonScope() {

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(MockImplementation1.class).inSingletonScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance2 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance3 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }

    @Test
    public void bindSingletonScopeByAnnotation() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(MockImplementation1.class).in(Singleton.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance2 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance3 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }

    @Test
    public void bindServiceScopeByAnnotation() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bindScope(Service.class, new ServiceScope());
                binder.bind(MockInterface1.class).to(MockImplementation1.class).in(Service.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);

        ServiceScope scope = (ServiceScope) injector.getScopeBindings().get(Service.class);
        assertNotNull(scope);
        assertEquals(1, scope.counter);
    }

    @Test(expected = DIRuntimeException.class)
    public void failOnScopeAgain() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bindScope(Service.class, new ServiceScope());
                // fail because MockImplementation1_ServiceScope scoped by
                // annotation
                // and try scope again with singleton scope
                binder.bind(MockInterface1.class).to(MockImplementation1_ServiceScope.class).inSingletonScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);

        ServiceScope scope = (ServiceScope) injector.getScopeBindings().get(Service.class);
        assertNotNull(scope);
        assertEquals(1, scope.counter);
    }

    @Test(expected = DIRuntimeException.class)
    public void failOnUndeclaredScope() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                // fail because MockImplementation1_ServiceScope scoped by
                // annotation,
                // but Service scope is not bound yet
                binder.bind(MockInterface1.class).to(MockImplementation1_ServiceScope.class);
            }
        };

        new DefaultInjector(module);
    }

    @Test
    public void justInTimeBindingServiceScopeByAnnotation() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bindScope(Service.class, new ServiceScope());
                binder.bind(MockInterface1.class).to(MockImplementation1_ServiceScope.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);

        ServiceScope scope = (ServiceScope) injector.getScopeBindings().get(Service.class);
        assertNotNull(scope);
        assertEquals(1, scope.counter);
    }

    @Test
    public void testSingletonScope_AnnotatedEvents() {

        MockImplementation1_EventAnnotations.reset();

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(MockImplementation1_EventAnnotations.class).inSingletonScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        assertEquals("XuI", instance1.getName());

        assertFalse(MockImplementation1_EventAnnotations.shutdown1);
        assertFalse(MockImplementation1_EventAnnotations.shutdown2);
        assertFalse(MockImplementation1_EventAnnotations.shutdown3);

        injector.getSingletonScope().shutdown();

        assertTrue(MockImplementation1_EventAnnotations.shutdown1);
        assertTrue(MockImplementation1_EventAnnotations.shutdown2);
        assertTrue(MockImplementation1_EventAnnotations.shutdown3);
    }

    @Test
    public void testSingletonScope_JSR330AnnotatedEvents() {

        MockImplementation1_EventAnnotations.reset();

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(Mock_JSR330_Implementation1_JSR250_ScopeEvent.class)
                        .inSingletonScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        // assertTrue(MockImplementation1_EventAnnotations.initialize1);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        assertEquals("XuI", instance1.getName());
        assertTrue(MockImplementation1_EventAnnotations.initialize1);

        assertFalse(MockImplementation1_EventAnnotations.shutdown1);
        assertFalse(MockImplementation1_EventAnnotations.shutdown2);
        assertFalse(MockImplementation1_EventAnnotations.shutdown3);

        injector.getSingletonScope().shutdown();

        assertTrue(MockImplementation1_EventAnnotations.initialize1);

        assertTrue(MockImplementation1_EventAnnotations.shutdown1);
        assertTrue(MockImplementation1_EventAnnotations.shutdown2);
        assertTrue(MockImplementation1_EventAnnotations.shutdown3);
    }

    @Test
    public void testSingletonScope_InherritedJSR330AnnotatedEvents() {

        MockImplementation1_EventAnnotations.reset();

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(Mock_JSR330_Implementation1_PostConstruct.class)
                        .inSingletonScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        assertEquals("XuI", instance1.getName());
        assertTrue(MockImplementation1_EventAnnotations.initialize1);
        assertTrue(MockImplementation1_EventAnnotations.initialize2);
        assertEquals(1, ((Mock_JSR330_Implementation1_PostConstruct) instance1).initializeCounter);

        assertFalse(MockImplementation1_EventAnnotations.shutdown1);
        assertFalse(MockImplementation1_EventAnnotations.shutdown2);
        assertFalse(MockImplementation1_EventAnnotations.shutdown3);

        injector.getSingletonScope().shutdown();

        assertTrue(MockImplementation1_EventAnnotations.initialize1);
        assertTrue(MockImplementation1_EventAnnotations.initialize2);

        assertTrue(MockImplementation1_EventAnnotations.shutdown1);
        assertTrue(MockImplementation1_EventAnnotations.shutdown2);
        assertTrue(MockImplementation1_EventAnnotations.shutdown3);
    }

    @Test
    public void testSingletonScope_WithInstanceOfProvider() {

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).toProvider(MockImplementation1_Provider.class).inSingletonScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance2 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance3 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }

    @Test
    public void testNoScope_WithInstanceOfProvider() {

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).toProvider(MockImplementation1_Provider.class).withoutScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance2 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance3 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertNotSame(instance1, instance2);
        assertNotSame(instance2, instance3);
    }

    @Test
    public void testSingletonScope_WithProvider() {

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).toProvider(MockImplementation1_Provider.class).inSingletonScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        javax.inject.Provider<MockInterface1> instance1 = injector.getProvider(MockInterface1.class);
        javax.inject.Provider<MockInterface1> instance2 = injector.getProvider(MockInterface1.class);
        javax.inject.Provider<MockInterface1> instance3 = injector.getProvider(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }

    @Test
    public void testNoScope_WithProvider() {

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).toProvider(MockImplementation1_Provider.class).withoutScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        javax.inject.Provider<MockInterface1> instance1 = injector.getProvider(MockInterface1.class);
        javax.inject.Provider<MockInterface1> instance2 = injector.getProvider(MockInterface1.class);
        javax.inject.Provider<MockInterface1> instance3 = injector.getProvider(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }

    @Test
    public void testNoScope_WithJSR330Provider() {

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).toProvider(Mock_JSR330_Implementation1_Provider.class).withoutScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance2 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance3 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertNotSame(instance1, instance2);
        assertNotSame(instance2, instance3);
    }

    @Test
    public void testNoScope_WithJSR330InstanceProvider() {

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).toProviderInstance(new Mock_JSR330_Implementation1_Provider())
                        .withoutScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance2 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance3 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertNotSame(instance1, instance2);
        assertNotSame(instance2, instance3);
    }

    @Test
    public void testSingleton_WithJSR330InstanceProvider() {

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).toProviderInstance(new Mock_JSR330_Implementation1_JSR250_Provider())
                        .inSingletonScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        assertEquals(1, ((Mock_JSR330_Implementation1_PostConstruct) instance1).initializeCounter);
        MockInterface1 instance2 = injector.getInstance(MockInterface1.class);
        assertEquals(1, ((Mock_JSR330_Implementation1_PostConstruct) instance2).initializeCounter);
        MockInterface1 instance3 = injector.getInstance(MockInterface1.class);
        assertEquals(1, ((Mock_JSR330_Implementation1_PostConstruct) instance3).initializeCounter);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }
}
