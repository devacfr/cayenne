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
package org.apache.cayenne.tx.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.tx.CayenneTransactionManager;
import org.apache.cayenne.tx.TransactionManagerFactory;

public class DefaultTransactionManagerFactory implements TransactionManagerFactory {

    @Inject
    protected AdhocObjectFactory objectFactory;

    @Inject
    protected Provider<ObjectContextFactory> objectContextFactory;

    protected Map<DataSource, TransactionManager> managedTransactionManager;

    public DefaultTransactionManagerFactory() {
        managedTransactionManager = new ConcurrentHashMap<DataSource, TransactionManager>();
    }

    @Override
    public TransactionManager getTransactionManager(DataSource dataSource) {
        if (dataSource == null)
            return null;
        if (this.managedTransactionManager.containsKey(dataSource))
            return this.managedTransactionManager.get(dataSource);
        TransactionManager transactionManager = createTransactionManager(dataSource);
        this.managedTransactionManager.put(dataSource, transactionManager);
        return transactionManager;
    }

    protected TransactionManager createTransactionManager(DataSource dataSource) {
        return new CayenneTransactionManager(dataSource, objectContextFactory.get());
    }

    @BeforeScopeEnd
    public void shutdown() {
        managedTransactionManager.clear();
    }

}