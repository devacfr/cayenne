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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.conn.support.ConnectionHolder;
import org.apache.cayenne.tx.TransactionDefinition.IsolationLevel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultTransactionSynchronizationManager implements TransactionSynchronizationManager {

    private static final Log logger = LogFactory.getLog(DefaultTransactionSynchronizationManager.class);

    private final ThreadLocal<Map<Object, Object>> resources = new ThreadLocal<Map<Object, Object>>();

    private final ThreadLocal<Set<TransactionSynchronizer>> synchronizations = new ThreadLocal<Set<TransactionSynchronizer>>();

    private final ThreadLocal<String> currentTransactionName = new ThreadLocal<String>();

    private final ThreadLocal<Boolean> currentTransactionReadOnly = new ThreadLocal<Boolean>();

    private final ThreadLocal<IsolationLevel> currentTransactionIsolationLevel = new ThreadLocal<IsolationLevel>();

    private final ThreadLocal<Boolean> actualTransactionActive = new ThreadLocal<Boolean>();

    /**
	 * 
	 */
    public DefaultTransactionSynchronizationManager() {
        if (!TransactionSynchronizationSupport.hasManager()) {
            TransactionSynchronizationSupport.register(this);
        }
    }

    public Map<Object, Object> getResourceMap() {
        Map<Object, Object> map = resources.get();
        return (map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap());
    }

    public boolean hasResource(Object key) {
        Object actualKey = key;
        Object value = doGetResource(actualKey);
        return (value != null);
    }

    public Object getResource(Object key) {
        Object actualKey = key;
        Object value = doGetResource(actualKey);
        if (value != null && logger.isTraceEnabled()) {
            logger.trace("Retrieved value [" + value + "] for key [" + actualKey + "] bound to thread ["
                    + Thread.currentThread().getName() + "]");
        }
        return value;
    }

    private Object doGetResource(Object actualKey) {
        Map<Object, Object> map = resources.get();
        if (map == null) {
            return null;
        }
        Object value = map.get(actualKey);
        // Transparently remove ConnectionHolder that was marked as detached...
        if (value instanceof ConnectionHolder) {
            map.remove(actualKey);
            // Remove entire ThreadLocal if empty...
            if (map.isEmpty()) {
                resources.remove();
            }
            value = null;
        }
        return value;
    }

    public void bindResource(Object key, Object value) throws IllegalStateException {
        Object actualKey = key;
        Map<Object, Object> map = resources.get();
        // set ThreadLocal Map if none found
        if (map == null) {
            map = new HashMap<Object, Object>();
            resources.set(map);
        }
        map.put(actualKey, value);
        if (logger.isTraceEnabled()) {
            logger.trace("Bound value [" + value + "] for key [" + actualKey + "] to thread ["
                    + Thread.currentThread().getName() + "]");
        }
    }

    public Object unbindResource(Object key) throws IllegalStateException {
        Object actualKey = key;
        Object value = doUnbindResource(actualKey);
        if (value == null) {
            throw new IllegalStateException("No value for key [" + actualKey + "] bound to thread ["
                    + Thread.currentThread().getName() + "]");
        }
        return value;
    }

    public Object unbindResourceIfPossible(Object key) {
        Object actualKey = key;
        return doUnbindResource(actualKey);
    }

    private Object doUnbindResource(Object actualKey) {
        Map<Object, Object> map = resources.get();
        if (map == null) {
            return null;
        }
        Object value = map.remove(actualKey);
        // Remove entire ThreadLocal if empty...
        if (map.isEmpty()) {
            resources.remove();
        }
        return value;
    }

    public boolean isSynchronizationActive() {
        return (synchronizations.get() != null);
    }

    public void initSynchronization() throws IllegalStateException {
        if (isSynchronizationActive()) {
            throw new IllegalStateException("Cannot activate transaction synchronization - already active");
        }
        logger.trace("Initializing transaction synchronization");
        synchronizations.set(new LinkedHashSet<TransactionSynchronizer>());
    }

    public void registerSynchronization(TransactionSynchronizer synchronization) throws IllegalStateException {
        if (!isSynchronizationActive()) {
            throw new IllegalStateException("Transaction synchronization is not active");
        }
        synchronizations.get().add(synchronization);
    }

    public List<TransactionSynchronizer> getSynchronizations() throws IllegalStateException {
        Set<TransactionSynchronizer> synchs = synchronizations.get();
        if (synchs == null) {
            throw new IllegalStateException("Transaction synchronization is not active");
        }
        if (synchs.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(new ArrayList<TransactionSynchronizer>(synchs));
        }
    }

    public void clearSynchronization() throws IllegalStateException {
        if (!isSynchronizationActive()) {
            throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
        }
        logger.trace("Clearing transaction synchronization");
        synchronizations.remove();
    }

    public void setCurrentTransactionName(String name) {
        currentTransactionName.set(name);
    }

    public String getCurrentTransactionName() {
        return currentTransactionName.get();
    }

    public void setCurrentTransactionReadOnly(boolean readOnly) {
        currentTransactionReadOnly.set(readOnly ? Boolean.TRUE : null);
    }

    public boolean isCurrentTransactionReadOnly() {
        return (currentTransactionReadOnly.get() != null);
    }

    public void setCurrentTransactionIsolationLevel(IsolationLevel isolationLevel) {
        currentTransactionIsolationLevel.set(isolationLevel);
    }

    public IsolationLevel getCurrentTransactionIsolationLevel() {
        return currentTransactionIsolationLevel.get();
    }

    public void setActualTransactionActive(boolean active) {
        actualTransactionActive.set(active ? Boolean.TRUE : null);
    }

    public boolean isActualTransactionActive() {
        return (actualTransactionActive.get() != null);
    }

    public void clear() {
        clearSynchronization();
        setCurrentTransactionName(null);
        setCurrentTransactionReadOnly(false);
        setCurrentTransactionIsolationLevel(null);
        setActualTransactionActive(false);
    }

}