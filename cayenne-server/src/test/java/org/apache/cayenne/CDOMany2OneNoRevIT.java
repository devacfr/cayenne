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

package org.apache.cayenne;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting1;
import org.apache.cayenne.testing.CayenneConfiguration;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.junit.Test;

@CayenneConfiguration(ServerCase.TESTMAP_PROJECT)
public class CDOMany2OneNoRevIT extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        // [devacfr] To resolve conflict (Something wrong depending the order execution)
        // between cached primary key and the real primary keys in database
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST");
    }
    
    @Test
    public void testNewAdd() throws Exception {

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("a");
        Painting1 p1 = context.newObject(Painting1.class);
        p1.setPaintingTitle("p");

        // TESTING THIS
        p1.setToArtist(a1);

        assertSame(a1, p1.getToArtist());

        context.commitChanges();
        ObjectId aid = a1.getObjectId();
        ObjectId pid = p1.getObjectId();
        context.invalidateObjects(a1, p1);

        Painting1 p2 = (Painting1) Cayenne.objectForPK(context, pid);
        Artist a2 = p2.getToArtist();
        assertNotNull(a2);
        assertEquals(aid, a2.getObjectId());
    }
}
