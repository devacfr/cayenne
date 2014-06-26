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

import javax.inject.Provider;

import org.apache.cayenne.di.BindingBuilder;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Scope;

/**
 * @since 3.1
 */
class DefaultBindingBuilder<T> implements BindingBuilder<T> {

    protected DefaultInjector injector;
    protected Key<T> bindingKey;

    DefaultBindingBuilder(Key<T> bindingKey, DefaultInjector injector) {
        this.injector = injector;
        this.bindingKey = bindingKey;
    }

    @Override
    public BindingBuilder<T> to(Class<? extends T> implementation) throws DIRuntimeException {

        Provider<T> provider0 = new ConstructorInjectingProvider<T>(implementation, injector);
        Provider<T> provider1 = new FieldInjectingProvider<T>(provider0, injector);

        injector.putBinding(named(this.bindingKey, implementation), provider1, implementation);
        return this;
    }

    @Override
    public BindingBuilder<T> toInstance(T instance) throws DIRuntimeException {
        Provider<T> provider0 = new InstanceProvider<T>(instance);
        Provider<T> provider1 = new FieldInjectingProvider<T>(provider0, injector);
        @SuppressWarnings("unchecked")
        Class<? extends T> implementedType = (Class<? extends T>) instance.getClass();
        injector.putBinding(named(bindingKey, implementedType), provider1, implementedType);
        return this;
    }

    @Override
    public BindingBuilder<T> toProvider(Class<? extends Provider<? extends T>> providerType) {

        Provider<Provider<? extends T>> provider0 = new ConstructorInjectingProvider<Provider<? extends T>>(
                providerType, injector);
        Provider<Provider<? extends T>> provider1 = new FieldInjectingProvider<Provider<? extends T>>(provider0,
                injector);
        // create two binding:
        // one specific binding for Provider DI
        Key providerKey = Key.get(providerType);
        injector.putBinding(named(providerKey, providerType), provider1, providerType);
        // and another for the declared binding.
        toInternalProvider(providerType);
        return this;
    }

    @Override
    public BindingBuilder<T> toProviderInstance(org.apache.cayenne.di.Provider<? extends T> provider) {
        return toProviderInstance((javax.inject.Provider<? extends T>) provider);
    }

    @Override
    public BindingBuilder<T> toProviderInstance(Provider<? extends T> provider) throws DIRuntimeException {
        Provider<Provider<? extends T>> provider0 = new InstanceProvider<Provider<? extends T>>(provider);
        Provider<Provider<? extends T>> provider1 = new FieldInjectingProvider<Provider<? extends T>>(provider0,
                injector);

        Provider<T> provider2 = new CustomProvidersProvider<T>(provider1);
        Provider<T> provider3 = new FieldInjectingProvider<T>(provider2, injector);

        // create two binding:
        // one specific binding for Provider DI
        Class<? extends Provider<? extends T>> providerType = (Class<? extends Provider<? extends T>>) provider
                .getClass();
        Key providerKey = Key.get(providerType);
        injector.putBinding(named(providerKey, providerType), provider3, providerType);
        // and another for the declared binding.
        toInternalProvider(providerType);
        return this;
    }

    @Override
    public void in(Class<? extends Annotation> scopeAnnotation) {
        Scope scope = this.injector.getScopeBindings().get(scopeAnnotation);
        if (scope == null)
            throw new DIRuntimeException("DO NOT exist declared scope for this '%s' annotation", scopeAnnotation);
        injector.applyBindingScope(bindingKey, scope);
    }

    @Override
    public void in(Scope scope) {
        injector.applyBindingScope(bindingKey, scope);
    }

    @Override
    public void withoutScope() {
        in(injector.getNoScope());
    }

    @Override
    public void inSingletonScope() {
        in(injector.getSingletonScope());
    }

    /**
     * Gets or creates new {@link Key} with binding name already defined in
     * {@code key} parameter or by naming annotation.
     * <p>
     * This function allows setting the binding name for key without already
     * defined binding name.
     *
     * @param key
     *            the reference key used.
     * @param implementedType
     *            the implemented binding type associated to the key parameter
     * @return Returns {@code Key} with appropriate binding name.
     * @see DIUtil#determineBindingName(Annotation[]))
     */
    protected static <T> Key<T> named(Key<T> key, Class<? extends T> implementedType) {
        if (implementedType == null) {
            return key;
        }
        if (key.getBindingName() == null) {
            String bindingName = DIUtil.determineBindingName(implementedType.getAnnotations());
            if (bindingName != null && bindingName.length() > 0) {
                return Key.get(key.getType(), bindingName);
            }
        }
        return key;
    }

    protected BindingBuilder<T> toInternalProvider(Class<? extends Provider<? extends T>> providerType) {
        Provider<Provider<? extends T>> provider0 = new ConstructorInjectingProvider<Provider<? extends T>>(
                providerType, injector);
        Provider<Provider<? extends T>> provider1 = new FieldInjectingProvider<Provider<? extends T>>(provider0,
                injector);

        Provider<T> provider2 = new CustomProvidersProvider<T>(provider1);
        Provider<T> provider3 = new FieldInjectingProvider<T>(provider2, injector);

        injector.putBinding(bindingKey, provider3, providerType);
        return this;
    }
}
