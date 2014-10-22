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
package org.apache.cayenne.di.spi.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.di.event.EventPublisher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class DefaultEventPublisher implements EventPublisher {

    private static Log LOGGER = LogFactory.getLog(DefaultEventPublisher.class);

    private final Map<Class<?>, Collection<ListenerInvoker>> listenerInvokers;

    private final List<ListenerHandler> listenerHandlers = Arrays
            .<ListenerHandler> asList(new AnnotatedMethodsListenerHandler());

    public DefaultEventPublisher() {
        this.listenerInvokers = new ConcurrentHashMap<Class<?>, Collection<ListenerInvoker>>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(Object event) {
        invokeListeners(findListenerInvokersForEvent(event), event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(Object listener) {
        unregister(listener);

        final List<ListenerInvoker> invokers = new ArrayList<ListenerInvoker>();
        for (ListenerHandler listenerHandler : listenerHandlers) {
            invokers.addAll(listenerHandler.getInvokers(listener));
        }
        if (!invokers.isEmpty()) {
            registerListenerInvokers(invokers);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregister(Object listener) {
        if (listener == null)
            return;
        for (Iterator<Entry<Class<?>, Collection<ListenerInvoker>>> invokerIterator = listenerInvokers.entrySet()
                .iterator(); invokerIterator.hasNext();) {
            Collection<ListenerInvoker> invokers = invokerIterator.next().getValue();
            for (Iterator<ListenerInvoker> it = invokers.iterator(); it.hasNext();) {
                if (listener.equals(it.next().getListener())) {
                    it.remove();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterAll() {
        listenerInvokers.clear();
    }

    private Set<ListenerInvoker> findListenerInvokersForEvent(Object event) {
        final Set<ListenerInvoker> invokersForEvent = new LinkedHashSet<ListenerInvoker>();

        for (Class<?> eventClass : findAllTypes(event.getClass())) {
            Collection<ListenerInvoker> invokers = listenerInvokers.get(eventClass);
            if (invokers != null)
                invokersForEvent.addAll(invokers);
        }

        return invokersForEvent;
    }

    private void invokeListeners(Collection<ListenerInvoker> listenerInvokers, Object event) {
        for (ListenerInvoker invoker : listenerInvokers) {
            try {
                invoker.invoke(event);
            } catch (Exception e) {
                // error("There was an exception thrown trying to dispatch event '"
                // + event + "' from the invoker '" + invoker
                // + "'.", e);
            }
        }
    }

    private void registerListenerInvokers(List<? extends ListenerInvoker> invokers) {
        for (ListenerInvoker invoker : invokers) {
            registerListenerInvoker(invoker);
        }
    }

    private void registerListenerInvoker(ListenerInvoker invoker) {
        for (Class<?> eventClass : invoker.getSupportedEventTypes()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("registerListenerInvoker(): register invoker [" + invoker.toString()
                        + "] supporting event [" + eventClass + "]");
            }
            putListenerInvokers(eventClass, invoker);
        }
    }

    private void putListenerInvokers(Class<?> key, ListenerInvoker value) {
        Collection<ListenerInvoker> coll = listenerInvokers.get(key);
        if (coll == null) {
            coll = new ArrayList<ListenerInvoker>();
            coll.add(value);
            if (coll.size() > 0) {
                listenerInvokers.put(key, coll);
            }
        } else {
            coll.add(value);
        }
    }

    private Set<Class<?>> findAllTypes(final Class<?> cls) {
        final Set<Class<?>> types = new HashSet<Class<?>>();
        findAllTypes(cls, types);
        return types;
    }

    private void findAllTypes(final Class<?> cls, final Set<Class<?>> types) {
        if (cls == null) {
            return;
        }

        // check to ensure it hasn't been scanned yet
        if (types.contains(cls)) {
            return;
        }

        types.add(cls);

        findAllTypes(cls.getSuperclass(), types);
        for (int x = 0; x < cls.getInterfaces().length; x++) {
            findAllTypes(cls.getInterfaces()[x], types);
        }
    }

}