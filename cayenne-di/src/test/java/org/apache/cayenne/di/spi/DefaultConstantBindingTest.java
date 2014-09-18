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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.testing.TestCase;
import org.junit.Test;

public class DefaultConstantBindingTest extends TestCase {

    @Test
    public void test() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(ClassA.class).to(ClassA.class);
                binder.bindConstant(Key.get(String.class, "jsr330.value")).to("aJsr330InjectedValue");
                binder.bindConstant(Key.get(String.class, "cayenne.value")).to("aCayenneInjectedValue");
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        ClassA obj = injector.getInstance(ClassA.class);
        assertNotNull(obj);
        assertEquals("aJsr330InjectedValue", obj.jsr330Value);
        assertEquals("aCayenneInjectedValue", obj.cayenneValue);
    }

    @Test(expected = DIRuntimeException.class)
    public void wrongInjectedConstantType() {
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(ClassA.class).to(ClassA.class).asEagerSingleton();
                binder.bindConstant(Key.get(String.class, "jsr330.value")).to(20);
            }
        };

        new DefaultInjector(module);
    }

    public static class ClassA {

        @Inject
        @Named("jsr330.value")
        public String jsr330Value;

        @org.apache.cayenne.di.Inject("cayenne.value")
        public String cayenneValue;
    }

}
