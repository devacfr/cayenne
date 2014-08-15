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
package org.apache.cayenne.tx.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import javax.sql.DataSource;

import org.apache.cayenne.BaseContext;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.conn.support.ConnectionHolder;
import org.apache.cayenne.conn.support.DataSources;
import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.tx.TransactionDefinition;
import org.apache.cayenne.tx.exception.DependencyTransactionException;
import org.apache.cayenne.tx.exception.InvalidTimeoutException;
import org.apache.cayenne.tx.exception.SavepointNotSupportedException;
import org.apache.cayenne.tx.exception.TransactionException;
import org.apache.cayenne.tx.exception.TransactionUsageException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author devacfr<christophefriederich@mac.com>
 * @see 3.2
 */
public class DefaultTransaction implements Transaction {

    private static final Log logger = LogFactory.getLog(DefaultTransaction.class);

    private boolean newConnectionHolder;


    private boolean commitChanges;

    private ConnectionHolder connectionHolder;

    private boolean savepointAllowed = false;

    private final DataSource dataSource;

    private final ObjectContextFactory objectContextFactory;

    private int defaultTimeout = TransactionDefinition.TIMEOUT_DEFAULT;

    /**
	 *
	 */
    public DefaultTransaction(final DataSource dataSource, final ObjectContextFactory objectContextFactory) {
        if (dataSource == null) {
            throw new IllegalArgumentException("datasource is required");
        }
        if (objectContextFactory == null) {
            throw new IllegalArgumentException("objectContextFactory is required");
        }
        this.dataSource = dataSource;
        this.objectContextFactory = objectContextFactory;
        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationSupport.getResource(this.dataSource);
        setConnectionHolder(conHolder, false);
    }

    public void setConnectionHolder(final ConnectionHolder connectionHolder, final boolean newConnectionHolder) {
        setConnectionHolder(connectionHolder);
        this.newConnectionHolder = newConnectionHolder;
    }

    public boolean isNewConnectionHolder() {
        return newConnectionHolder;
    }

    /**
     * @param commitChanges
     *            the commitChanges to set
     */
    public void setCommitChanges(boolean commitChanges) {
        this.commitChanges = commitChanges;
    }

    /**
     * @return the commitChanges
     */
    public boolean isCommitChanges() {
        return commitChanges;
    }

    @Override
    public boolean hasTransaction() {
        return getConnectionHolder() != null && getConnectionHolder().isTransactionActive();
    }

    public final void setDefaultTimeout(int defaultTimeout) {
        if (defaultTimeout < TransactionDefinition.TIMEOUT_DEFAULT) {
            throw new InvalidTimeoutException("Invalid default timeout", defaultTimeout);
        }
        this.defaultTimeout = defaultTimeout;
    }

    public final int getDefaultTimeout() {
        return this.defaultTimeout;
    }

    

    public void setRollbackOnly() {
        getConnectionHolder().setRollbackOnly();
    }

    @Override
    public boolean isRollbackOnly() {
        return getConnectionHolder().isRollbackOnly();
    }

    @Override
    public void commitChanges() {
        ConnectionHolder connectionHolder = getConnectionHolder();
        connectionHolder.getObjectContext().commitChanges();
    }

    @Override
    public void begin(TransactionDefinition definition) throws TransactionException {
        Connection con = null;

        try {
            if (getConnectionHolder() == null || getConnectionHolder().isSynchronizedWithTransaction()) {
                Connection newCon = dataSource.getConnection();
                if (logger.isDebugEnabled()) {
                    logger.debug("Acquired Connection [" + newCon + "] for JDBC transaction");
                }
                setConnectionHolder(new ConnectionHolder(newCon, this.objectContextFactory.createContext()), true);
            }

            // set the current context
            BaseContext.bindThreadObjectContext(getConnectionHolder().getObjectContext());

            con = getConnectionHolder().getConnection();
            
            getConnectionHolder().begin(definition, determineTimeout(definition));

            // Bind the session holder to the thread.
            if (isNewConnectionHolder()) {
                TransactionSynchronizationSupport.bindResource(dataSource, getConnectionHolder());
            }
        }

        catch (SQLException ex) {
            DataSources.releaseConnection(con, this.dataSource);
            throw new TransactionException("Could not open JDBC Connection for transaction", ex);
        }
    }

    @Override
    public Object suspend() throws TransactionException {
        setConnectionHolder(null);
        ConnectionHolder connectionHolder = (ConnectionHolder) TransactionSynchronizationSupport
                .unbindResource(this.dataSource);
        BaseContext.bindThreadObjectContext(null);
        return connectionHolder;
    }

    @Override
    public void resume(Object suspendedResources) throws TransactionException {
        ConnectionHolder conHolder = (ConnectionHolder) suspendedResources;
        TransactionSynchronizationSupport.bindResource(this.dataSource, conHolder);
        BaseContext.bindThreadObjectContext(conHolder.getObjectContext());
    }

    @Override
    public void commit() throws TransactionException {
        Connection con = getConnectionHolder().getConnection();

        try {
            if (commitChanges)
                commitChanges();
            con.commit();
        } catch (SQLException ex) {
            throw new DependencyTransactionException("Could not commit JDBC transaction", ex);
        } catch (CayenneRuntimeException ex) {
            throw new DependencyTransactionException("Could not commit JDBC transaction", ex);
        }
    }

    @Override
    public void rollback() throws TransactionException {
        Connection con = getConnectionHolder().getConnection();
        ObjectContext dataContext = getConnectionHolder().getObjectContext();
        try {
            if (dataContext != null && commitChanges) {
                dataContext.rollbackChanges();
            }
            con.rollback();
        } catch (SQLException ex) {
            throw new DependencyTransactionException("Could not commit JDBC transaction", ex);
        } catch (CayenneRuntimeException ex) {
            throw new DependencyTransactionException("Could not commit JDBC transaction", ex);
        }
    }

    @Override
    public void close() {
        // Remove the connection holder from the thread, if exposed.
        if (isNewConnectionHolder()) {
            TransactionSynchronizationSupport.unbindResource(this.dataSource);
        }

        // Reset connection.
        getConnectionHolder().finish();
        Connection con = getConnectionHolder().getConnection();

        if (isNewConnectionHolder()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Releasing JDBC Connection [" + con + "] after transaction");
            }
            DataSources.releaseConnection(con, this.dataSource);
        }        
    }

    public void setConnectionHolder(ConnectionHolder connectionHolder) {
        this.connectionHolder = connectionHolder;
    }

    public ConnectionHolder getConnectionHolder() {
        return this.connectionHolder;
    }

    public boolean hasConnectionHolder() {
        return (this.connectionHolder != null);
    }

    public void setSavepointAllowed(boolean savepointAllowed) {
        this.savepointAllowed = savepointAllowed;
    }

    public boolean isSavepointAllowed() {
        return this.savepointAllowed;
    }

    @Override
    public Object createSavepoint() throws TransactionException {
        ConnectionHolder conHolder = getConnectionHolderForSavepoint();
        try {
            if (!conHolder.supportsSavepoints()) {
                throw new TransactionException(
                        "Cannot create a nested transaction because savepoints are not supported by your JDBC driver");
            }
        } catch (Throwable ex) {
            throw new TransactionException(
                    "Cannot create a nested transaction because your JDBC driver is not a JDBC 3.0 driver", ex);
        }
        try {
            return conHolder.createSavepoint();
        } catch (Throwable ex) {
            throw new TransactionException("Could not create JDBC savepoint", ex);
        }
    }

    /**
     * This implementation rolls back to the given JDBC 3.0 Savepoint.
     *
     * @see java.sql.Connection#rollback(java.sql.Savepoint)
     */
    @Override
    public void rollbackToSavepoint(Object savepoint) throws TransactionException {
        try {
            getConnectionHolderForSavepoint().getConnection().rollback((Savepoint) savepoint);
        } catch (Throwable ex) {
            throw new DependencyTransactionException("Could not roll back to JDBC savepoint", ex);
        }
    }

    /**
     * This implementation releases the given JDBC 3.0 Savepoint.
     *
     * @see java.sql.Connection#releaseSavepoint
     */
    @Override
    public void releaseSavepoint(Object savepoint) throws TransactionException {
        try {
            getConnectionHolderForSavepoint().getConnection().releaseSavepoint((Savepoint) savepoint);
        } catch (Throwable ex) {
            logger.debug("Could not explicitly release JDBC savepoint", ex);
        }
    }

    protected ConnectionHolder getConnectionHolderForSavepoint() throws TransactionException {
        if (!isSavepointAllowed()) {
            throw new SavepointNotSupportedException("Transaction manager does not allow nested transactions");
        }
        if (!hasConnectionHolder()) {
            throw new TransactionUsageException("Cannot create nested transaction if not exposing a JDBC transaction");
        }
        return getConnectionHolder();
    }

    protected int determineTimeout(TransactionDefinition definition) {
        if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
            return definition.getTimeout();
        }
        return this.defaultTimeout;
    }
}
