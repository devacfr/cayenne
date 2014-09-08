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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.Scope;

/**
 * An implementation of a DI scopes with support scope events.
 *
 * @since 3.1
 */
public class DefaultScope implements Scope {

    private Map<Key<?>, DefaultScopeProvider<?>> providers = new ConcurrentHashMap<Key<?>, DefaultScopeProvider<?>>();

    /**
     * not null for singleton scope
     */
    private final DefaultInjector injector;

    /**
     *
     */
    public DefaultScope(final DefaultInjector injector) {
        this.injector = injector;
    }

    /**
     *
     */
    public DefaultScope() {
        this(null);
    }

    /**
     *
     */
    public void reset() {
        if (injector != null) {
            for (DefaultScopeProvider<?> provider : this.providers.values()) {
                provider.beforeEndScope(injector);
            }
            for (DefaultScopeProvider<?> provider : this.providers.values()) {
                provider.afterEndScope(injector);
            }
        }
    }

    /**
     * Shuts down this scope, posting {@link BeforeScopeEnd} and
     * {@link AfterScopeEnd} events.
     */
    public void shutdown() {
        this.reset();
        this.providers.clear();
        this.providers = null;
    }

    /**
     * Removes the {@link javax.inject.Provider scoped provider} of this scope
     * instance.
     *
     * @param scoped
     *            the provider to remove of this scope (can be <tt>null</tt>).
     */
    public <T> void unScope(javax.inject.Provider<T> scoped) {
        if (scoped == null)
            return;
        Key<T> key = ((DefaultScopeProvider<T>) scoped).getKey();
        if (key != null) {
            this.providers.remove(key);
        }
    }

    @Override
    public <T> Provider<T> scope(Key<T> key, javax.inject.Provider<T> unscoped) {
        if (has(key)) {
            return (Provider<T>) this.providers.get(key);
        }
        DefaultScopeProvider<T> provider = new DefaultScopeProvider<T>(key, unscoped);
        this.providers.put(key, provider);
        return provider;
    }

    public boolean has(Key<?> key) {
        return this.providers.containsKey(key);
    }

}
