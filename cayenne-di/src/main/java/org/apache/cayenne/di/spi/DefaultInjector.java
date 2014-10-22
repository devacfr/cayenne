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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Scope;
import org.apache.cayenne.di.spi.LifecycleProcessor.LifecycleMetadata;

/**
 * A default Cayenne implementations of a DI injector.
 *
 * @since 3.1
 */
public class DefaultInjector implements Injector {

    private DefaultScope singletonScope;
    private Scope noScope;

    private Map<Key<?>, Binding<?>> bindings;

    private Map<Class<? extends Annotation>, Scope> scopes;

    private Map<Key<?>, Decoration<?>> decorations;

    private InjectionStack injectionStack;

    private Scope defaultScope;

    private final LifecycleProcessor lifecycleProcessor;

    public DefaultInjector(Module... modules) throws DIRuntimeException {
        this.lifecycleProcessor = new LifecycleProcessor();
        this.singletonScope = new DefaultScope(this);
        this.noScope = NoScope.INSTANCE;
        scopes = new HashMap<Class<? extends Annotation>, Scope>();
        scopes.put(Singleton.class, this.singletonScope);

        // this is intentionally hardcoded and is not configurable
        this.defaultScope = singletonScope;

        this.bindings = new HashMap<Key<?>, Binding<?>>();
        this.decorations = new HashMap<Key<?>, Decoration<?>>();
        this.injectionStack = new InjectionStack();

        DefaultBinder binder = new DefaultBinder(this);

        // bind self for injector injection...
        binder.bind(Injector.class).toInstance(this);

        // bind modules
        if (modules != null && modules.length > 0) {

            for (Module module : modules) {
                module.configure(binder);
            }
            applyDecorators();
            applyEagerSingleton();
        }
    }

    InjectionStack getInjectionStack() {
        return injectionStack;
    }

    public <T> Binding<T> getBinding(Key<T> key) throws DIRuntimeException {

        if (key == null) {
            throw new NullPointerException("Null key");
        }

        // may return null - this is intentionally allowed in this non-public
        // method
        return (Binding<T>) bindings.get(key);
    }

    <T> Binding<T> putBinding(Key<T> bindingKey, javax.inject.Provider<T> provider, Class<?> implementationClass) {
        Class<? extends Annotation> scopeAnnotation = DIUtil.findScopeAnnotation(implementationClass);
        Scope scope = null;
        Binding<T> binding = new Binding<T>(bindingKey, provider, implementationClass, this);
        if (scopeAnnotation != null) {
            scope = this.scopes.get(scopeAnnotation);
            if (scope == null) {
                throw new DIRuntimeException("Any declared scope does not exist for this '%s' annotation",
                        scopeAnnotation);
            }
            binding.applyScope(scope);
        } else {
            // lazy default scope set, wait the binding is complete see
            // #applyEagerSingleton method.
        }
        bindings.put(bindingKey, binding);
        return binding;
    }

    <T> ProviderBinding<T> putProviderBinding(Key<T> bindingKey, Provider<Provider<T>> provider1, Class<Provider<T>> cl) {
        Class<? extends Annotation> scopeAnnotation = DIUtil.findScopeAnnotation(cl);
        Scope scope = null;
        Key<Provider<T>> providerKey = DIUtil.named(Key.get(cl));
        ProviderBinding<T> bindingProvider = new ProviderBinding<T>(bindingKey, providerKey, provider1, cl, this);
        if (scopeAnnotation != null) {
            scope = this.scopes.get(scopeAnnotation);
            if (scope == null) {
                throw new DIRuntimeException("Any declared scope does not exist for this '%s' annotation",
                        scopeAnnotation);
            }
            bindingProvider.applyScope(scope);
        } else {
            // lazy default scope set, wait the binding is complete see
            // #applyEagerSingleton method.
        }
        bindings.put(bindingProvider.getKey(), bindingProvider);
        bindings.put(bindingProvider.getProvidedKey(), bindingProvider);
        return bindingProvider;
    }

    public void putScope(Class<? extends Annotation> scopeAnnotation, Scope scope) {
        this.scopes.put(scopeAnnotation, scope);

    }

    <T> void putDecorationAfter(Key<T> bindingKey, DecoratorProvider<T> decoratorProvider) {

        Decoration<T> decoration = (Decoration<T>) decorations.get(bindingKey);
        if (decoration == null) {
            decoration = new Decoration<T>();
            decorations.put(bindingKey, decoration);
        }

        decoration.after(decoratorProvider);
    }

    <T> void putDecorationBefore(Key<T> bindingKey, DecoratorProvider<T> decoratorProvider) {

        Decoration<T> decoration = (Decoration<T>) decorations.get(bindingKey);
        if (decoration == null) {
            decoration = new Decoration<T>();
            decorations.put(bindingKey, decoration);
        }

        decoration.before(decoratorProvider);
    }

    <T> void applyBindingScope(Key<T> bindingKey, Scope scope) {
        if (scope == null) {
            scope = noScope;
        }
        Binding<?> binding = bindings.get(bindingKey);
        if (binding == null) {
            throw new DIRuntimeException("No existing binding for key " + bindingKey);
        }

        if (binding.hasScope()) {
            throw new DIRuntimeException("Binding '%s' is already scoped", bindingKey);
        }

        binding.applyScope(scope);
    }

    <T> void asEagerSingleton(Key<T> bindingKey) {
        Binding<?> binding = bindings.get(bindingKey);
        if (binding == null) {
            throw new DIRuntimeException("No existing binding for key " + bindingKey);
        }

        binding.asEagerSingleton();
    }

    @Override
    public <T> T getInstance(Class<T> type) throws DIRuntimeException {
        return getProvider(type).get();
    }

    @Override
    public <T> T getInstance(Key<T> key) throws DIRuntimeException {
        return getProvider(key).get();
    }

    @Override
    public <T> javax.inject.Provider<T> getProvider(Class<T> type) throws DIRuntimeException {
        return getProvider(Key.get(type));
    }

    @Override
    public <T> javax.inject.Provider<T> getProvider(Key<T> key) throws DIRuntimeException {

        if (key == null) {
            throw new NullPointerException("Null key");
        }

        Binding<T> binding = (Binding<T>) bindings.get(key);
        if (binding == null) {
            throw new DIRuntimeException("DI container has no binding for key %s", key);
        }

        if (!Provider.class.isAssignableFrom(key.getType()) && binding instanceof ProviderBinding) {
            ProviderBinding<T> p = (ProviderBinding<T>) binding;
            return p.getScoped().get();
        } else {
            return binding.getScoped();
        }

    }

    @Override
    public void injectMembers(Object object) {
        javax.inject.Provider<Object> provider0 = new InstanceProvider<Object>(object);
        javax.inject.Provider<Object> provider1 = new FieldInjectingProvider<Object>(provider0, this);
        provider1.get();
    }

    @Override
    public Map<Class<? extends Annotation>, Scope> getScopeBindings() {
        return Collections.unmodifiableMap(this.scopes);
    }

    @Override
    public void shutdown() {
        singletonScope.shutdown();
        bindings.clear();
        decorations.clear();
        injectionStack.reset();
        lifecycleProcessor.clear();
    }

    DefaultScope getSingletonScope() {
        return singletonScope;
    }

    Scope getNoScope() {
        return noScope;
    }

    LifecycleMetadata findLifecycleMetadata(Class<?> cls) {
        return this.lifecycleProcessor.findLifecycleMetadata(cls);
    }

    void applyEagerSingleton() {
        for (Binding<?> binding : this.bindings.values()) {
            Key<?> key = binding.getKey();
            if (!binding.hasScope()) {
                binding.applyScope(this.singletonScope);
            }
            if (this.singletonScope.has(key) && binding.isEager()) {
                Provider<?> provider = this.getProvider(key);
                // force instantiation.
                provider.get();
            }
        }
    }

    void applyDecorators() {
        for (Entry<Key<?>, Decoration<?>> e : decorations.entrySet()) {

            Binding b = bindings.get(e.getKey());
            if (b == null) {
                // TODO: print warning - decorator of a non-existing service..
                continue;
            }

            b.decorate(e.getValue());
        }
    }

}
