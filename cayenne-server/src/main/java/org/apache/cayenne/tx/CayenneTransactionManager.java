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
package org.apache.cayenne.tx;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;

import javax.sql.DataSource;

import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.tx.TransactionDefinition.IsolationLevel;
import org.apache.cayenne.tx.TransactionDefinition.PropagationBehavior;
import org.apache.cayenne.tx.exception.IllegalTransactionStateException;
import org.apache.cayenne.tx.exception.InvalidTimeoutException;
import org.apache.cayenne.tx.exception.TransactionException;
import org.apache.cayenne.tx.support.DefaultTransaction;
import org.apache.cayenne.tx.support.DefaultTransactionStatus;
import org.apache.cayenne.tx.support.DefaultTransactionSynchronizationManager;
import org.apache.cayenne.tx.support.TransactionManager;
import org.apache.cayenne.tx.support.TransactionSynchronizationManager;
import org.apache.cayenne.tx.support.TransactionSynchronizationSupport;
import org.apache.cayenne.tx.support.TransactionSynchronizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CayenneTransactionManager implements TransactionManager, Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 539461528764376410L;

    protected static Log logger = LogFactory.getLog(CayenneTransactionManager.class);

    private Synchronization transactionSynchronization = Synchronization.Always;

    private int defaultTimeout = TransactionDefinition.TIMEOUT_DEFAULT;

    private boolean globalRollbackOnParticipationFailure = true;

    private DataSource dataSource;

    private ObjectContextFactory objectContextFactory;

    private TransactionSynchronizationManager transactionSynchronizationManager;

    public CayenneTransactionManager() {
        transactionSynchronizationManager = new DefaultTransactionSynchronizationManager();
    }

    public CayenneTransactionManager(final DataSource dataSource, final ObjectContextFactory objectContextFactory) {
        this();
        if (dataSource == null) {
            throw new IllegalArgumentException("datasource is required");
        }
        if (objectContextFactory == null) {
            throw new IllegalArgumentException("objectContextFactory is required");
        }
        setDataSource(dataSource);
        this.objectContextFactory = objectContextFactory;
    }

    public void setDataSource(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Return the JDBC DataSource that this instance manages transactions for.
     */
    public DataSource getDataSource() {
        return this.dataSource;
    }

    public final void setTransactionSynchronization(Synchronization transactionSynchronization) {
        this.transactionSynchronization = transactionSynchronization;
    }

    public final Synchronization getTransactionSynchronization() {
        return this.transactionSynchronization;
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

    /**
     * Set whether to globally mark an existing transaction as rollback-only
     * after a participating transaction failed.
     * <p>
     * Default is "true": If a participating transaction (e.g. with
     * PROPAGATION_REQUIRES or PROPAGATION_SUPPORTS encountering an existing
     * transaction) fails, the transaction will be globally marked as
     * rollback-only. The only possible outcome of such a transaction is a
     * rollback: The transaction originator <i>cannot</i> make the transaction
     * commit anymore.
     * <p>
     * Switch this to "false" to let the transaction originator make the
     * rollback decision. If a participating transaction fails with an
     * exception, the caller can still decide to continue with a different path
     * within the transaction. However, note that this will only work as long as
     * all participating resources are capable of continuing towards a
     * transaction commit even after a data access failure: This is generally
     * not the case for a Hibernate Session, for example; neither is it for a
     * sequence of JDBC insert/update/delete operations.
     * <p>
     * <b>Note:</b>This flag only applies to an explicit rollback attempt for a
     * subtransaction, typically caused by an exception thrown by a data access
     * operation (where TransactionInterceptor will trigger a
     * <code>PlatformTransactionManager.rollback()</code> call according to a
     * rollback rule). If the flag is off, the caller can handle the exception
     * and decide on a rollback, independent of the rollback rules of the
     * subtransaction. This flag does, however, <i>not</i> apply to explicit
     * <code>setRollbackOnly</code> calls on a <code>TransactionStatus</code>,
     * which will always cause an eventual global rollback (as it might not
     * throw an exception after the rollback-only call).
     * <p>
     * The recommended solution for handling failure of a subtransaction is a
     * "nested transaction", where the global transaction can be rolled back to
     * a savepoint taken at the beginning of the subtransaction.
     * PROPAGATION_NESTED provides exactly those semantics; however, it will
     * only work when nested transaction support is available. This is the case
     * with DataSourceTransactionManager, but not with JtaTransactionManager.
     * 
     * @see #setNestedTransactionAllowed
     * @see org.springframework.transaction.jta.JtaTransactionManager
     */
    public final void setGlobalRollbackOnParticipationFailure(boolean globalRollbackOnParticipationFailure) {
        this.globalRollbackOnParticipationFailure = globalRollbackOnParticipationFailure;
    }

    /**
     * Return whether to globally mark an existing transaction as rollback-only
     * after a participating transaction failed.
     */
    public final boolean isGlobalRollbackOnParticipationFailure() {
        return this.globalRollbackOnParticipationFailure;
    }

    protected Transaction createTransaction() throws TransactionException {
        DefaultTransaction transaction = new DefaultTransaction(getDataSource(), this.objectContextFactory);
        transaction.setSavepointAllowed(true);
        return transaction;
    }

    protected boolean isExistingTransaction(Transaction transaction) throws TransactionException {
        Transaction txObject = transaction;
        return txObject.hasTransaction();
    }

    @Override
    public final TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        Transaction transaction = createTransaction();

        // Cache debug flag to avoid repeated checks.
        boolean debugEnabled = logger.isDebugEnabled();

        if (definition == null) {
            // Use defaults if no transaction definition given.
            definition = new TransactionDefinition();
        }

        if (isExistingTransaction(transaction)) {
            // Existing transaction found -> check propagation behavior to find
            // out how to behave.
            return handleExistingTransaction(definition, transaction, debugEnabled);
        }

        // Check definition settings for new transaction.
        if (definition.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
            throw new InvalidTimeoutException("Invalid transaction timeout", definition.getTimeout());
        }

        // No existing transaction found -> check propagation behavior to find
        // out how to proceed.
        if (definition.getPropagationBehavior() == PropagationBehavior.Mandatory) {
            throw new IllegalTransactionStateException(
                    "No existing transaction found for transaction marked with propagation 'mandatory'");
        } else if (definition.getPropagationBehavior() == PropagationBehavior.Required
                || definition.getPropagationBehavior() == PropagationBehavior.RequiresNew
                || definition.getPropagationBehavior() == PropagationBehavior.Nested) {
            SuspendedResourcesHolder suspendedResources = suspend(null);
            if (debugEnabled) {
                logger.debug("Creating new transaction with name [" + definition.getName() + "]: " + definition);
            }
            try {
                boolean newSynchronization = (getTransactionSynchronization() != Synchronization.Never);
                DefaultTransactionStatus status = newTransactionStatus(definition, transaction, true,
                        newSynchronization, debugEnabled, suspendedResources);
                transaction.begin(definition);
                prepareSynchronization(status, definition);
                return status;
            } catch (RuntimeException ex) {
                resume(null, suspendedResources);
                throw ex;
            } catch (Error err) {
                resume(null, suspendedResources);
                throw err;
            }
        } else {
            // Create "empty" transaction: no actual transaction, but
            // potentially synchronization.
            boolean newSynchronization = (getTransactionSynchronization() == Synchronization.Always);
            return prepareTransactionStatus(definition, null, true, newSynchronization, debugEnabled, null);
        }
    }

    /**
     * Create a TransactionStatus for an existing transaction.
     */
    private TransactionStatus handleExistingTransaction(TransactionDefinition definition, Transaction transaction,
            boolean debugEnabled) throws TransactionException {

        if (definition.getPropagationBehavior() == PropagationBehavior.Never) {
            throw new IllegalTransactionStateException(
                    "Existing transaction found for transaction marked with propagation 'never'");
        }

        if (definition.getPropagationBehavior() == PropagationBehavior.NotSupported) {
            if (debugEnabled) {
                logger.debug("Suspending current transaction");
            }
            Object suspendedResources = suspend(transaction);
            boolean newSynchronization = (getTransactionSynchronization() == Synchronization.Always);
            return prepareTransactionStatus(definition, null, false, newSynchronization, debugEnabled,
                    suspendedResources);
        }

        if (definition.getPropagationBehavior() == PropagationBehavior.RequiresNew) {
            if (debugEnabled) {
                logger.debug("Suspending current transaction, creating new transaction with name ["
                        + definition.getName() + "]");
            }
            SuspendedResourcesHolder suspendedResources = suspend(transaction);
            try {
                boolean newSynchronization = (getTransactionSynchronization() != Synchronization.Never);
                DefaultTransactionStatus status = newTransactionStatus(definition, transaction, true,
                        newSynchronization, debugEnabled, suspendedResources);
                transaction.begin(definition);
                prepareSynchronization(status, definition);
                return status;
            } catch (RuntimeException beginEx) {
                resumeAfterBeginException(transaction, suspendedResources, beginEx);
                throw beginEx;
            } catch (Error beginErr) {
                resumeAfterBeginException(transaction, suspendedResources, beginErr);
                throw beginErr;
            }
        }

        if (definition.getPropagationBehavior() == PropagationBehavior.Nested) {
            if (debugEnabled) {
                logger.debug("Creating nested transaction with name [" + definition.getName() + "]");
            }
            if (useSavepointForNestedTransaction()) {
                DefaultTransactionStatus status = prepareTransactionStatus(definition, transaction, false, false,
                        debugEnabled, null);
                status.createAndHoldSavepoint();
                return status;
            } else {
                boolean newSynchronization = (getTransactionSynchronization() != Synchronization.Never);
                DefaultTransactionStatus status = newTransactionStatus(definition, transaction, true,
                        newSynchronization, debugEnabled, null);
                transaction.begin(definition);
                prepareSynchronization(status, definition);
                return status;
            }
        }

        if (debugEnabled) {
            logger.debug("Participating in existing transaction");
        }
        boolean newSynchronization = (getTransactionSynchronization() != Synchronization.Never);
        return prepareTransactionStatus(definition, transaction, false, newSynchronization, debugEnabled, null);
    }

    /**
     * Create a new TransactionStatus for the given arguments, also initializing
     * transaction synchronization as appropriate.
     *
     * @see #newTransactionStatus
     * @see #prepareTransactionStatus
     */
    protected final DefaultTransactionStatus prepareTransactionStatus(TransactionDefinition definition,
            Transaction transaction, boolean newTransaction, boolean newSynchronization, boolean debug,
            Object suspendedResources) {
        DefaultTransactionStatus status = newTransactionStatus(definition, transaction, newTransaction,
                newSynchronization, debug, suspendedResources);
        prepareSynchronization(status, definition);
        return status;
    }

    /**
     * Create a rae TransactionStatus instance for the given arguments.
     */
    protected DefaultTransactionStatus newTransactionStatus(TransactionDefinition definition, Transaction transaction,
            boolean newTransaction, boolean newSynchronization, boolean debug, Object suspendedResources) {

        boolean actualNewSynchronization = newSynchronization
                && !transactionSynchronizationManager.isSynchronizationActive();
        return new DefaultTransactionStatus(transaction, newTransaction, actualNewSynchronization,
                definition.isReadOnly(), debug, suspendedResources);
    }

    /**
     * Initialize transaction synchronization as appropriate.
     */
    protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
        if (status.isNewSynchronization()) {
            // TODO [devacfr] simply on call
            transactionSynchronizationManager.setActualTransactionActive(status.hasTransaction());
            transactionSynchronizationManager
                    .setCurrentTransactionIsolationLevel((definition.getIsolationLevel() != IsolationLevel.Default) ? definition
                            .getIsolationLevel() : null);
            transactionSynchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
            transactionSynchronizationManager.setCurrentTransactionName(definition.getName());
            transactionSynchronizationManager.initSynchronization();
        }
    }

    protected final SuspendedResourcesHolder suspend(Transaction transaction) throws TransactionException {
        if (transactionSynchronizationManager.isSynchronizationActive()) {
            List<TransactionSynchronizer> suspendedSynchronizations = doSuspendSynchronization();
            try {
                Object suspendedResources = null;
                if (transaction != null) {
                    suspendedResources = transaction.suspend();
                }
                // TODO [devacfr] on call transactionSynchronizationManager
                String name = transactionSynchronizationManager.getCurrentTransactionName();
                transactionSynchronizationManager.setCurrentTransactionName(null);
                boolean readOnly = transactionSynchronizationManager.isCurrentTransactionReadOnly();
                transactionSynchronizationManager.setCurrentTransactionReadOnly(false);
                IsolationLevel isolationLevel = transactionSynchronizationManager.getCurrentTransactionIsolationLevel();
                transactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
                boolean wasActive = transactionSynchronizationManager.isActualTransactionActive();
                transactionSynchronizationManager.setActualTransactionActive(false);
                return new SuspendedResourcesHolder(suspendedResources, suspendedSynchronizations, name, readOnly,
                        isolationLevel, wasActive);
            } catch (RuntimeException ex) {
                // doSuspend failed - original transaction is still active...
                doResumeSynchronization(suspendedSynchronizations);
                throw ex;
            } catch (Error err) {
                // doSuspend failed - original transaction is still active...
                doResumeSynchronization(suspendedSynchronizations);
                throw err;
            }
        } else if (transaction != null) {
            // Transaction active but no synchronization active.
            Object suspendedResources = transaction.suspend();
            return new SuspendedResourcesHolder(suspendedResources);
        } else {
            // Neither transaction nor synchronization active.
            return null;
        }
    }

    protected final void resume(Transaction transaction, SuspendedResourcesHolder resourcesHolder)
            throws TransactionException {

        if (resourcesHolder != null) {
            Object suspendedResources = resourcesHolder.suspendedResources;
            if (suspendedResources != null) {
                transaction.resume(suspendedResources);
            }
            List<TransactionSynchronizer> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
            if (suspendedSynchronizations != null) {
                // TODO [] devacfr one call
                transactionSynchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
                transactionSynchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);
                transactionSynchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
                transactionSynchronizationManager.setCurrentTransactionName(resourcesHolder.name);
                doResumeSynchronization(suspendedSynchronizations);
            }
        }
    }

    private void resumeAfterBeginException(Transaction transaction, SuspendedResourcesHolder suspendedResources,
            Throwable beginEx) {

        String exMessage = "Inner transaction begin exception overridden by outer transaction resume exception";
        try {
            transaction.resume(suspendedResources);
        } catch (RuntimeException resumeEx) {
            logger.error(exMessage, beginEx);
            throw resumeEx;
        } catch (Error resumeErr) {
            logger.error(exMessage, beginEx);
            throw resumeErr;
        }
    }

    private List<TransactionSynchronizer> doSuspendSynchronization() {
        List<TransactionSynchronizer> suspendedSynchronizations = transactionSynchronizationManager
                .getSynchronizations();
        for (TransactionSynchronizer synchronization : suspendedSynchronizations) {
            synchronization.suspend();
        }
        transactionSynchronizationManager.clearSynchronization();
        return suspendedSynchronizations;
    }

    private void doResumeSynchronization(List<TransactionSynchronizer> suspendedSynchronizations) {
        transactionSynchronizationManager.initSynchronization();
        for (TransactionSynchronizer synchronization : suspendedSynchronizations) {
            synchronization.resume();
            transactionSynchronizationManager.registerSynchronization(synchronization);
        }
    }

    @Override
    public final void commit(TransactionStatus status) throws TransactionException {
        if (status.isCompleted()) {
            throw new IllegalTransactionStateException(
                    "Transaction is already completed - do not call commit or rollback more than once per transaction");
        }

        DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
        if (defStatus.isLocalRollbackOnly()) {
            if (defStatus.isDebug()) {
                logger.debug("Transactional code has requested rollback");
            }
            processRollback(defStatus);
            return;
        }
        if (!shouldCommitOnGlobalRollbackOnly() && defStatus.isGlobalRollbackOnly()) {
            if (defStatus.isDebug()) {
                logger.debug("Global transaction is marked as rollback-only but transactional code requested commit");
            }
            processRollback(defStatus);
            // Throw UnexpectedRollbackException only at outermost transaction
            // boundary
            if (status.isNewTransaction()) {
                throw new TransactionException("Transaction rolled back because it has been marked as rollback-only");
            }
            return;
        }

        processCommit(defStatus);
    }

    private void processCommit(DefaultTransactionStatus status) throws TransactionException {
        try {
            boolean beforeCompletionInvoked = false;
            try {
                triggerBeforeCommit(status);
                triggerBeforeCompletion(status);
                beforeCompletionInvoked = true;
                boolean globalRollbackOnly = false;
                if (status.isNewTransaction()) {
                    globalRollbackOnly = status.isGlobalRollbackOnly();
                }
                if (status.hasSavepoint()) {
                    if (status.isDebug()) {
                        logger.debug("Releasing transaction savepoint");
                    }
                    status.releaseHeldSavepoint();
                } else if (status.isNewTransaction()) {
                    if (status.isDebug()) {
                        logger.debug("Initiating transaction commit");
                    }
                    Transaction transaction = status.getTransaction();
                    transaction.commitChanges();
                    transaction.commit();
                }
                // Throw UnexpectedRollbackException if we have a global
                // rollback-only
                // marker but still didn't get a corresponding exception from
                // commit.
                if (globalRollbackOnly) {
                    throw new TransactionException(
                            "Transaction silently rolled back because it has been marked as rollback-only");
                }
            } catch (TransactionException ex) {
                triggerAfterCompletion(status, TransactionSynchronizer.Status.UNKNOWN);
                throw ex;
            } catch (RuntimeException ex) {
                if (!beforeCompletionInvoked) {
                    triggerBeforeCompletion(status);
                }
                doRollbackOnCommitException(status, ex);
                throw ex;
            } catch (Error err) {
                if (!beforeCompletionInvoked) {
                    triggerBeforeCompletion(status);
                }
                doRollbackOnCommitException(status, err);
                throw err;
            }

            // Trigger afterCommit callbacks, with an exception thrown there
            // propagated to callers but the transaction still considered as
            // committed.
            try {
                triggerAfterCommit(status);
            } finally {
                triggerAfterCompletion(status, TransactionSynchronizer.Status.COMMITTED);
            }

        } finally {
            cleanupAfterCompletion(status);
        }
    }

    @Override
    public final void rollback(TransactionStatus status) throws TransactionException {
        if (status.isCompleted()) {
            throw new IllegalTransactionStateException(
                    "Transaction is already completed - do not call commit or rollback more than once per transaction");
        }

        DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
        processRollback(defStatus);
    }

    private void processRollback(DefaultTransactionStatus status) {
        try {
            try {
                triggerBeforeCompletion(status);
                if (status.hasSavepoint()) {
                    if (status.isDebug()) {
                        logger.debug("Rolling back transaction to savepoint");
                    }
                    status.rollbackToHeldSavepoint();
                } else if (status.isNewTransaction()) {
                    if (status.isDebug()) {
                        logger.debug("Initiating transaction rollback");
                    }
                    Transaction transaction = status.getTransaction();
                    transaction.rollback();
                } else if (status.hasTransaction()) {
                    if (status.isLocalRollbackOnly() || isGlobalRollbackOnParticipationFailure()) {
                        if (status.isDebug()) {
                            logger.debug("Participating transaction failed - marking existing transaction as rollback-only");
                        }
                        status.getTransaction().setRollbackOnly();
                    } else {
                        if (status.isDebug()) {
                            logger.debug("Participating transaction failed - letting transaction originator decide on rollback");
                        }
                    }
                } else {
                    logger.debug("Should roll back transaction but cannot - no transaction available");
                }
            } catch (RuntimeException ex) {
                triggerAfterCompletion(status, TransactionSynchronizer.Status.UNKNOWN);
                throw ex;
            } catch (Error err) {
                triggerAfterCompletion(status, TransactionSynchronizer.Status.UNKNOWN);
                throw err;
            }
            triggerAfterCompletion(status, TransactionSynchronizer.Status.ROLLED_BACK);
        } finally {
            cleanupAfterCompletion(status);
        }
    }

    private void doRollbackOnCommitException(DefaultTransactionStatus status, Throwable ex) throws TransactionException {
        try {
            if (status.isNewTransaction()) {
                if (status.isDebug()) {
                    logger.debug("Initiating transaction rollback after commit exception", ex);
                }
                status.getTransaction().rollback();
            } else if (status.hasTransaction() && isGlobalRollbackOnParticipationFailure()) {
                if (status.isDebug()) {
                    logger.debug("Marking existing transaction as rollback-only after commit exception", ex);
                }
                status.getTransaction().setRollbackOnly();
            }
        } catch (RuntimeException rbex) {
            logger.error("Commit exception overridden by rollback exception", ex);
            triggerAfterCompletion(status, TransactionSynchronizer.Status.UNKNOWN);
            throw rbex;
        } catch (Error rberr) {
            logger.error("Commit exception overridden by rollback exception", ex);
            triggerAfterCompletion(status, TransactionSynchronizer.Status.UNKNOWN);
            throw rberr;
        }
        triggerAfterCompletion(status, TransactionSynchronizer.Status.ROLLED_BACK);
    }

    protected final void triggerBeforeCommit(DefaultTransactionStatus status) {
        if (status.isNewSynchronization()) {
            if (status.isDebug()) {
                logger.trace("Triggering beforeCommit synchronization");
            }
            TransactionSynchronizationSupport.triggerBeforeCommit(status.isReadOnly());
        }
    }

    protected final void triggerBeforeCompletion(DefaultTransactionStatus status) {
        if (status.isNewSynchronization()) {
            if (status.isDebug()) {
                logger.trace("Triggering beforeCompletion synchronization");
            }
            TransactionSynchronizationSupport.triggerBeforeCompletion();
        }
    }

    private void triggerAfterCommit(DefaultTransactionStatus status) {
        if (status.isNewSynchronization()) {
            if (status.isDebug()) {
                logger.trace("Triggering afterCommit synchronization");
            }
            TransactionSynchronizationSupport.triggerAfterCommit();
        }
    }

    private void triggerAfterCompletion(DefaultTransactionStatus status, TransactionSynchronizer.Status completionStatus) {
        if (status.isNewSynchronization()) {
            List<TransactionSynchronizer> synchronizations = transactionSynchronizationManager.getSynchronizations();
            if (!status.hasTransaction() || status.isNewTransaction()) {
                if (status.isDebug()) {
                    logger.trace("Triggering afterCompletion synchronization");
                }
                // No transaction or new transaction for the current scope ->
                // invoke the afterCompletion callbacks immediately
                TransactionSynchronizationSupport.invokeAfterCompletion(synchronizations, completionStatus);
            } else if (!synchronizations.isEmpty()) {
                // Existing transaction that we participate in, controlled
                // outside
                // of the scope of this Spring transaction manager -> try to
                // register
                // an afterCompletion callback with the existing (JTA)
                // transaction.
                registerAfterCompletionWithExistingTransaction(status.getTransaction(), synchronizations);
            }
        }
    }

    private void cleanupAfterCompletion(DefaultTransactionStatus status) {
        status.setCompleted();
        if (status.isNewSynchronization()) {
            transactionSynchronizationManager.clear();
        }
        if (status.isNewTransaction()) {
            status.getTransaction().close();
        }
        if (status.getSuspendedResources() != null) {
            if (status.isDebug()) {
                logger.debug("Resuming suspended transaction after completion of inner transaction");
            }
            resume(status.getTransaction(), (SuspendedResourcesHolder) status.getSuspendedResources());
        }
    }

    protected boolean useSavepointForNestedTransaction() {
        return true;
    }

    protected boolean shouldCommitOnGlobalRollbackOnly() {
        return false;
    }

    protected void registerAfterCompletionWithExistingTransaction(Object transaction,
            List<TransactionSynchronizer> synchronizations) throws TransactionException {

        logger.debug("Cannot register Spring after-completion synchronization with existing transaction - "
                + "processing Spring after-completion callbacks immediately, with outcome status 'unknown'");
        TransactionSynchronizationSupport.invokeAfterCompletion(synchronizations,
                TransactionSynchronizer.Status.UNKNOWN);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Rely on default serialization; just initialize state after
        // deserialization.
        ois.defaultReadObject();
    }

    /**
     * Holder for suspended resources. Used internally by <code>suspend</code>
     * and <code>resume</code>.
     */
    protected static class SuspendedResourcesHolder {

        private final Object suspendedResources;

        private List<TransactionSynchronizer> suspendedSynchronizations;

        private String name;

        private boolean readOnly;

        private IsolationLevel isolationLevel;

        private boolean wasActive;

        private SuspendedResourcesHolder(Object suspendedResources) {
            this.suspendedResources = suspendedResources;
        }

        private SuspendedResourcesHolder(Object suspendedResources,
                List<TransactionSynchronizer> suspendedSynchronizations, String name, boolean readOnly,
                IsolationLevel isolationLevel, boolean wasActive) {
            this.suspendedResources = suspendedResources;
            this.suspendedSynchronizations = suspendedSynchronizations;
            this.name = name;
            this.readOnly = readOnly;
            this.isolationLevel = isolationLevel;
            this.wasActive = wasActive;
        }
    }

}