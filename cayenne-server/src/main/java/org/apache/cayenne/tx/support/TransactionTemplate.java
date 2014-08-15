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

import java.lang.reflect.UndeclaredThrowableException;

import org.apache.cayenne.tx.TransactionDefinition;
import org.apache.cayenne.tx.TransactionOperations;
import org.apache.cayenne.tx.TransactionStatus;
import org.apache.cayenne.tx.TransactionalOperation;
import org.apache.cayenne.tx.exception.DependencyTransactionException;
import org.apache.cayenne.tx.exception.TransactionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TransactionTemplate extends TransactionDefinition implements TransactionOperations {

    /**
     * Default serial version.
     */
    private static final long serialVersionUID = 1L;

    /** Logger available to subclasses */
    protected final Log logger = LogFactory.getLog(getClass());

    private TransactionManager transactionManager;

    /**
     * Construct a new TransactionTemplate for bean usage.
     * <p>
     * Note: The PlatformTransactionManager needs to be set before any
     * <code>execute</code> calls.
     *
     * @see #setTransactionManager
     */
    public TransactionTemplate() {
    }

    /**
     * Construct a new TransactionTemplate using the given transaction manager.
     *
     * @param transactionManager
     *            the transaction management strategy to be used
     */
    public TransactionTemplate(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Construct a new TransactionTemplate using the given transaction manager,
     * taking its default settings from the given transaction definition.
     *
     * @param transactionManager
     *            the transaction management strategy to be used
     * @param transactionDefinition
     *            the transaction definition to copy the default settings from.
     *            Local properties can still be set to change values.
     */
    public TransactionTemplate(TransactionManager transactionManager, TransactionDefinition transactionDefinition) {
        super(transactionDefinition);
        this.transactionManager = transactionManager;
    }

    /**
     * Set the transaction management strategy to be used.
     */
    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Return the transaction management strategy to be used.
     */
    public TransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    public void afterPropertiesSet() {
        if (this.transactionManager == null) {
            throw new IllegalArgumentException("Property 'transactionManager' is required");
        }
    }

    @Override
    public <T> T performInTransaction(TransactionalOperation<T> action) throws TransactionException {
        return performInTransaction(this, action);
    }

    @Override
    public <T> T performInTransaction(final TransactionDefinition definition, final TransactionalOperation<T> action)
            throws TransactionException {
        TransactionStatus status = this.transactionManager.getTransaction(definition == null ? this : definition);
        T result;
        try {
            result = action.execute(status);
        } catch (RuntimeException ex) {
            // Transactional code threw application exception -> rollback
            rollbackOnException(status, ex);
            throw ex;
        } catch (Error err) {
            // Transactional code threw error -> rollback
            rollbackOnException(status, err);
            throw err;
        } catch (Exception ex) {
            // Transactional code threw unexpected exception -> rollback
            rollbackOnException(status, ex);
            throw new UndeclaredThrowableException(ex, "TransactionCallback threw undeclared checked exception");
        }
        this.transactionManager.commit(status);
        return result;

    }

    /**
     * Perform a rollback, handling rollback exceptions properly.
     *
     * @param status
     *            object representing the transaction
     * @param ex
     *            the thrown application exception or error
     * @throws TransactionException
     *             in case of a rollback error
     */
    private void rollbackOnException(TransactionStatus status, Throwable ex) throws TransactionException {
        logger.debug("Initiating transaction rollback on application exception", ex);
        try {
            this.transactionManager.rollback(status);
        } catch (DependencyTransactionException ex2) {
            logger.error("Application exception overridden by rollback exception", ex);
            ex2.initDependencyException(ex);
            throw ex2;
        } catch (RuntimeException ex2) {
            logger.error("Application exception overridden by rollback exception", ex);
            throw ex2;
        } catch (Error err) {
            logger.error("Application exception overridden by rollback error", ex);
            throw err;
        }
    }

}