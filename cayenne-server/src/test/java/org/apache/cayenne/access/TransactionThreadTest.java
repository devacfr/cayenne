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

import java.sql.Connection;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testing.CayenneConfiguration;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.CayenneTransaction;
import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.junit.Test;

@CayenneConfiguration(ServerCase.TESTMAP_PROJECT)
public class TransactionThreadTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    private JdbcEventLogger logger;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
    }

    @Test
    public void testThreadConnectionReuseOnSelect() throws Exception {

        ConnectionCounterTx t = new ConnectionCounterTx(new CayenneTransaction(logger));
        BaseTransaction.bindThreadTransaction(t);

        try {

            SelectQuery q1 = new SelectQuery(Artist.class);
            context.performQuery(q1);
            assertEquals(1, t.connectionCount);

            // delegate will fail if the second query opens a new connection
            SelectQuery q2 = new SelectQuery(Artist.class);
            context.performQuery(q2);

        } finally {
            BaseTransaction.bindThreadTransaction(null);
            t.commit();
        }
    }

    class ConnectionCounterTx implements Transaction {

        private Transaction delegate;
        int connectionCount;

        ConnectionCounterTx(Transaction delegate) {
            this.delegate = delegate;
        }

        @Override
        public void begin() {
            delegate.begin();
        }

        @Override
        public void commit() {
            delegate.commit();
        }

        @Override
        public void rollback() {
            delegate.rollback();
        }

        @Override
        public void setRollbackOnly() {
            delegate.setRollbackOnly();
        }

        @Override
        public boolean isRollbackOnly() {
            return delegate.isRollbackOnly();
        }

        @Override
        public Connection getConnection(String name) {
            return delegate.getConnection(name);
        }

        @Override
        public void addConnection(String name, Connection connection) {
            if (connectionCount++ > 0) {
                fail("Invalid attempt to add connection");
            }

            delegate.addConnection(name, connection);
        }
    }
}
