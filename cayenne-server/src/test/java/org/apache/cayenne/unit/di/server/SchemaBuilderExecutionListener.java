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

package org.apache.cayenne.unit.di.server;

import org.apache.cayenne.di.Injector;
import org.apache.cayenne.testing.CayenneTestContext;
import org.apache.cayenne.testing.support.AbstractTestExecutionListener;
import org.apache.cayenne.testing.support.CayenneRuntimeInvoker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>SchemaBuilderExecutionListener</code> allows rebuilding the database
 * schema before execution test class and restore the cayenne runtime context
 * with initial value.
 *
 * @since 3.2
 */
public class SchemaBuilderExecutionListener extends AbstractTestExecutionListener {

    private static Log LOGGER = LogFactory.getLog(SchemaBuilderExecutionListener.class);

    /**
     * rebuild the database schema before execution each test class.
     */
    @Override
    public void beforeTestClass(CayenneTestContext testContext) throws Exception {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Before Cayenne Runtime Context");
        }
        CayenneRuntimeInvoker invoker = testContext.getCayenneRuntime();
        Injector injector = invoker.getInjector();
        SchemaBuilder schemaBuilder = injector.getInstance(SchemaBuilder.class);
        schemaBuilder.rebuildSchema();
    }

    /**
     * Forces the restore of context with initial value after test call.
     */
    @Override
    public void afterTestMethod(CayenneTestContext testContext) throws Exception {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Refershing Cayenne Runtime Context");
        }
        CayenneRuntimeInvoker invoker = testContext.getCayenneRuntime();
        invoker.refresh();
    }

}
