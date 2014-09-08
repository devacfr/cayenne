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
package org.apache.cayenne.di;

import java.lang.annotation.Annotation;

/**
 * A binding builder that helps with fluent binding creation.
 *
 * @param <T>
 *            An interface type of the service being bound.
 * @since 3.1
 */
public interface BindingBuilder<T> {

    BindingBuilder<T> to(Class<? extends T> implementation) throws DIRuntimeException;

    BindingBuilder<T> toInstance(T instance) throws DIRuntimeException;

    BindingBuilder<T> toProvider(Class<? extends javax.inject.Provider<? extends T>> providerType)
            throws DIRuntimeException;

    BindingBuilder<T> toProviderInstance(Provider<? extends T> provider) throws DIRuntimeException;

    BindingBuilder<T> toProviderInstance(javax.inject.Provider<? extends T> provider) throws DIRuntimeException;

    BindingBuilder<T> in(Class<? extends Annotation> scopeAnnotation);

    /**
     * Sets the scope of a bound instance. This method is used to change the
     * default scope which is usually a singleton to a custom scope.
     */
    BindingBuilder<T> in(Scope scope);

    /**
     * Sets the scope of a bound instance to singleton. Singleton is normally
     * the default, so calling this method explicitly is rarely needed.
     */
    BindingBuilder<T> inSingletonScope();

    /**
     * Sets the scope of a bound instance to "no scope". This means that a new
     * instance of an object will be created on every call to
     * {@link Injector#getInstance(Class)}.
     */
    BindingBuilder<T> withoutScope();

    /**
     * Indicates to eagerly initialize this singleton-scoped binding upon
     * cayenne startup.
     * <p>
     * <b>Note</b>
     * </p>
     * If there are problems with any of the singleton beans, exceptions will
     * occur at cayenne startup time versus at the time when the singleton may
     * first be used. Secondly, as many singleton beans are resource manager
     * instances (like event manager or transaction managers) having the beans
     * start with the initialization of the container avoids any delay when the
     * service provided by the resource manager bean is requested the first
     * time.
     */
    BindingBuilder<T> asEagerSingleton();
}
