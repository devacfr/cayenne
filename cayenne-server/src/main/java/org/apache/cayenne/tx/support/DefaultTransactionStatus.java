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

import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.tx.TransactionStatus;
import org.apache.cayenne.tx.exception.TransactionException;
import org.apache.cayenne.tx.exception.TransactionUsageException;

public class DefaultTransactionStatus implements TransactionStatus {

    private boolean rollbackOnly = false;

    private boolean completed = false;

    private Object savepoint;

    private final Transaction transaction;

    private final boolean newTransaction;

    private final boolean newSynchronization;

    private final boolean readOnly;

    private final boolean debug;

    private final Object suspendedResources;

    public DefaultTransactionStatus(final Transaction transaction, final boolean newTransaction,
            final boolean newSynchronization, final boolean readOnly, final boolean debug,
            final Object suspendedResources) {

        this.transaction = transaction;
        this.newTransaction = newTransaction;
        this.newSynchronization = newSynchronization;
        this.readOnly = readOnly;
        this.debug = debug;
        this.suspendedResources = suspendedResources;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRollbackOnly() {
        return (isLocalRollbackOnly() || isGlobalRollbackOnly());
    }

    public boolean isLocalRollbackOnly() {
        return this.rollbackOnly;
    }

    public Transaction getTransaction() {
        return this.transaction;
    }

    public boolean hasTransaction() {
        return (this.transaction != null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNewTransaction() {
        return (hasTransaction() && this.newTransaction);
    }

    public boolean isNewSynchronization() {
        return this.newSynchronization;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public boolean isDebug() {
        return this.debug;
    }

    public Object getSuspendedResources() {
        return this.suspendedResources;
    }

    public boolean isGlobalRollbackOnly() {
        return this.transaction.isRollbackOnly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() {
        this.transaction.commitChanges();
    }

    public void setCompleted() {
        this.completed = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCompleted() {
        return this.completed;
    }

    protected void setSavepoint(Object savepoint) {
        this.savepoint = savepoint;
    }

    /**
     * Get the savepoint for this transaction, if any.
     */
    protected Object getSavepoint() {
        return this.savepoint;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasSavepoint() {
        return (this.savepoint != null);
    }

    public void createAndHoldSavepoint() throws TransactionException {
        setSavepoint(getSavepointManager().createSavepoint());
    }

    public void rollbackToHeldSavepoint() throws TransactionException {
        if (!hasSavepoint()) {
            throw new TransactionUsageException("No savepoint associated with current transaction");
        }
        getSavepointManager().rollbackToSavepoint(getSavepoint());
        setSavepoint(null);
    }

    public void releaseHeldSavepoint() throws TransactionException {
        if (!hasSavepoint()) {
            throw new TransactionUsageException("No savepoint associated with current transaction");
        }
        getSavepointManager().releaseSavepoint(getSavepoint());
        setSavepoint(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object createSavepoint() throws TransactionException {
        return getSavepointManager().createSavepoint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollbackToSavepoint(Object savepoint) throws TransactionException {
        getSavepointManager().rollbackToSavepoint(savepoint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseSavepoint(Object savepoint) throws TransactionException {
        getSavepointManager().releaseSavepoint(savepoint);
    }

    protected SavepointAccessor getSavepointManager() {
        if (!isTransactionSavepointManager()) {
            throw new TransactionException("Transaction object [" + getTransaction() + "] does not support savepoints");
        }
        return getTransaction();
    }

    public boolean isTransactionSavepointManager() {
        return (getTransaction() instanceof SavepointAccessor);
    }

}
