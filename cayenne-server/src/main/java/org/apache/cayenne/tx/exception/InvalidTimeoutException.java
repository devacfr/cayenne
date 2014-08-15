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
 * Exception thrown when an invalid timeout is specified, that is, the specified
 * timeout valid is out of range or the transaction manager implementation
 * doesn't support timeouts.
 *
 * @author devacfr<christophefriederich@mac.com>
 * @since 3.2
 */
public class InvalidTimeoutException extends TransactionUsageException {

    /**
     * default serial version
     */
    private static final long serialVersionUID = 1L;

    private int timeout;

    /**
     * Constructor for InvalidTimeoutException.
     * 
     * @param msg
     *            the detail message
     * @param timeout
     *            the invalid timeout value
     */
    public InvalidTimeoutException(String msg, int timeout) {
        super(msg);
        this.timeout = timeout;
    }

    /**
     * Gets the invalid timeout value.
     */
    public int getTimeout() {
        return timeout;
    }

}