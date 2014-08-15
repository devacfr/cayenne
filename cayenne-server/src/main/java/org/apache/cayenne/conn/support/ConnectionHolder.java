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
import java.sql.Savepoint;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.tx.CayenneTransactionManager;
import org.apache.cayenne.tx.TransactionDefinition;
import org.apache.cayenne.tx.TransactionDefinition.IsolationLevel;
import org.apache.cayenne.tx.exception.TransactionTimedOutException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Connection holder maintains the relation between JDBC Connection and cayenne
 * context to the thread, for a specific DataSource.
 * <p>
 * this class supports features:
 * <ul>
 * <li>Rollback-only for nested transactions</li>
 * <li>creation of JDBC 3.0 Savepoint</li>
 * <li>Connection's reference counting that internally tracks how many
 * connection usage has.</li>
 * </ul>
 * <p>
 * Note: NOT use directly by applications.
 * 
 * @author devacfr<christophefriederich@mac.com>
 * 
 * @since 3.2
 * 
 * @see DataSources
 * @see CayenneTransactionManager
 */
public class ConnectionHolder {

    private static final Log logger = LogFactory.getLog(ConnectionHolder.class);

    /**
     * cayenne context link to connection.
     */
    private ObjectContext baseContext;

    /**
     * Arbritary Savepoint name prefix.
     */
    public static final String SAVEPOINT_NAME_PREFIX = "SAVEPOINT_";

    /**
     * connection handler.
     */
    private ConnectionHandle connectionHandle;

    /**
     * the current JDBC connection
     */
    private Connection currentConnection;

    /**
     * indicating wheter the transaction bound to this holder is active.
     */
    private boolean transactionActive = false;

    /**
     * cached flag form JDBC Metadata indicating whether the the database
     * supports Savepoint.
     */
    private Boolean savepointsSupported;

    /**
     * incremental counter allowing generate unique savepoint name.
     */
    private int savepointCounter = 0;

    /**
     * 
     */
    private boolean synchronizedWithTransaction = false;

    /**
     * rollback-only flag
     */
    private boolean rollbackOnly = false;

    /**
     * allows to check the timeout of transaction
     */
    private Long timeout;

    /**
     * Connection's reference count.
     */
    private int referenceCount = 0;

    /**
     * store the previous isolation level, using for restore parent nested
     * transaction.
     */
    private Integer previousIsolationLevel;

    /**
     * indicating whether the holder must restore
     * {@link Connection#setAutoCommit(boolean) auto-commit mode} on connection.
     */
    private boolean restoreAutoCommit;

    /**
     * Create a new ConnectionHolder for the given JDBC Connection, assuming
     * that there is no ongoing transaction.
     * 
     * @param connection
     *            the JDBC Connection to hold (never <code>null</code>).
     * @param baseContext
     *            the cayenne context bound to connection (never
     *            <code>null</code>).
     */
    public ConnectionHolder(final Connection connection, final ObjectContext baseContext) {
        if (connection == null) {
            throw new IllegalArgumentException("connection argument is required");
        }
        this.connectionHandle = new ConnectionHandle(connection);
        setObjectContext(baseContext);
    }

    /**
     * Clears the transactional state of this holder.
     */
    protected void clear() {
        this.synchronizedWithTransaction = false;
        this.rollbackOnly = false;
        this.timeout = null;
        this.transactionActive = false;
        this.savepointsSupported = null;
        this.savepointCounter = 0;
    }

    /**
     * Initialize the current connection of holder for transaction.
     * 
     * @param definition
     *            the transaction definition to use
     * @param timeout
     *            the timeout to use (optional if set with
     *            {@link TransactionDefinition.TIMEOUT_DEFAULT}).
     * @throws SQLException
     */
    public void begin(final TransactionDefinition definition, final int timeout) throws SQLException {
        final Connection connection = getConnection();
        prepareConnection(connection, definition);
        setSynchronizedWithTransaction(true);
        setTransactionActive(true);
        if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
            setTimeout(timeout);
        }
    }

    /**
     * Restore the current connection and reset this holder.
     */
    public void finish() {

        restoreConnection(getConnection());
        clear();
    }

    /**
     * Gets indicating whether JDBC 3.0 Savepoints are supported.
     * <p>
     * <em>Caches the flag for the lifetime of this holder.</em>
     * 
     * @return Returns <code>true</code> whether JDBC 3.0 Savepoints are
     *         supported, otherwise <code>false</code>.
     * @throws SQLException
     *             if thrown by the JDBC driver
     */
    public final boolean supportsSavepoints() throws SQLException {
        if (this.savepointsSupported == null) {
            this.savepointsSupported = new Boolean(getConnection().getMetaData().supportsSavepoints());
        }
        return this.savepointsSupported.booleanValue();
    }

    /**
     * Creates a new JDBC 3.0 Savepoint for the current Connection, using
     * generated savepoint names that are unique for the Connection.
     * 
     * @return Returns new {@link Savepoint Savepoint} instance (never
     *         <code>null</code>).
     * @throws SQLException
     *             if thrown by the JDBC driver
     */
    public Savepoint createSavepoint() throws SQLException {
        this.savepointCounter++;
        return getConnection().setSavepoint(SAVEPOINT_NAME_PREFIX + this.savepointCounter);
    }

    /**
     * Gets the Cayenne context.
     * 
     * @return Returns the Cayenne context (never <code>null</code>).
     */
    public final ObjectContext getObjectContext() {
        return baseContext;
    }

    /**
     * Sets the Cayenne context.
     * 
     * @param baseContext
     *            the Cayenne context (never <code>null</code>).
     */
    protected final void setObjectContext(final ObjectContext baseContext) {
        if (baseContext == null) {
            throw new IllegalArgumentException("baseContext argument is required");
        }
        this.baseContext = baseContext;
    }

    /**
     * Gets indicating whether this holder currently has a connection.
     * 
     * @return Returns <code>true</code> whether this holder currently has a
     *         Connection, otherwise <code>false</code>.
     */
    protected final boolean hasConnection() {
        return (this.connectionHandle != null);
    }

    /**
     * Sets indicating whether this holder participate to active transaction.
     * 
     * @param transactionActive
     */
    protected final void setTransactionActive(final boolean transactionActive) {
        this.transactionActive = transactionActive;
    }

    /**
     * Gets indicating whether this holder represents an active, JDBC-managed
     * transaction.
     * 
     * @return Return <code>true</code> whether this holder represents an
     *         active, JDBC-managed transaction, otherwise <code>false</code>.
     */
    public final boolean isTransactionActive() {
        return this.transactionActive;
    }

    /**
     * Sets this holder with the given JDBC Connection.
     * <p>
     * Used for releasing the Connection on suspend (with a <code>null</code>
     * argument) and setting a fresh Connection on resume.
     * 
     * @param connection
     *            the JDBC Connection to hold (may be <code>null</code>).
     */
    protected final void setConnection(final Connection connection) {
        if (this.currentConnection != null) {
            this.currentConnection = null;
        }
        if (connection != null) {
            this.connectionHandle = new ConnectionHandle(connection);
        } else {
            this.connectionHandle = null;
        }
    }

    /**
     * Gets the current Connection held by this ConnectionHolder.
     * <p>
     * This will be the same Connection until <code>release</code> gets called
     * on the ConnectionHolder, which will reset the held Connection, fetching a
     * new Connection on demand.
     * 
     * @return Returns the current Connection (never <code>null</code>).
     * @see #release()
     */
    public final Connection getConnection() {
        if (this.connectionHandle == null) {
            throw new RuntimeException("Active Connection is required");
        }
        if (this.currentConnection == null) {
            this.currentConnection = this.connectionHandle.getConnection();
        }
        return this.currentConnection;
    }

    /**
     * Sets indicating whether this holder is synchronized with a transaction.
     * 
     * @param synchronizedWithTransaction
     *            flag indicating whether this holder is synchronized with
     *            transaction.
     */
    public final void setSynchronizedWithTransaction(final boolean synchronizedWithTransaction) {
        this.synchronizedWithTransaction = synchronizedWithTransaction;
    }

    /**
     * Gets indicating whether this holder is synchronized with a transaction.
     * 
     * @return Returns <code>true</code> whether this holder is synchronized
     *         with a transaction, otherwise <code>false</code>.
     */
    public final boolean isSynchronizedWithTransaction() {
        return this.synchronizedWithTransaction;
    }

    /**
     * Sets the current transaction isolation level.
     * 
     * @param previousIsolationLevel
     *            the current transaction isolation level to store (may be
     *            <code>null</code>).
     * @see Connection#getTransactionIsolation()
     */
    protected final void setPreviousIsolationLevel(Integer previousIsolationLevel) {
        this.previousIsolationLevel = previousIsolationLevel;
    }

    /**
     * Gets the previous transaction isolation level.
     * 
     * @return Returns Integer representing the previous transaction isolatio
     *         level (may be <code>null</code>).
     */
    public final Integer getPreviousIsolationLevel() {
        return this.previousIsolationLevel;
    }

    /**
     * 
     * @param restoreAutoCommit
     */
    public final void setRestoreAutoCommit(boolean restoreAutoCommit) {
        this.restoreAutoCommit = restoreAutoCommit;
    }

    /**
     * Gets indicating whether this holder must restore auto-commit of current
     * connection after completion of transaction.
     * 
     * @return
     */
    public final boolean isRestoreAutoCommit() {
        return restoreAutoCommit;
    }

    /**
     * Marks the transaction as rollback-only.
     */
    public final void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    /**
     * Gets indicating whether the transaction is marked as rollback-only.
     * 
     * @return Returns <code>true</code> whether the transaction is marked
     *         rollback-only, otherwise <code>false</code>.
     */
    public final boolean isRollbackOnly() {
        return this.rollbackOnly;
    }

    /**
     * Set the timeout for this holder in seconds.
     * 
     * @param seconds
     *            number of seconds until expiration
     */
    protected final void setTimeout(final int seconds) {
        this.timeout = System.currentTimeMillis() + (seconds * 1000);
    }

    /**
     * Gets indicating whether this holder has an associated timeout.
     * 
     * @return Returns <code>true</code> whether this holder has an associated
     *         timeout, otherwise <code>false</code>.
     */
    public final boolean hasTimeout() {
        return (this.timeout != null);
    }

    /**
     * Gets the time to live for this holder in seconds.
     * Rounds up eagerly, e.g. 9.00001 still to 10.
     * @return Returns number of seconds until expiration.
     * @throws TransactionTimedOutException if the deadline has already been reached.
     */
    public final int getTimeToLive() {
        if (this.timeout == null) {
            throw new IllegalStateException("No timeout specified for this resource holder");
        }
        long timeToLive = this.timeout - System.currentTimeMillis();
        double diff = ((double) timeToLive) / 1000;
        int secs = (int) Math.ceil(diff);
        checkTransactionTimeout(secs <= 0);
        return secs;
    }

    /**
     * Checks if the transaction deadline has been reached, then transaction is
     * set rollback-only and throw a {@link TransactionTimedOutException}.
     * 
     * @param deadlineReached
     *            <code>true</code> if reached
     * @throws TransactionTimedOutException
     *             if the transaction timed out.
     */
    private void checkTransactionTimeout(final boolean deadlineReached) throws TransactionTimedOutException {
        if (deadlineReached) {
            setRollbackOnly();
            throw new TransactionTimedOutException("Transaction timed out: deadline was " + this.timeout);
        }
    }

    /**
     * Decrements the connection’s reference count. (required)
     * <p>
     * The current connection is unheld when its reference count reaches 0.
     */
    public final void release() {
        this.referenceCount--;
        if (!isOpen()) {
            this.currentConnection = null;
        }
    }

    /**
     * Increments the connection's reference count. (required)
     * <p>
     * Call this method you want to prevent connection from being unheld until
     * you have finished using it.
     */
    public final void retain() {
        this.referenceCount++;
    }

    /**
     * Gets indicating whether there are still open references to this holder.
     * 
     * @return
     */
    public boolean isOpen() {
        return (this.referenceCount > 0);
    }

    /**
     * Resets transactional and reference state.
     */
    public void reset() {
        clear();
        this.referenceCount = 0;
    }

    /**
     * Applies the given Connection with the given transaction modes.
     * 
     * @param connection
     *            the Connection to prepare (never <code>null</code>).
     * @param definition
     *            the transaction definition to apply
     * @return the previous isolation level, if any
     * @throws SQLException
     *             if thrown by JDBC methods
     */
    private void prepareConnection(final Connection connection, final TransactionDefinition definition)
            throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("No Connection specified");
        }

        if (connection.getAutoCommit()) {
            setRestoreAutoCommit(true);
            if (logger.isDebugEnabled()) {
                logger.debug("Switching JDBC Connection [" + connection + "] to manual commit");
            }
            connection.setAutoCommit(false);
        }

        // Set read-only flag.
        if (definition != null && definition.isReadOnly()) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Setting JDBC Connection [" + connection + "] read-only");
                }
                connection.setReadOnly(true);
            } catch (Throwable ex) {
                // SQLException or UnsupportedOperationException
                // -> ignore, it's just a hint anyway.
                logger.debug("Could not set JDBC Connection read-only", ex);
            }
        }

        if (definition != null && definition.getIsolationLevel() != IsolationLevel.Default) {
            if (logger.isDebugEnabled()) {
                logger.debug("Changing isolation level of JDBC Connection [" + connection + "] to "
                        + definition.getIsolationLevel());
            }
            previousIsolationLevel = new Integer(connection.getTransactionIsolation());
            connection.setTransactionIsolation(definition.getIsolationLevel().intValue());
        }
    }

    /**
     * Restores the given Connection after a transaction, regarding read-only
     * flag and isolation level.
     * 
     * @param connection
     *            the Connection to reset (never <code>null</code>).
     */
    private void restoreConnection(final Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("No Connection specified");
        }
        try {
            if (isRestoreAutoCommit()) {
                connection.setAutoCommit(true);
            }
            // restore transaction isolation to previous value, if changed for
            // the
            // transaction.
            if (previousIsolationLevel != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Resetting isolation level of JDBC Connection [" + connection + "] to "
                            + previousIsolationLevel);
                }
                connection.setTransactionIsolation(previousIsolationLevel.intValue());
            }

            // Reset read-only flag.
            if (connection.isReadOnly()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Resetting read-only flag of JDBC Connection [" + connection + "]");
                }
                connection.setReadOnly(false);
            }
        } catch (Throwable ex) {
            logger.debug("Could not restore JDBC Connection", ex);
        }
    }

}