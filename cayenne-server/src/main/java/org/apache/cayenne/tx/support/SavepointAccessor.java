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

import org.apache.cayenne.tx.TransactionStatus;
import org.apache.cayenne.tx.exception.SavepointNotSupportedException;
import org.apache.cayenne.tx.exception.TransactionException;

/**
 * This Interface specifies an API (based on JDBC 3.0's Savepoint) to
 * programmatically manage transaction savepoints and expose by
 * {@link TransactionStatus}.
 * <p>
 * Note that savepoints can only work within an active transaction. Just use
 * this programmatic savepoint handling for advanced needs; else, a
 * subtransaction with nested propagation is preferable.
 * <p>
 * Note that savepoints are only supported for drivers which support JDBC 3.0 or
 * higher.
 * 
 * @author devacfr<christophefriederich@mac.com>
 * @since 3.2
 * @see Connection
 */
public interface SavepointAccessor {

    /**
     * Create a new savepoint. You can roll back to a specific savepoint via
     * <code>rollbackToSavepoint</code>, and explicitly release a savepoint that
     * you don't need anymore via <code>releaseSavepoint</code>.
     * <p>
     * Note the transaction manager will automatically release savepoints at
     * transaction completion.
     * 
     * @return a savepoint object, to be passed into rollbackToSavepoint or
     *         releaseSavepoint
     * @throws SavepointNotSupportedException
     *             if the underlying transaction does not support savepoints
     * @throws TransactionException
     *             if the savepoint could not be created, for example because
     *             the transaction is not in an appropriate state
     */
    Object createSavepoint() throws TransactionException;

    /**
     * Roll back to the given savepoint. The savepoint will be automatically
     * released afterwards.
     * 
     * @param savepoint
     *            the savepoint to roll back to
     * @throws SavepointNotSupportedException
     *             if the underlying transaction does not support savepoints
     * @throws TransactionException
     *             if the rollback failed
     */
    void rollbackToSavepoint(Object savepoint) throws TransactionException;

    /**
     * Explicitly release the given savepoint.
     * <p>
     * Note the transaction managers will automatically release savepoints at
     * transaction completion.
     * 
     * @param savepoint
     *            the savepoint to release
     * @throws SavepointNotSupportedException
     *             if the underlying
     * @throws TransactionException
     *             if the release failed or the underlying transaction does not
     *             support savepoints
     */
    void releaseSavepoint(Object savepoint) throws TransactionException;

}
