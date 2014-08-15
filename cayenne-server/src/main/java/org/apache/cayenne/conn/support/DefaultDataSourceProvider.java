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

package org.apache.cayenne.conn.support;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.cayenne.BaseContext;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.tx.Synchronization;
import org.apache.cayenne.tx.support.TransactionSynchronizationSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultDataSourceProvider implements DataSourceProvider {


    private static final Log logger = LogFactory.getLog(DefaultDataSourceProvider.class);

    /**
	 * 
	 */
    public DefaultDataSourceProvider() {
        DataSources.registerProvider(this);
    }


    /**
     * {@inheritDoc}
     */
    public Connection getConnection(final DataSource dataSource) throws SQLException {
        if (dataSource == null) {
            throw new IllegalArgumentException("No DataSource specified");
        }

        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationSupport.getResource(dataSource);
        if (conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction())) {
            conHolder.retain();
            if (!conHolder.hasConnection()) {
                logger.debug("Fetching resumed JDBC Connection from DataSource");
                conHolder.setConnection(dataSource.getConnection());
            }
            return conHolder.getConnection();
        }
        // Else we either got no holder or an empty thread-bound holder here.

        logger.debug("Fetching JDBC Connection from DataSource");
        Connection con = dataSource.getConnection();

        if (Synchronization.isSynchronizationActive()) {
            logger.debug("Registering transaction synchronization for JDBC Connection");
            ConnectionHolder holderToUse = conHolder;
            if (holderToUse == null) {
                ObjectContext context = BaseContext.getThreadObjectContext();
                if (context == null) {
                    throw new CayenneRuntimeException("bind a ObjectContext to the current thread before");
                }
                holderToUse = new ConnectionHolder(con, context);
            } else {
                holderToUse.setConnection(con);
            }
            holderToUse.retain();
            Synchronization.registerSynchronization(new ConnectionSynchronization( holderToUse, dataSource));
            holderToUse.setSynchronizedWithTransaction(true);
            if (holderToUse != conHolder) {
                TransactionSynchronizationSupport.bindResource(dataSource, holderToUse);
            }
        }

        return con;
    }

        


    /**
     * {@inheritDoc}
     */
    public void releaseConnection(final Connection con, final DataSource dataSource) throws SQLException {
        if (con == null) {
            return;
        }

        if (dataSource != null) {
            ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationSupport.getResource(dataSource);
            if (conHolder != null && connectionEquals(conHolder, con)) {
                // It's the transactional Connection: Don't close it.
                conHolder.release();
                return;
            }
        }
        con.close();
    }

    private boolean connectionEquals(final ConnectionHolder conHolder, final Connection passedInCon) {
        if (!conHolder.hasConnection()) {
            return false;
        }
        Connection heldCon = conHolder.getConnection();
        // Explicitly check for identity too: for Connection handles that do not
        // implement "equals" properly, such as the ones Commons DBCP exposes).
        return (heldCon == passedInCon || heldCon.equals(passedInCon));
    }



    

}