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
package org.apache.cayenne.unit.di.client;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.DataChannelSyncStats;
import org.apache.cayenne.unit.di.UnitTestClosure;

public class ClientServerDataChannelInterceptor implements DataChannelInterceptor {

    @Inject
    protected Provider<ClientRuntime> clientRuntimeProvider;

    private ClientServerDataChannelDecorator getChannelDecorator() {

        LocalConnection connection = (LocalConnection) clientRuntimeProvider
                .get()
                .getConnection();

        return (ClientServerDataChannelDecorator) connection.getChannel();
    }

    @Override
    public void runWithQueriesBlocked(UnitTestClosure closure) {
        ClientServerDataChannelDecorator channel = getChannelDecorator();

        channel.setBlockingMessages(true);
        try {
            closure.execute();
        }
        finally {
            channel.setBlockingMessages(false);
        }
    }

    @Override
    public int runWithQueryCounter(UnitTestClosure closure) {
        throw new UnsupportedOperationException("TODO... unused for now");
    }

    @Override
    public DataChannelSyncStats runWithSyncStatsCollection(UnitTestClosure closure) {
        ClientServerDataChannelDecorator channel = getChannelDecorator();

        DataChannelSyncStats stats = new DataChannelSyncStats();

        channel.setSyncStatsCounter(stats);
        try {
            closure.execute();
        }
        finally {
            channel.setSyncStatsCounter(null);
        }

        return stats;
    }

}
