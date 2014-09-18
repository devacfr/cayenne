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

import javax.inject.Provider;

import org.apache.cayenne.di.ConstantBindingBuilder;
import org.apache.cayenne.di.Key;

/**
 *
 * @since 3.2
 * @param <T>
 */
public final class DefaultConstantBindingBuilder<T> implements ConstantBindingBuilder<T> {

    protected final DefaultInjector injector;
    protected final Key<T> bindingKey;

    public DefaultConstantBindingBuilder(final Key<T> bindingKey, final DefaultInjector injector) {
        this.injector = injector;
        this.bindingKey = bindingKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void to(final String value) {
        toValue(String.class, value);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void to(final char value) {
        toValue(char.class, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void to(final int value) {
        toValue(Integer.class, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void to(final long value) {
        toValue(Long.class, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void to(final boolean value) {
        toValue(Boolean.class, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void to(final double value) {
        toValue(Double.class, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void to(final float value) {
        toValue(Float.class, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void to(final short value) {
        toValue(Short.class, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void to(final byte value) {
        toValue(Byte.class, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void to(final Class<?> value) {
        toValue(Class.class, value);

    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    void toValue(final Class<?> type, final Object value) {
        T obj = (T) value;
        Class<T> cl = (Class<T>) type;
        Provider<T> provider = new InstanceProvider<T>(obj);
        injector.putBinding(bindingKey, provider, cl);
    }
}