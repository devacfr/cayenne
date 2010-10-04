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
package org.apache.cayenne.configuration.server;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.cayenne.configuration.AdhocObjectFactory;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DefaultAdhocObjectFactory;
import org.apache.cayenne.configuration.server.DbAdapterDetector;
import org.apache.cayenne.configuration.server.DefaultDbAdapterFactory;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.MockDbAdapter;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbEntity;

import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockDataSource;

public class DefaultDbAdapterFactoryTest extends TestCase {

    public void testCreatedAdapter_Auto() throws Exception {

        final DbAdapter adapter = new MockDbAdapter() {

            @Override
            public String createTable(DbEntity ent) {
                return "XXXXX";
            }
        };

        List<DbAdapterDetector> detectors = new ArrayList<DbAdapterDetector>();
        detectors.add(new DbAdapterDetector() {

            public DbAdapter createAdapter(DatabaseMetaData md) throws SQLException {
                return adapter;
            }
        });

        MockConnection connection = new MockConnection();

        MockDataSource dataSource = new MockDataSource();
        dataSource.setupConnection(connection);

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
                binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        DefaultDbAdapterFactory factory = new DefaultDbAdapterFactory(detectors);
        injector.injectMembers(factory);

        DbAdapter createdAdapter = factory.createAdapter(
                new DataNodeDescriptor(),
                dataSource);
        assertTrue(createdAdapter instanceof AutoAdapter);
        assertEquals("XXXXX", createdAdapter.createTable(new DbEntity("Test")));
    }

    public void testCreatedAdapter_Generic() throws Exception {

        List<DbAdapterDetector> detectors = new ArrayList<DbAdapterDetector>();

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
                binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        DefaultDbAdapterFactory factory = new DefaultDbAdapterFactory(detectors);
        injector.injectMembers(factory);

        DbAdapter createdAdapter = factory.createAdapter(
                new DataNodeDescriptor(),
                new MockDataSource());
        assertNotNull(createdAdapter);
        assertTrue(
                "Unexpected class: " + createdAdapter.getClass().getName(),
                createdAdapter instanceof AutoAdapter);
        assertEquals("CREATE TABLE Test ()", createdAdapter.createTable(new DbEntity(
                "Test")));
    }

    public void testCreatedAdapter_Custom() throws Exception {

        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setAdapterType(MockDbAdapter.class.getName());

        List<DbAdapterDetector> detectors = new ArrayList<DbAdapterDetector>();

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
                binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        DefaultDbAdapterFactory factory = new DefaultDbAdapterFactory(detectors);
        injector.injectMembers(factory);

        DbAdapter createdAdapter = factory.createAdapter(
                nodeDescriptor,
                new MockDataSource());
        assertNotNull(createdAdapter);
        assertTrue(
                "Unexpected class: " + createdAdapter.getClass().getName(),
                createdAdapter instanceof MockDbAdapter);
    }
}
