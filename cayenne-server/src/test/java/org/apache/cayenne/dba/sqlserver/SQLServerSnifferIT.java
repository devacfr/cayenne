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

package org.apache.cayenne.dba.sqlserver;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testing.CayenneConfiguration;
import org.apache.cayenne.unit.SQLServerUnitDbAdapter;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.junit.Test;

@CayenneConfiguration(ServerCase.TESTMAP_PROJECT)
public class SQLServerSnifferIT extends ServerCase {

    @Inject
    private ServerCaseDataSourceFactory dataSourceFactory;

    @Inject
    private UnitDbAdapter accessStackAdapter;
    
    @Inject
    private AdhocObjectFactory objectFactory;

    @Test
    public void testCreateAdapter() throws Exception {

        SQLServerSniffer sniffer = new SQLServerSniffer(objectFactory);

        DbAdapter adapter = null;
        Connection c = dataSourceFactory.getSharedDataSource().getConnection();

        try {
            adapter = sniffer.createAdapter(c.getMetaData());
        }
        finally {
            try {
                c.close();
            }
            catch (SQLException e) {

            }
        }

        if (accessStackAdapter instanceof SQLServerUnitDbAdapter) {
            assertNotNull(adapter);
        }
        else {
            assertNull(adapter);
        }
    }
}
