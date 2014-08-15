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

import org.apache.cayenne.tx.exception.TransactionException;
import org.apache.cayenne.tx.support.SavepointAccessor;

/**
 * @author devacfr<christophefriederich@mac.com>
 *
 */
public interface Transaction extends SavepointAccessor {

    /**
	 *
	 */
    void commitChanges();

    void begin(TransactionDefinition definition) throws TransactionException;

    /**
     * @return
     */
    boolean hasTransaction();

    /**
     * @return
     */
    Object suspend();

    /**
     *
     * @param suspendedResources
     * @throws TransactionException
     */
    void resume(Object suspendedResources) throws TransactionException;

    void commit() throws TransactionException;

    /**
	 *
	 */
    void setRollbackOnly();

    /**
     *
     * @return
     */
    boolean isRollbackOnly();

    /**
	 *
	 */
    public void rollback();

    /**
	 *
	 */
    public void close();

}
