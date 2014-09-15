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



import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.mock.MockImplementation1;
import org.apache.cayenne.di.mock.MockImplementation1Alt;
import org.apache.cayenne.di.mock.MockImplementation1Alt2;
import org.apache.cayenne.di.mock.MockInterface1;
import org.apache.cayenne.di.mock.MockInterface1Provider;
import org.apache.cayenne.di.mock.Mock_Implementation1_Named;
import org.apache.cayenne.di.testing.TestCase;
import org.junit.Test;

public class DefaultInjectorBindingTest extends TestCase {

    @Test
    public void testClassBinding() {

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(MockImplementation1.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals("MyName", service.getName());
    }

    @Test
    public void testClassNamedBinding() {

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(MockImplementation1.class);
                binder.bind(Key.get(MockInterface1.class, "abc")).to(
                        MockImplementation1Alt.class);
                binder.bind(Key.get(MockInterface1.class, "xyz")).to(
                        MockImplementation1Alt2.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 defaultObject = injector.getInstance(MockInterface1.class);
        assertNotNull(defaultObject);
        assertEquals("MyName", defaultObject.getName());

        MockInterface1 abcObject = injector.getInstance(Key.get(
                MockInterface1.class,
                "abc"));
        assertNotNull(abcObject);
        assertEquals("alt", abcObject.getName());

        MockInterface1 xyzObject = injector.getInstance(Key.get(
                MockInterface1.class,
                "xyz"));
        assertNotNull(xyzObject);
        assertEquals("alt2", xyzObject.getName());
    }
    
    
    @Test
    public void testAnnotateClassNamedBinding() {

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(Mock_Implementation1_Named.class);
                binder.bind(Key.get(MockInterface1.class, "abc")).to(
                    Mock_Implementation1_Named.class);
                binder.bind(Key.get(MockInterface1.class, "xyz")).to(
                    Mock_Implementation1_Named.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 defaultObject = injector.getInstance(Key.get(MockInterface1.class, "first"));
        assertNotNull(defaultObject);
        assertEquals("MyName", defaultObject.getName());

        MockInterface1 abcObject = injector.getInstance(Key.get(
                MockInterface1.class,
                "abc"));
        assertNotNull(abcObject);

        MockInterface1 xyzObject = injector.getInstance(Key.get(
                MockInterface1.class,
                "xyz"));
        assertNotNull(xyzObject);
    }

    @Test
    public void testProviderBinding() {
        Module module = new Module() {

            public void configure(Binder binder) {
                binder
                        .bind(MockInterface1.class)
                        .toProvider(MockInterface1Provider.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals("MyName", service.getName());
    }

    @Test
    public void testInstanceBinding() {

        final MockImplementation1 instance = new MockImplementation1();

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).toInstance(instance);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertSame(instance, service);
    }

    @Test
    public void testClassReBinding() {

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(MockImplementation1.class);
                binder.bind(MockInterface1.class).to(MockImplementation1Alt.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals("alt", service.getName());
    }

}
