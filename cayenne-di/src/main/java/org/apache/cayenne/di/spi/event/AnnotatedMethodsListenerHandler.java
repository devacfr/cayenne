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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.di.event.EventListener;

public final class AnnotatedMethodsListenerHandler implements ListenerHandler {

    private final Class<? extends Annotation> annotationClass;

    public AnnotatedMethodsListenerHandler() {
        this(EventListener.class);
    }

    public AnnotatedMethodsListenerHandler(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends ListenerInvoker> getInvokers(final Object listener) {
        final List<Method> validMethods = getValidMethods(listener);
        List<ListenerInvoker> invokers = new ArrayList<ListenerInvoker>(validMethods.size());
        for (Method method : validMethods) {
            invokers.add(new MethodListenerInvoker(listener, method));
        }
        return invokers;
    }

    private List<Method> getValidMethods(Object listener) {
        final List<Method> annotatedMethods = new ArrayList<Method>();
        for (Method method : listener.getClass().getMethods()) {
            if (isValidMethod(method)) {
                annotatedMethods.add(method);
            }
        }
        return annotatedMethods;
    }

    private boolean isValidMethod(Method method) {
        if (isAnnotated(method)) {
            if (hasOneAndOnlyOneParameter(method)) {
                return true;
            } else {
                throw new RuntimeException("Method <" + method + "> of class <" + method.getDeclaringClass() + "> "
                        + "is annotated with <" + annotationClass.getName() + "> but has 0 or more than 1 parameters! "
                        + "Listener methods MUST have 1 and only 1 parameter.");
            }
        }
        return false;
    }

    private boolean isAnnotated(Method method) {
        return method.getAnnotation(annotationClass) != null;
    }

    private boolean hasOneAndOnlyOneParameter(Method method) {
        return method.getParameterTypes().length == 1;
    }
}