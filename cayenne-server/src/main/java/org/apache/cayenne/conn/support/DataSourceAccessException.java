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
package org.apache.cayenne.conn.support;

/**
 * This exception aims handle the kind of error encountered without knowing the
 * details of the particular data access in use (e.g. JDBC).
 *
 * <p>
 * As this class is a runtime exception, there is no need for user code to catch
 * it or subclasses if any error is to be considered fatal (the usual case).
 * 
 * @author devacfr<christophefriederich@mac.com>
 * 
 * @since 3.2
 */
public class DataSourceAccessException extends NestedRuntimeException {

    /**
     * default serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for DataSourceAccessException.
     * 
     * @param msg
     *            the detail message
     */
    public DataSourceAccessException(String msg) {
        super(msg);
    }

    /**
     * Constructor for DataSourceAccessException.
     * 
     * @param msg
     *            the detail message
     * @param cause
     *            the root cause (usually from using a underlying data access
     *            API such as JDBC)
     */
    public DataSourceAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }

}