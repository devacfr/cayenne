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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.PrimitivesTestEntity;
import org.apache.cayenne.testing.CayenneConfiguration;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.junit.Test;

@CayenneConfiguration(ServerCase.TESTMAP_PROJECT)
public class PrimitiveAttributesIT extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PRIMITIVES_TEST");
    }

    @Test
    public void testCommit() {
        PrimitivesTestEntity e = context.newObject(PrimitivesTestEntity.class);
        e.setBooleanColumn(true);
        e.setIntColumn(88);
        context.commitChanges();
    }
}
