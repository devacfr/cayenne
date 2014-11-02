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
package org.apache.cayenne.remote;

import java.util.Arrays;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.persistent.Continent;
import org.apache.cayenne.testdo.persistent.Country;
import org.apache.cayenne.testing.CayenneConfiguration;
import org.apache.cayenne.testing.CayenneParameterizedJUnit4SuiteRunner;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test for entites that are implemented in same class on client and server
 */
@CayenneConfiguration(ClientCase.MULTI_TIER_PROJECT)
@RunWith(CayenneParameterizedJUnit4SuiteRunner.class)
public class LightSuperClassIT extends RemoteCayenneCase {

    @Inject
    private DBHelper dbHelper;

    private boolean server;

    @Parameters(name = "server={0},serializationPolicy={1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] { { true, LocalConnection.HESSIAN_SERIALIZATION },
                { true, LocalConnection.JAVA_SERIALIZATION }, { true, LocalConnection.NO_SERIALIZATION },
                { false, LocalConnection.HESSIAN_SERIALIZATION }, { false, LocalConnection.JAVA_SERIALIZATION },
                { false, LocalConnection.NO_SERIALIZATION } });
    }

    /**
     *
     */
    public LightSuperClassIT(final boolean server, final int serializationPolicy) {
        super(serializationPolicy);
        this.server = server;
    }

    @Override
    public void setUpAfterInjection() throws Exception {
        super.setUpAfterInjection();

        dbHelper.deleteAll("CONTINENT");
        dbHelper.deleteAll("COUNTRY");
    }

    private ObjectContext createContext() {
        if (server) {
            return serverContext;
        } else {
            return createROPContext();
        }
    }

    @Test
    public void testServer() throws Exception {
        ObjectContext context = createContext();
        Continent continent = context.newObject(Continent.class);
        continent.setName("Europe");

        Country country = new Country();
        context.registerNewObject(country);

        // TODO: setting property before object creation does not work on ROP (CAY-1320)
        country.setName("Russia");

        country.setContinent(continent);
        assertEquals(continent.getCountries().size(), 1);

        context.commitChanges();

        context.deleteObjects(country);
        assertEquals(continent.getCountries().size(), 0);
        continent.setName("Australia");

        context.commitChanges();
        context.performQuery(new RefreshQuery());

        assertEquals(context.performQuery(new SelectQuery<Country>(Country.class)).size(), 0);
        assertEquals(context.performQuery(new SelectQuery<Continent>(Continent.class)).size(), 1);
    }
}
