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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.tx.TransactionDefinition.IsolationLevel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author devacfr<christophefriederich@mac.com>
 *
 */
public abstract class TransactionSynchronizationSupport {

    private static final Log logger = LogFactory.getLog(TransactionSynchronizationSupport.class);

    private static AtomicReference<TransactionSynchronizationManager> instance = new AtomicReference<TransactionSynchronizationManager>();

    synchronized static boolean hasManager() {
        return instance.get() != null;
    }

    synchronized static void register(TransactionSynchronizationManager manager) {
        if (manager == null) {
            return;
        }
        if (instance.get() != null) {
            logger.warn("TransactionSynchronizationManager is exposed as final singleton and can not be change");
            return;
        }
        instance.set(manager);
    }

    private static TransactionSynchronizationManager getManager() {
        final TransactionSynchronizationManager manager = instance.get(); 
        if (manager == null) {
            throw new CayenneRuntimeException("A TransactionSynchronizationManager must be declare before use");
        }
        return manager;
    }

    public static Map<Object, Object> getResourceMap() {
        return getManager().getResourceMap();
    }

    public static boolean hasResource(Object key) {
        return getManager().hasResource(key);
    }

    public static Object getResource(Object key) {
        return getManager().getResource(key);
    }

    public static void bindResource(Object key, Object value) throws IllegalStateException {
        getManager().bindResource(key, value);
    }

    public static Object unbindResource(Object key) throws IllegalStateException {
        return getManager().unbindResource(key);
    }

    public static Object unbindResourceIfPossible(Object key) {
        return getManager().unbindResourceIfPossible(key);
    }

    public static boolean isSynchronizationActive() {
        return getManager().isSynchronizationActive();
    }

    public static void initSynchronization() throws IllegalStateException {
        getManager().initSynchronization();
    }

    public static void registerSynchronization(TransactionSynchronizer synchronization) throws IllegalStateException {
        getManager().registerSynchronization(synchronization);
    }

    public static List<TransactionSynchronizer> getSynchronizations() throws IllegalStateException {
        return getManager().getSynchronizations();
    }

    public static void clearSynchronization() throws IllegalStateException {
        getManager().clearSynchronization();
    }

    public static void setCurrentTransactionName(String name) {
        getManager().setCurrentTransactionName(name);
    }

    public static String getCurrentTransactionName() {
        return getManager().getCurrentTransactionName();
    }

    public static void setCurrentTransactionReadOnly(boolean readOnly) {
        getManager().setCurrentTransactionReadOnly(readOnly);
    }

    public static boolean isCurrentTransactionReadOnly() {
        return getManager().isCurrentTransactionReadOnly();
    }

    public static void setCurrentTransactionIsolationLevel(IsolationLevel isolationLevel) {
        getManager().setCurrentTransactionIsolationLevel(isolationLevel);
    }

    public static IsolationLevel getCurrentTransactionIsolationLevel() {
        return getManager().getCurrentTransactionIsolationLevel();
    }

    public static void setActualTransactionActive(boolean active) {
        getManager().setActualTransactionActive(active);
    }

    public static boolean isActualTransactionActive() {
        return getManager().isActualTransactionActive();
    }

    public static void clear() {
        getManager().clear();
    }

    public static void triggerBeforeCommit(boolean readOnly) {
        for (TransactionSynchronizer synchronization : getSynchronizations()) {
            synchronization.beforeCommit(readOnly);
        }
    }

    public static void triggerBeforeCompletion() {
        for (TransactionSynchronizer synchronization : getSynchronizations()) {
            try {
                synchronization.beforeCompletion();
            } catch (Throwable tsex) {
                logger.error("beforeCompletion threw exception", tsex);
            }
        }
    }

    public static void triggerAfterCommit() {
        invokeAfterCommit(getSynchronizations());
    }

    public static void invokeAfterCommit(List<TransactionSynchronizer> synchronizations) {
        if (synchronizations != null) {
            for (TransactionSynchronizer synchronization : synchronizations) {
                synchronization.afterCommit();
            }
        }
    }

    public static void triggerAfterCompletion(TransactionSynchronizer.Status completionStatus) {
        List<TransactionSynchronizer> synchronizations = getSynchronizations();
        invokeAfterCompletion(synchronizations, completionStatus);
    }

    public static void invokeAfterCompletion(List<TransactionSynchronizer> synchronizations, TransactionSynchronizer.Status completionStatus) {
        if (synchronizations != null) {
            for (TransactionSynchronizer synchronization : synchronizations) {
                try {
                    synchronization.afterCompletion(completionStatus);
                } catch (Throwable tsex) {
                    logger.error("TransactionSynchronization.afterCompletion threw exception", tsex);
                }
            }
        }
    }

}