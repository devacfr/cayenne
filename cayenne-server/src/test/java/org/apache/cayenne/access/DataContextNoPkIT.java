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

package org.apache.cayenne.access;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.NoPkTestEntity;
import org.apache.cayenne.testing.CayenneConfiguration;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.junit.Test;

@CayenneConfiguration(ServerCase.TESTMAP_PROJECT)
public class DataContextNoPkIT extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        TableHelper noPkTestTable = new TableHelper(dbHelper, "NO_PK_TEST", "ATTRIBUTE1");
        noPkTestTable.deleteAll();

        noPkTestTable.insert(1);
        noPkTestTable.insert(2);
    }

    @Test
    public void testNoPkFetchObjects() throws Exception {
        try {
            List objects = context.performQuery(new SelectQuery(NoPkTestEntity.class));
            fail("Query for entity with no primary key must have failed, instead we got "
                    + objects.size()
                    + " rows.");
        }
        catch (CayenneRuntimeException ex) {
            // exception expected
        }
    }

    @Test
    public void testNoPkFetchDataRows() throws Exception {
        SelectQuery query = new SelectQuery(NoPkTestEntity.class);
        query.setFetchingDataRows(true);

        List rows = context.performQuery(query);
        assertNotNull(rows);
        assertEquals(2, rows.size());

        Map row1 = (Map) rows.get(0);
        Map row2 = (Map) rows.get(1);

        // assert that rows have different values
        // (there was a bug earlier that fetched distinct rows for
        // entities with no primary key.
        assertTrue(!row1.get("ATTRIBUTE1").equals(row2.get("ATTRIBUTE1")));
    }
}
