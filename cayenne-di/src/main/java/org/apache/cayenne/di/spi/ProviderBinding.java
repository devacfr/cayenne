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

import org.apache.cayenne.di.Key;

public class ProviderBinding<T> extends Binding<Provider<T>> {

    private final Key<T> providedKey;

    /**
     * @param key
     * @param provider1
     * @param cl
     * @param injector
     */
    ProviderBinding(Key<T> providedKey, Key<Provider<T>> providerKey, Provider<Provider<T>> provider1,
            Class<Provider<T>> cl, DefaultInjector injector) {
        super(providerKey, provider1, cl, injector);
        this.providedKey = providedKey;
    }

    /**
     * @return the providerKey
     */
    public Key<T> getProvidedKey() {
        return providedKey;
    }

}
