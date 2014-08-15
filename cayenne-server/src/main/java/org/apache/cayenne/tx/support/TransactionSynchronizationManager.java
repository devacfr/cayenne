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

import java.util.List;
import java.util.Map;

import org.apache.cayenne.tx.TransactionDefinition.IsolationLevel;

/**
 * Central helper that manages resources and transaction synchronizations per
 * thread. To be used by resource management code but not by typical application
 * code.
 *
 * <p>
 * Supports one resource per key without overwriting, that is, a resource needs
 * to be removed before a new one can be set for the same key. Supports a list
 * of transaction synchronizations if synchronization is active.
 *
 * <p>
 * Resource management code should check for thread-bound resources, e.g. JDBC
 * Connections or Hibernate Sessions, via <code>getResource</code>. Such code is
 * normally not supposed to bind resources to threads, as this is the
 * responsibility of transaction managers. A further option is to lazily bind on
 * first use if transaction synchronization is active, for performing
 * transactions that span an arbitrary number of resources.
 *
 * <p>
 * Transaction synchronization must be activated and deactivated by a
 * transaction manager via {@link #initSynchronization()} and
 * {@link #clearSynchronization()}. This is automatically supported by
 * {@link AbstractPlatformTransactionManager}, and thus by all standard Spring
 * transaction managers, such as
 * {@link org.springframework.transaction.jta.JtaTransactionManager} and
 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}.
 *
 * <p>
 * Resource management code should only register synchronizations when this
 * manager is active, which can be checked via {@link #isSynchronizationActive};
 * it should perform immediate resource cleanup else. If transaction
 * synchronization isn't active, there is either no current transaction, or the
 * transaction manager doesn't support transaction synchronization.
 *
 * <p>
 * Synchronization is for example used to always return the same resources
 * within a JTA transaction, e.g. a JDBC Connection or a Hibernate Session for
 * any given DataSource or SessionFactory, respectively.
 *
 * @author Juergen Hoeller
 * @since 02.06.2003
 * @see #isSynchronizationActive
 * @see #registerSynchronization
 * @see TransactionSynchronization
 * @see AbstractPlatformTransactionManager#setTransactionSynchronization
 * @see org.springframework.transaction.jta.JtaTransactionManager
 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
 * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection
 */
public interface TransactionSynchronizationManager {

    /**
     * Return all resources that are bound to the current thread.
     * <p>
     * Mainly for debugging purposes. Resource managers should always invoke
     * <code>hasResource</code> for a specific resource key that they are
     * interested in.
     * 
     * @return a Map with resource keys (usually the resource factory) and
     *         resource values (usually the active resource object), or an empty
     *         Map if there are currently no resources bound
     * @see #hasResource
     */
    Map<Object, Object> getResourceMap();

    /**
     * Check if there is a resource for the given key bound to the current
     * thread.
     * 
     * @param key
     *            the key to check (usually the resource factory)
     * @return if there is a value bound to the current thread
     * @see ResourceTransactionManager#getResourceFactory()
     */
    boolean hasResource(Object key);

    /**
     * Retrieve a resource for the given key that is bound to the current
     * thread.
     * 
     * @param key
     *            the key to check (usually the resource factory)
     * @return a value bound to the current thread (usually the active resource
     *         object), or <code>null</code> if none
     * @see ResourceTransactionManager#getResourceFactory()
     */
    Object getResource(Object key);

    /**
     * Bind the given resource for the given key to the current thread.
     * 
     * @param key
     *            the key to bind the value to (usually the resource factory)
     * @param value
     *            the value to bind (usually the active resource object)
     * @throws IllegalStateException
     *             if there is already a value bound to the thread
     * @see ResourceTransactionManager#getResourceFactory()
     */
    void bindResource(Object key, Object value) throws IllegalStateException;

    /**
     * Unbind a resource for the given key from the current thread.
     * 
     * @param key
     *            the key to unbind (usually the resource factory)
     * @return the previously bound value (usually the active resource object)
     * @throws IllegalStateException
     *             if there is no value bound to the thread
     * @see ResourceTransactionManager#getResourceFactory()
     */
    Object unbindResource(Object key) throws IllegalStateException;

    /**
     * Unbind a resource for the given key from the current thread.
     * 
     * @param key
     *            the key to unbind (usually the resource factory)
     * @return the previously bound value, or <code>null</code> if none bound
     */
    Object unbindResourceIfPossible(Object key);

    /**
     * Return if transaction synchronization is active for the current thread.
     * Can be called before register to avoid unnecessary instance creation.
     * 
     * @see #registerSynchronization
     */
    boolean isSynchronizationActive();

    /**
     * Activate transaction synchronization for the current thread. Called by a
     * transaction manager on transaction begin.
     * 
     * @throws IllegalStateException
     *             if synchronization is already active
     */
    void initSynchronization() throws IllegalStateException;

    /**
     * Register a new transaction synchronization for the current thread.
     * Typically called by resource management code.
     * <p>
     * Note that synchronizations can implement the
     * {@link org.springframework.core.Ordered} interface. They will be executed
     * in an order according to their order value (if any).
     * 
     * @param synchronization
     *            the synchronization object to register
     * @throws IllegalStateException
     *             if transaction synchronization is not active
     * @see org.springframework.core.Ordered
     */
    void registerSynchronization(TransactionSynchronizer synchronization) throws IllegalStateException;

    /**
     * Return an unmodifiable snapshot list of all registered synchronizations
     * for the current thread.
     * 
     * @return unmodifiable List of TransactionSynchronization instances
     * @throws IllegalStateException
     *             if synchronization is not active
     * @see TransactionSynchronization
     */
    List<TransactionSynchronizer> getSynchronizations() throws IllegalStateException;

    /**
     * Deactivate transaction synchronization for the current thread. Called by
     * the transaction manager on transaction cleanup.
     * 
     * @throws IllegalStateException
     *             if synchronization is not active
     */
    void clearSynchronization() throws IllegalStateException;

    /**
     * Expose the name of the current transaction, if any. Called by the
     * transaction manager on transaction begin and on cleanup.
     * 
     * @param name
     *            the name of the transaction, or <code>null</code> to reset it
     * @see org.springframework.transaction.TransactionDefinition#getName()
     */
    void setCurrentTransactionName(String name);

    /**
     * Return the name of the current transaction, or <code>null</code> if none
     * set. To be called by resource management code for optimizations per use
     * case, for example to optimize fetch strategies for specific named
     * transactions.
     * 
     * @see org.springframework.transaction.TransactionDefinition#getName()
     */
    String getCurrentTransactionName();

    /**
     * Expose a read-only flag for the current transaction. Called by the
     * transaction manager on transaction begin and on cleanup.
     * 
     * @param readOnly
     *            <code>true</code> to mark the current transaction as
     *            read-only; <code>false</code> to reset such a read-only marker
     * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
     */
    void setCurrentTransactionReadOnly(boolean readOnly);

    /**
     * Return whether the current transaction is marked as read-only. To be
     * called by resource management code when preparing a newly created
     * resource (for example, a Hibernate Session).
     * <p>
     * Note that transaction synchronizations receive the read-only flag as
     * argument for the <code>beforeCommit</code> callback, to be able to
     * suppress change detection on commit. The present method is meant to be
     * used for earlier read-only checks, for example to set the flush mode of a
     * Hibernate Session to "FlushMode.NEVER" upfront.
     * 
     * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
     * @see TransactionSynchronization#beforeCommit(boolean)
     */
    boolean isCurrentTransactionReadOnly();

    /**
     * Expose an isolation level for the current transaction. Called by the
     * transaction manager on transaction begin and on cleanup.
     * 
     * @param isolationLevel
     *            the isolation level to expose, according to the JDBC
     *            Connection constants (equivalent to the corresponding Spring
     *            TransactionDefinition constants), or <code>null</code> to
     *            reset it
     * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
     * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
     * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
     * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
     * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
     * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
     * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
     * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
     * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
     */
    void setCurrentTransactionIsolationLevel(IsolationLevel isolationLevel);

    /**
     * Return the isolation level for the current transaction, if any. To be
     * called by resource management code when preparing a newly created
     * resource (for example, a JDBC Connection).
     * 
     * @return the currently exposed isolation level, according to the JDBC
     *         Connection constants (equivalent to the corresponding Spring
     *         TransactionDefinition constants), or <code>null</code> if none
     * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
     * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
     * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
     * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
     * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
     * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
     * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
     * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
     * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
     */
    IsolationLevel getCurrentTransactionIsolationLevel();

    /**
     * Expose whether there currently is an actual transaction active. Called by
     * the transaction manager on transaction begin and on cleanup.
     * 
     * @param active
     *            <code>true</code> to mark the current thread as being
     *            associated with an actual transaction; <code>false</code> to
     *            reset that marker
     */
    void setActualTransactionActive(boolean active);

    /**
     * Return whether there currently is an actual transaction active. This
     * indicates whether the current thread is associated with an actual
     * transaction rather than just with active transaction synchronization.
     * <p>
     * To be called by resource management code that wants to discriminate
     * between active transaction synchronization (with or without backing
     * resource transaction; also on PROPAGATION_SUPPORTS) and an actual
     * transaction being active (with backing resource transaction; on
     * PROPAGATION_REQUIRES, PROPAGATION_REQUIRES_NEW, etc).
     * 
     * @see #isSynchronizationActive()
     */
    boolean isActualTransactionActive();

    /**
     * Clear the entire transaction synchronization state for the current
     * thread: registered synchronizations as well as the various transaction
     * characteristics.
     * 
     * @see #clearSynchronization()
     * @see #setCurrentTransactionName
     * @see #setCurrentTransactionReadOnly
     * @see #setCurrentTransactionIsolationLevel
     * @see #setActualTransactionActive
     */
    void clear();

}
