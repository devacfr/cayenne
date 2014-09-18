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
package org.apache.cayenne.merge;

import java.sql.Types;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testing.CayenneConfiguration;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.junit.Test;

@CayenneConfiguration(ServerCase.TESTMAP_PROJECT)
public class SetNotNullToDbTest extends MergeCase {

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        super.setUpAfterInjection();

        // must cleanup the tables as changing NULL column to NOT NULL may require that no
        // nullable data is stored in the column
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
    }

    @Test
    public void test() throws Exception {
        DbEntity dbEntity = map.getDbEntity("PAINTING");
        assertNotNull(dbEntity);

        // create and add new column to model and db
        DbAttribute column = new DbAttribute("NEWCOL2", Types.VARCHAR, dbEntity);

        column.setMandatory(false);
        column.setMaxLength(10);
        dbEntity.addAttribute(column);
        assertTokensAndExecute(1, 0);

        // check that is was merged
        assertTokensAndExecute(0, 0);

        // set not null
        column.setMandatory(true);

        // merge to db
        assertTokensAndExecute(1, 0);

        // check that is was merged
        assertTokensAndExecute(0, 0);

        // clean up
        dbEntity.removeAttribute(column.getName());
        assertTokensAndExecute(1, 0);
        assertTokensAndExecute(0, 0);
    }

}
