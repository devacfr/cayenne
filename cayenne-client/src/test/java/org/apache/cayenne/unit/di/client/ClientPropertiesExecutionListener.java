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

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.di.Injector;
import org.apache.cayenne.testing.CayenneTestContext;
import org.apache.cayenne.testing.support.AbstractTestExecutionListener;

public class ClientPropertiesExecutionListener extends AbstractTestExecutionListener {


    /**
    * {@inheritDoc}
    */
    @Override
    public void prepareTestInstance(CayenneTestContext testContext) throws Exception {
        Injector injector = testContext.getCayenneRuntime().getInjector();
        ClientCaseProperties propertiesProvider = injector.getInstance(ClientCaseProperties.class);
        propertiesProvider.setRuntimeProperties(getClientRuntimeProperties(testContext.getTestClass()));
    }



    /**
    * {@inheritDoc}
    */
    @Override
    public void afterTestMethod(CayenneTestContext testContext) throws Exception {
        Injector injector = testContext.getCayenneRuntime().getInjector();
        ClientCaseProperties propertiesProvider = injector.getInstance(ClientCaseProperties.class);
        propertiesProvider.setRuntimeProperties(getClientRuntimeProperties(testContext.getTestClass()));
    }


    /**
     * @return
     */
    private Map<String, String> getClientRuntimeProperties(Class<?> testClass) {
        ClientRuntimeProperty properties = testClass.getAnnotation(ClientRuntimeProperty.class);

        Map<String, String> map = new HashMap<String, String>();
        if (properties != null) {
            String[] pairs = properties.value();
            if (pairs != null && pairs.length > 1) {

                String key = null;

                for (int i = 0; i < pairs.length; i++) {
                    if (i % 2 == 0) {
                        key = pairs[i];
                    } else {
                        map.put(key, pairs[i]);
                    }
                }
            }
        }
        return map;
    }




}
