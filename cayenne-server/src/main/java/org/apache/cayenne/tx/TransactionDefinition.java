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

import java.io.Serializable;
import java.sql.Connection;

public class TransactionDefinition implements Serializable {

    /**
     * default serial version
     */
    private static final long serialVersionUID = 1L;

    public enum PropagationBehavior {

        Required(0),
        Supports(1),
        Mandatory(2),
        RequiresNew(3),
        NotSupported(4),
        Never(5),
        Nested(6);

        private final int intValue;

        private PropagationBehavior(final int intValue) {
            this.intValue = intValue;
        }

        public int intValue() {
            return intValue;
        }
    }

    public enum IsolationLevel {

        Default(-1),
        ReadUncommitted(Connection.TRANSACTION_READ_UNCOMMITTED),
        ReadCommitted(Connection.TRANSACTION_READ_COMMITTED),
        RepeatableRead(Connection.TRANSACTION_REPEATABLE_READ),
        Serializable(Connection.TRANSACTION_SERIALIZABLE);

        private final int intValue;

        private IsolationLevel(final int intValue) {
            this.intValue = intValue;
        }

        public int intValue() {
            return intValue;
        }
    }

    public static final int TIMEOUT_DEFAULT = -1;

    private PropagationBehavior propagationBehavior = PropagationBehavior.Required;

    private IsolationLevel isolationLevel = IsolationLevel.Default;

    private int timeout = TIMEOUT_DEFAULT;

    private boolean readOnly = false;

    private String name;

    public TransactionDefinition() {
    }

    public TransactionDefinition(TransactionDefinition other) {
        this.propagationBehavior = other.getPropagationBehavior();
        this.isolationLevel = other.getIsolationLevel();
        this.timeout = other.getTimeout();
        this.readOnly = other.isReadOnly();
        this.name = other.getName();
    }

    public final PropagationBehavior getPropagationBehavior() {
        return this.propagationBehavior;
    }

    public final IsolationLevel getIsolationLevel() {
        return this.isolationLevel;
    }

    public final int getTimeout() {
        return this.timeout;
    }

    public final boolean isReadOnly() {
        return this.readOnly;
    }

    public final String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof TransactionDefinition && toString().equals(other.toString()));
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return getDefinitionDescription().toString();
    }

    /**
     * Return an identifying description for this transaction definition.
     * <p>
     * Available to subclasses, for inclusion in their <code>toString()</code>
     * result.
     */
    protected final StringBuilder getDefinitionDescription() {
        StringBuilder result = new StringBuilder();
        result.append(this.propagationBehavior.name());
        result.append(',');
        result.append(this.isolationLevel.name());
        if (this.timeout != TIMEOUT_DEFAULT) {
            result.append(',');
            result.append(this.timeout);
        }
        if (this.readOnly) {
            result.append(",readOnly");
        }
        return result;
    }

    public static final TransactionDefinition REQUIRED = TransactionDefinition.builder().propagationRequired().build();

    public static final TransactionDefinition DEFAULT = TransactionDefinition.builder().build();

    public static final TransactionDefinition SUPPORTS = TransactionDefinition.builder().propagationSupports().build();

    public static Builder builder() {
        return new TransactionDefinition().new Builder();
    }

    public class Builder {

        private Builder() {
        }

        public Builder propagationRequired() {
            TransactionDefinition.this.propagationBehavior = PropagationBehavior.Required;
            return this;
        }

        public Builder propagationMandatory() {
            TransactionDefinition.this.propagationBehavior = PropagationBehavior.Mandatory;
            return this;
        }

        public Builder propagationNested() {
            TransactionDefinition.this.propagationBehavior = PropagationBehavior.Nested;
            return this;
        }

        public Builder propagationNever() {
            TransactionDefinition.this.propagationBehavior = PropagationBehavior.Never;
            return this;
        }

        public Builder propagationNotSupported() {
            TransactionDefinition.this.propagationBehavior = PropagationBehavior.NotSupported;
            return this;
        }

        public Builder propagationRequiresNew() {
            TransactionDefinition.this.propagationBehavior = PropagationBehavior.RequiresNew;
            return this;
        }

        public Builder propagationSupports() {
            TransactionDefinition.this.propagationBehavior = PropagationBehavior.Supports;
            return this;
        }

        public Builder isolationReadCommitted() {
            TransactionDefinition.this.isolationLevel = IsolationLevel.ReadCommitted;
            return this;
        }

        public Builder isolationReadUncommitted() {
            TransactionDefinition.this.isolationLevel = IsolationLevel.ReadUncommitted;
            return this;
        }

        public Builder isolationRepeatableRead() {
            TransactionDefinition.this.isolationLevel = IsolationLevel.RepeatableRead;
            return this;
        }

        public Builder isolationSerializable() {
            TransactionDefinition.this.isolationLevel = IsolationLevel.Serializable;
            return this;
        }

        public Builder readOnly() {
            TransactionDefinition.this.readOnly = true;
            return this;
        }

        public Builder name(String name) {
            TransactionDefinition.this.name = name;
            return this;
        }

        public Builder timeout(int timeout) {
            if (timeout < TIMEOUT_DEFAULT) {
                throw new IllegalArgumentException("Timeout must be a positive integer or TIMEOUT_DEFAULT");
            }
            TransactionDefinition.this.timeout = timeout;
            return this;
        }

        public TransactionDefinition build() {
            return TransactionDefinition.this;
        }
    }

}