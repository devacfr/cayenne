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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Provider;

import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Scope;
import org.apache.cayenne.di.spi.LifecycleProcessor.LifecycleMetadata;

/**
 * A binding encapsulates DI provider scoping settings and allows to change them
 * as many times as needed.
 * <p>
 * <b>Note</b>:
 * <p>
 * the binding can be scoped only once, likewise for the decorator.
 *
 * @since 3.1
 */
class Binding<T> {

    /**
     * The binding key.
     */
    private final Key<T> key;

    /**
     * The original binding provider (never {@code null}).
     */
    private final Provider<T> original;

    /**
     * Decorated provider which delegates the original provider.
     */
    private Provider<T> decorated;

    /**
     * Scoped provider which delegate the original or decorated provider (never
     * {@code null}).
     */
    private Provider<T> scoped;

    /**
     * Instance of injector.
     */
    private final DefaultInjector injector;

    /**
     * The current scope associated.
     */
    private Scope scope;

    /**
     *
     */
    private final Class<?> implementedType;

    /**
     *
     */
    private final LifecycleMetadata lifecycleMetadata;

    /**
     * Creates new binding instance.
     *
     * @param key
     *            the binding key.
     * @param provider
     *            the original provider (never {@code null}).
     * @param injector
     *            the associated injector (never {@code null}).
     */
    Binding(final Key<T> key, final Provider<T> provider, final Class<?> implementedType, final DefaultInjector injector) {
        this.key = key;
        this.original = provider;
        this.decorated = provider;
        this.injector = injector;
        this.implementedType = implementedType;
        this.lifecycleMetadata = injector.findLifecycleMetadata(implementedType);
    }

    /**
     * Gets inidcating whether the binding is scoped
     *
     * @return Returns <tt>true</tt> whether this binding is associated to a
     *         scope.
     */
    boolean hasScope() {
        return this.scope != null;
    }

    /**
     * Allows to apply a specific scope
     *
     * @param scope
     *            the scope to apply
     */
    void applyScope(Scope scope) {
        if (hasScope()) {
            throw new RuntimeException("the binding " + key + " is already scoped");
        }
        this.scoped = scope.scope(key, original);
        this.scope = scope;
    }

    /**
     * Apply the decoration to this binding.
     *
     * @param decoration
     *            containing list of decorators apply to.
     */
    void decorate(Decoration<T> decoration) {
        List<DecoratorProvider<T>> decorators = decoration.decorators();
        if (decorators.isEmpty()) {
            return;
        }

        javax.inject.Provider<T> provider = this.original;
        for (DecoratorProvider<T> decoratorProvider : decorators) {
            provider = decoratorProvider.get(provider);
        }

        this.decorated = provider;

        // set default scope
        if (scope == null) {
            this.scope = injector.getSingletonScope();
        }
        // TODO: what happens to the old scoped value? Seems like this leaks
        // scope event listeners and may cause unexpected events...
        // TODO [devacfr] can be better
        if (scope instanceof DefaultScope && scoped != null) {
            ((DefaultScope) scope).unScope(scoped);
        }

        this.scoped = scope.scope(key, decorated);
    }

    /**
     * Gets the original {@link Provider} associated to this binding
     *
     * @return Returns the original {@link Provider} (never {@code null}).
     */
    Provider<T> getOriginal() {
        return original;
    }

    /**
     * Gets the scoped {@link Provider} associated to this binding.
     * <p>
     * Note : if any scope is defined for this binding, The default scope is
     * associated to it.
     *
     * @return Returns the scoped {@link Provider} (never {@code null}).
     */
    Provider<T> getScoped() {
        // set default scope
        if (!hasScope()) {
            applyScope(injector.getSingletonScope());
        }
        return scoped;
    }

    Class<T> getBindingType() {
        return key.getType();
    }

    /**
     * @return the implementedType
     */
    Class<?> getImplementedType() {
        return implementedType;
    }

    /**
     * Performs methods that need to be executed after dependency injection is
     * done to perform any initialization.
     *
     * @see PostConstruct
     */
    void doInitialize(Object bean) {
        lifecycleMetadata.invokeInitMethods(bean);
    }

    /**
     * Performs methods that need to be executed to signal to associated object
     * is in the process of being removed by the DI container (typically used to
     * release resources that it has been holding).
     *
     * @see PreDestroy
     */
    void doPreDestroy(Object bean) {
        lifecycleMetadata.invokePreDestroyMethods(bean);
    }

    /**
     * Performs methods that need to be executed to signal to associated object
     * is removed by the DI container.
     */
    void doPostDestroy(Object bean) {
        lifecycleMetadata.invokePostDestroyMethods(bean);
    }

}
