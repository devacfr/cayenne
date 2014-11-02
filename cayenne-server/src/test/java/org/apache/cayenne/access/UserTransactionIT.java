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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testing.CayenneConfiguration;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.CayenneTransaction;
import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.junit.Test;

@CayenneConfiguration(ServerCase.TESTMAP_PROJECT)
public class UserTransactionIT extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private JdbcEventLogger logger;

    @Test
    public void testCommit() throws Exception {

        Artist a = context.newObject(Artist.class);
        a.setArtistName("AAA");

        TxWrapper t = new TxWrapper(new CayenneTransaction(logger));
        BaseTransaction.bindThreadTransaction(t);

        try {
            context.commitChanges();
        } finally {
            t.rollback();
            BaseTransaction.bindThreadTransaction(null);
        }

        assertEquals(0, t.commitCount);
        assertEquals(1, t.connectionCount);
    }

    class TxWrapper implements Transaction {

        private Transaction delegate;
        int commitCount;
        int connectionCount;

        TxWrapper(Transaction delegate) {
            this.delegate = delegate;
        }

        @Override
        public void begin() {
            delegate.begin();
        }

        @Override
        public void commit() {
            commitCount++;
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
            connectionCount++;
            delegate.addConnection(name, connection);
        }
    }

}