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
package org.apache.cayenne.unit.di.client;

import org.apache.cayenne.testing.CayenneBlockJUnit4ClassRunner;
import org.apache.cayenne.testing.ClassMode;
import org.apache.cayenne.testing.Modules;
import org.apache.cayenne.testing.TestExecutionListeners;
import org.apache.cayenne.testing.support.DependencyInjectionTestExecutionListener;
import org.apache.cayenne.testing.support.DirtiesRuntimeTestExecutionListener;
import org.apache.cayenne.testing.support.InjectMode;
import org.apache.cayenne.unit.di.DICase;
import org.apache.cayenne.unit.di.server.SchemaBuilderExecutionListener;
import org.apache.cayenne.unit.di.server.ServerCaseModule;
import org.junit.runner.RunWith;

@RunWith(CayenneBlockJUnit4ClassRunner.class)
@TestExecutionListeners(listeners = {
        // Allows setting client properties before injection, before test method
        ClientPropertiesExecutionListener.class,
        // Injects dependencies in test class
        DependencyInjectionTestExecutionListener.class,
        // Allows setting client properties before injection, after test method
        ClientPropertiesExecutionListener.class,
        // Allows recreate new cayenne runtime
        DirtiesRuntimeTestExecutionListener.class,
        // Rebuild db schema
        SchemaBuilderExecutionListener.class })
@Modules({ ServerCaseModule.class, ClientCaseModule.class })
//re-inject dependencies in a test class for each test method (ex: create new ObjectContext
@InjectMode(classMode = ClassMode.AfterTestMethod)
public abstract class ClientCase extends DICase {

    /**
     * the name of ROP client in DI.
     */
    public static final String ROP_CLIENT_KEY = "client";

    public static final String MULTI_TIER_PROJECT = "cayenne-multi-tier.xml";

}
