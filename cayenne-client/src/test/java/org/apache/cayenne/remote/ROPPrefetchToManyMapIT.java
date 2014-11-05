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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.mt.ClientMtMapToMany;
import org.apache.cayenne.testdo.mt.ClientMtMapToManyTarget;
import org.apache.cayenne.testing.CayenneConfiguration;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.junit.Test;

@CayenneConfiguration("cayenne-multi-tier.xml")
public class ROPPrefetchToManyMapIT extends RemoteCayenneCase {
    
    @Inject
    private DBHelper dbHelper;
    
    @Inject
    private DataChannelInterceptor queryInterceptor;

    /**
     * @param serializationPolicy
     */
    public ROPPrefetchToManyMapIT(int serializationPolicy) {
        super(serializationPolicy);
    }

    @Override
    public void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_MAP_TO_MANY_TARGET");
        dbHelper.deleteAll("MT_MAP_TO_MANY");        
    }

    @Test
    public void test() throws Exception {
        ObjectContext context = createROPContext();
        
        ClientMtMapToMany map = context.newObject(ClientMtMapToMany.class);
        ClientMtMapToManyTarget target = context.newObject(ClientMtMapToManyTarget.class);
        target.setMapToMany(map);
        context.commitChanges();
        
        context.performQuery(new RefreshQuery());
        
        SelectQuery<ClientMtMapToMany> query = new SelectQuery<ClientMtMapToMany>(ClientMtMapToMany.class);
        query.addPrefetch("targets");
        
        final ClientMtMapToMany mapToMany = (ClientMtMapToMany) Cayenne.objectForQuery(context, query);
        
        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {
            
            public void execute() {
                assertEquals(mapToMany.getTargets().size(), 1);
            }
        });
    }
}
