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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class MethodListenerInvoker implements ListenerInvoker {
    private final Method method;
    private final Object listener;

    public MethodListenerInvoker(Object listener, Method method) {
        this.listener = listener;
        this.method = method;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getListener() {
        return listener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<?>> getSupportedEventTypes() {
        return new HashSet<Class<?>>(Arrays.asList(method.getParameterTypes()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(Object event) {
        try {
            method.invoke(listener, event);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() == null) {
                throw new RuntimeException(e);
            } else if (e.getCause().getMessage() == null) {
                throw new RuntimeException(e.getCause());
            } else {
                throw new RuntimeException(e.getCause().getMessage(), e.getCause());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "MethodListenerInvoker{method=" + method + ", listener=" + valueOf(listener) + '}';
    }

    private static String valueOf(Object object) {
        try {
            return String.valueOf(object);
        } catch (RuntimeException e) {
            return identityToString(object);
        }
    }

    public static String identityToString(Object object) {
        if (object == null) {
            throw new NullPointerException("Cannot get the toString of a null identity");
        }
        return object.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(object));
    }
}