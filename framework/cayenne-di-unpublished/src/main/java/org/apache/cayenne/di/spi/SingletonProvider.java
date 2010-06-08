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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.di.Provider;

/**
 * @since 3.1
 */
class SingletonProvider<T> implements Provider<T> {

    private Provider<T> delegate;
    private SingletonScope scope;

    // presumably "volatile" works in Java 5 and newer to prevent double-checked locking
    private volatile T instance;

    public SingletonProvider(SingletonScope scope, Provider<T> delegate) {
        this.scope = scope;
        this.delegate = delegate;
    }

    public T get() {

        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = delegate.get();

                    if (instance == null) {
                        throw new ConfigurationException(
                                "Underlying provider (%s) returned NULL instance",
                                delegate.getClass().getName());
                    }

                    scope.addScopeEventListener(instance);
                }
            }
        }

        return instance;
    }

}
