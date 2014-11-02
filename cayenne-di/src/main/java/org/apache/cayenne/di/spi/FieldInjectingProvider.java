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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.spi.LifecycleProcessor.LifecycleMetadata;

/**
 * @since 3.1
 */
class FieldInjectingProvider<T> implements Provider<T> {

    private DefaultInjector injector;
    private javax.inject.Provider<T> delegate;

    FieldInjectingProvider(javax.inject.Provider<T> delegate, DefaultInjector injector) {
        this.delegate = delegate;
        this.injector = injector;
    }

    @Override
    public T get() throws DIRuntimeException {
        T object = delegate.get();
        Class<? extends T> type = (Class<? extends T>) object.getClass();
        injectMembers(object, type);
        // perform initialization method for all binding instance (no only for
        // singleton)
        LifecycleMetadata lifecycleMetadata = injector.findLifecycleMetadata(type);
        lifecycleMetadata.invokeInitMethods(object);
        return object;
    }

    private void injectMembers(T object, Class<?> type) {

        // bail on recursion stop condition
        if (type == null) {
            return;
        }

        for (Field field : type.getDeclaredFields()) {
            Annotation annotation = DIUtil.getInjectAnnotation(field);
            if (annotation != null) {
                injectMember(object, field, DIUtil.determineBindingName(field));
            }
        }

        injectMembers(object, type.getSuperclass());
    }

    private void injectMember(Object object, Field field, String bindingName) {

        Object value = value(field, bindingName);

        field.setAccessible(true);
        try {
            field.set(object, value);
        } catch (Exception e) {
            String message = String.format("Error injecting into field %s.%s of type %s", field.getDeclaringClass()
                    .getName(), field.getName(), field.getType().getName());
            throw new DIRuntimeException(message, e);
        }
    }

    /**
     * @since 4.0
     */
    protected Object value(Field field, String bindingName) {

        Class<?> fieldType = field.getType();
        InjectionStack stack = injector.getInjectionStack();

        if (javax.inject.Provider.class.isAssignableFrom(fieldType)) {

            Class<?> objectClass = DIUtil.parameterClass(field.getGenericType());

            if (objectClass == null) {
                throw new DIRuntimeException("Provider field %s.%s of type %s must be "
                        + "parameterized to be usable for injection", field.getDeclaringClass().getName(),
                        field.getName(), fieldType.getName());
            }

            javax.inject.Provider<?> provider = injector.getProvider(Key.get(objectClass, bindingName));
            return provider;
        } else {

            Key<?> key = Key.get(fieldType, bindingName);

            stack.push(key);
            try {
                return injector.getInstance(key);
            } finally {
                stack.pop();
            }
        }
    }
}
