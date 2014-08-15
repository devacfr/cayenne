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
package org.apache.cayenne.tx.exception;

import org.apache.cayenne.conn.support.NestedRuntimeException;

public class TransactionException extends NestedRuntimeException {

    /**
     * default serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for TransactionException.
     * 
     * @param msg
     *            the detail message
     */
    public TransactionException(String msg) {
        super(msg);
    }

    /**
     * Constructor for TransactionException.
     * 
     * @param msg
     *            the detail message
     * @param cause
     *            the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public TransactionException(String msg, Throwable cause) {
        super(msg, cause);
    }

}