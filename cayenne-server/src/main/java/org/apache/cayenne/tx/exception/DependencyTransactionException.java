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

/**
 * Exception thrown when a transaction system error is encountered, like on
 * commit or rollback.
 *
 * @author devacfr<christophefriederich@mac.com>
 * @since 3.2
 */
public class DependencyTransactionException extends TransactionException {

    /**
     * default serial version
     */
    private static final long serialVersionUID = 1L;
    private Throwable originalException;

    /**
     * 
     * @param msg
     *            the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     */
    public DependencyTransactionException(String msg) {
        super(msg);
    }

    /**
     * 
     * @param msg
     *            the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     * @param cause
     *            the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public DependencyTransactionException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Set cause exception that was thrown before this transaction exception,
     * preserving the original exception despite the overriding
     * TransactionSystemException.
     * 
     * @param ex
     *            the application exception
     * @throws IllegalStateException
     *             if this TransactionSystemException already holds an
     *             application exception
     */
    public void initDependencyException(Throwable ex) {
        if (this.originalException != null) {
            throw new IllegalStateException("Already holding an application exception: " + this.originalException);
        }
        this.originalException = ex;
    }

    /**
     * Return the application exception that was thrown before this transaction
     * exception, if any.
     * 
     * @return the application exception, or <code>null</code> if none set
     */
    public final Throwable getDependingException() {
        return this.originalException;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Class<?> exType) {
        return super.contains(exType) || (exType != null && exType.isInstance(this.originalException));
    }

}