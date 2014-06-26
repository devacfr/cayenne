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

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Provider;

/**
 * A provider that provides scoping for other providers.
 *
 * @since 3.1
 */
public class DefaultScopeProvider<T> implements Provider<T> {

    private final Key<T> key;
    private final javax.inject.Provider<T> delegate;
    private final DefaultScope scope;

    // presumably "volatile" works in Java 5 and newer to prevent double-checked
    // locking
    private volatile T instance;

    public DefaultScopeProvider(final Key<T> key, final DefaultScope scope, final javax.inject.Provider<T> delegate) {
        this.key = key;
        this.scope = scope;
        this.delegate = delegate;
    }

    @Override
    public T get() {

        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = delegate.get();

                    if (instance == null) {
                        throw new DIRuntimeException("Underlying provider (%s) returned NULL instance", delegate
                                .getClass().getName());
                    }
                }
            }
        }

        return instance;
    }

    T getInstance() {
        return instance;
    }

    /**
     * @return the key
     */
    public Key<T> getKey() {
        return key;
    }

    /**
     * Calls when associated scope {@link DefaultScope#reset() reset}. The
     * method is typically used to release resources that it has been holding.
     *
     * @param injector
     *            the associated injector.
     */
    void beforeEndScope(DefaultInjector injector) {
        if (instance == null)
            return;
        Binding<?> binding = injector.getBinding(getKey());
        binding.doPreDestroy(instance);
    }

    /**
     * Calls when associated scope {@link DefaultScope#reset() reset}. The
     * method is typically used to release scope resources that it has been
     * holding.
     *
     * @param injector
     *            the associated injector.
     */
    void afterEndScope(DefaultInjector injector) {
        if (instance == null)
            return;
        Binding<?> binding = injector.getBinding(getKey());
        binding.doPostDestroy(instance);
        clear();
    }

    /**
     * Allows detach the associated instance to this provider (internal use).
     * <p>
     * Note: used after the reset of associated scope.
     * 
     * @see #afterEndScope(DefaultInjector)
     */
    void clear() {
        Object localInstance = instance;

        if (localInstance != null) {
            instance = null;
        }
    }
}
