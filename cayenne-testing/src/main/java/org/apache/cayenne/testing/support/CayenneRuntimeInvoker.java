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

package org.apache.cayenne.testing.support;

import java.lang.reflect.Method;

import org.apache.cayenne.di.Injector;
import org.apache.cayenne.testing.utils.Assert;

/**
 * This Class is a proxy of {@code CayenneRuntime} resolving the recursive
 * dependency with other module of Cayenne project.
 *
 * @since 4.0
 */
public class CayenneRuntimeInvoker {

    private final Object runtime;

    private final Method injectorMethod;

    private final Method shutdownMethod;

    /**
     * @throws NoSuchMethodException
     * @throws SecurityException
     *
     */
    public CayenneRuntimeInvoker(Object runtime) throws SecurityException, NoSuchMethodException {
        this.runtime = Assert.notNull(runtime);
        this.injectorMethod = runtime.getClass().getMethod("getInjector");
        this.shutdownMethod = runtime.getClass().getMethod("shutdown");
    }

    public Injector getInjector() {
        try {
            return (Injector) injectorMethod.invoke(runtime);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     *
     */
    public void shutdown() {
        try {
            shutdownMethod.invoke(runtime);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * @return
     */
    public Object getInternalRuntime() {
        return runtime;
    }

}
