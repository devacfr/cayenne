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
package org.apache.cayenne.di.event;

import javax.inject.Provider;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultInjector;
import org.apache.cayenne.di.testing.TestCase;
import org.junit.Test;

public class EventPublisherCaseTest extends TestCase {

    @Test
    public void publishEventOnBean() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(StandardEventListener.class).to(StandardEventListener.class);
            }
        };

        Injector injector = new DefaultInjector(module);

        assertEquals(false, injector.getInstance(StandardEventListener.class).called);

        injector.refresh();

        assertEquals(true, injector.getInstance(StandardEventListener.class).called);
    }

    @Test
    public void unRegisterEventListener() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(AllSupportedEventListener.class).to(AllSupportedEventListener.class).asEagerSingleton();
                binder.bind(StandardEventListener.class).to(StandardEventListener.class).asEagerSingleton();
            }
        };

        Injector injector = new DefaultInjector(module);

        AllSupportedEventListener allSupportedEventListener = injector.getInstance(AllSupportedEventListener.class);

        assertEquals(false, allSupportedEventListener.called);

        injector.getInstance(EventPublisher.class).unregister(allSupportedEventListener);
        injector.refresh();

        assertEquals(true, injector.getInstance(StandardEventListener.class).called);
        assertEquals(false, allSupportedEventListener.called);

    }

    @Test
    public void publishEventOnBeanWithoutScope() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(StandardEventListener.class).to(StandardEventListener.class).withoutScope();
            }
        };

        Injector injector = new DefaultInjector(module);
        StandardEventListener eventListener = injector.getInstance(StandardEventListener.class);

        assertEquals(false, eventListener.called);

        injector.refresh();

        assertEquals(false, eventListener.called);
    }

    @Test
    public void publishOnAllSupportedEvent() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(AllSupportedEventListener.class).to(AllSupportedEventListener.class);
            }
        };

        Injector injector = new DefaultInjector(module);
        AllSupportedEventListener eventListener = injector.getInstance(AllSupportedEventListener.class);

        assertEquals(false, eventListener.called);

        injector.refresh();

        assertEquals(true, eventListener.called);
    }

    @Test
    public void publishOnProvider() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(StandardEventListener.class).toProvider(StandardProvider.class);
            }
        };

        Injector injector = new DefaultInjector(module);

        assertEquals(false, injector.getInstance(StandardProvider.class).refreshCalled);
        assertEquals(false, injector.getInstance(StandardEventListener.class).called);

        injector.refresh();

        assertEquals(true, injector.getInstance(StandardProvider.class).refreshCalled);
        assertEquals(false, injector.getInstance(StandardEventListener.class).called);
    }

    public static class StandardEventListener {

        public boolean called = false;

        @EventListener
        public void refresh(RefreshContextEvent event) {
            called = true;
        }
    }

    public static class AllSupportedEventListener {

        public boolean called = false;

        @EventListener
        public void onEvent(Object event) {
            called = true;
        }
    }

    public static class StandardProvider implements Provider<StandardEventListener> {

        public boolean refreshCalled = false;
        private StandardEventListener listener;

        /**
         *
         */
        public StandardProvider() {
            listener = new StandardEventListener();
        }

        @EventListener
        public void refresh(RefreshContextEvent event) {
            refreshCalled = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public StandardEventListener get() {
            return listener;
        }
    }
}
