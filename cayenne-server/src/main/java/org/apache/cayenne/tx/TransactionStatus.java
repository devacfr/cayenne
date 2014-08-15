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

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.tx.support.SavepointAccessor;

/**
 * This class represents the status of a transaction and allows managing the
 * transaction manually (instead of throwing an exception that causes an
 * implicit rollback).
 * <p>
 * Derives from the SavepointManager interface to provide access to savepoint
 * management facilities. Note that savepoint management is only available if
 * supported by the underlying transaction manager.
 * <p>
 *
 * @author devacfr<christophefriederich@mac.com>
 * @since 3.2
 * 
 */
public interface TransactionStatus extends SavepointAccessor {

    /**
     * Gets indicating whether the current transaction is new (else
     * participating in an existing transaction, or potentially not running in
     * an actual transaction in the first place).
     */
    boolean isNewTransaction();

    /**
     * Sets the transaction rollback-only. This indicates the transaction
     * manager that the only possible outcome of the transaction may be a
     * rollback, as alternative to throwing an exception which would in turn
     * trigger a rollback.
     */
    void setRollbackOnly();

    /**
     * Gets indicating whether the transaction has been marked as rollback-only.
     */
    boolean isRollbackOnly();

    /**
     * Flushes all changes to objects in this context to the parent
     * {@link DataChannel}, cascading flush operation all the way through the
     * stack, ultimately saving data in the database.
     */
    void flush();

    /**
     * Gets indicating whether this transaction is completed, that is, whether
     * it has already been committed or rolled back.
     */
    boolean isCompleted();

    /**
     * Gets indicating whether this transaction internally carries a savepoint,
     * that is, has been created as nested transaction based on a savepoint.
     */
    boolean hasSavepoint();

}