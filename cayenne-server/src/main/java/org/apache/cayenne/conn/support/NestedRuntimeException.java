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

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * A RuntimeException which is nested to preserve stack traces. 
 * @author devacfr<christophefriederich@mac.com>
 *
 */
public abstract class NestedRuntimeException extends RuntimeException {

    /**
     * Default serial version
     */
    private static final long serialVersionUID = 1L;

    /** Root cause of this nested exception */
    private Throwable cause;

    /**
     * Construct a <code>NestedRuntimeException</code> with the specified detail
     * message.
     * 
     * @param msg
     *            the detail message
     */
    public NestedRuntimeException(String msg) {
        super(msg);
    }

    /**
     * Construct a <code>NestedRuntimeException</code> with the specified detail
     * message and nested exception.
     * 
     * @param msg
     *            the detail message
     * @param ex
     *            the nested exception
     */
    public NestedRuntimeException(String msg, Throwable ex) {
        super(msg);
        this.cause = ex;
    }

    /**
     * Return the nested cause, or <code>null</code> if none.
     */
    @Override
    public Throwable getCause() {
        return (this.cause == this ? null : this.cause);
    }

    /**
     * Return the detail message, including the message from the nested
     * exception if there is one.
     */
    @Override
    public String getMessage() {
        if (getCause() == null) {
            return super.getMessage();
        } else {
            return super.getMessage() + "; nested exception is " + getCause().getClass().getName() + ": "
                    + getCause().getMessage();
        }
    }

    /**
     * Print the composite message and the embedded stack trace to the specified
     * stream.
     * 
     * @param ps
     *            the print stream
     */
    public void printStackTrace(PrintStream ps) {
        if (getCause() == null) {
            super.printStackTrace(ps);
        } else {
            ps.println(this);
            getCause().printStackTrace(ps);
        }
    }

    /**
     * Print the composite message and the embedded stack trace to the specified
     * writer.
     * 
     * @param pw
     *            the print writer
     */
    public void printStackTrace(PrintWriter pw) {
        if (getCause() == null) {
            super.printStackTrace(pw);
        } else {
            pw.println(this);
            getCause().printStackTrace(pw);
        }
    }

    /**
     * Check whether this exception contains an exception of the given class:
     * either it is of the given class itself or it contains a nested cause of
     * the given class.
     * <p>
     * Currently just traverses NestedRuntimeException causes. Will use the JDK
     * 1.4 exception cause mechanism once Spring requires JDK 1.4.
     * 
     * @param exClass
     *            the exception class to look for
     */
    public boolean contains(Class<?> exClass) {
        if (exClass == null) {
            return false;
        }
        Throwable ex = this;
        while (ex != null) {
            if (exClass.isInstance(ex)) {
                return true;
            }
            if (ex instanceof NestedRuntimeException) {
                // Cast is necessary on JDK 1.3, where Throwable does not
                // provide a "getCause" method itself.
                ex = ((NestedRuntimeException) ex).getCause();
            } else {
                ex = null;
            }
        }
        return false;
    }

}
