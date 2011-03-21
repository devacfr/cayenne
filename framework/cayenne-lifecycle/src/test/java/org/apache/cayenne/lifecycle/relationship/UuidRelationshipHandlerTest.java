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
package org.apache.cayenne.lifecycle.relationship;

import junit.framework.TestCase;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.lifecycle.db.E1;
import org.apache.cayenne.lifecycle.db.UuidRoot1;
import org.apache.cayenne.lifecycle.ref.ReferenceableHandler;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;

public class UuidRelationshipHandlerTest extends TestCase {

    private ServerRuntime runtime;

    @Override
    protected void setUp() throws Exception {
        runtime = new ServerRuntime("cayenne-lifecycle.xml");
    }

    @Override
    protected void tearDown() throws Exception {
        runtime.shutdown();
    }

    public void testRelate_Existing() throws Exception {

        DBHelper dbHelper = new DBHelper(runtime.getDataSource(null));

        TableHelper rootTable = new TableHelper(dbHelper, "UUID_ROOT1").setColumns(
                "ID",
                "UUID");
        rootTable.deleteAll();
        TableHelper e1Table = new TableHelper(dbHelper, "E1").setColumns("ID");
        e1Table.deleteAll();
        e1Table.insert(1);

        ObjectContext context = runtime.getContext();
        E1 e1 = (E1) Cayenne.objectForQuery(context, new SelectQuery(E1.class));

        UuidRoot1 r1 = context.newObject(UuidRoot1.class);

        ReferenceableHandler refHandler = new ReferenceableHandler(context
                .getEntityResolver());
        UuidRelationshipHandler handler = new UuidRelationshipHandler(refHandler);
        handler.relate(r1, e1);

        assertEquals("E1:1", r1.getUuid());
        assertSame(e1, r1.readPropertyDirectly("cay:related:uuid"));
        
        context.commitChanges();
        
        Object[] r1x = rootTable.select();
        assertEquals("E1:1", r1x[1]);
    }
}
