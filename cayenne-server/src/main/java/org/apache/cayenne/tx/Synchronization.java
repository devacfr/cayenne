/**
 * Copyright 2014 devacfr<christophefriederich@mac.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cayenne.tx;

import java.util.List;

import org.apache.cayenne.tx.support.TransactionSynchronizationSupport;
import org.apache.cayenne.tx.support.TransactionSynchronizer;

/**
 * @author devacfr<christophefriederich@mac.com>
 *
 */
public enum Synchronization {

    /**
     * All transactions are synchronized between them.
     */
    Always,
    /**
     * Only on actual active transaction.
     */
    OnActualTransaction,
    /**
     * Never active transaction synchronization, not even for actual
     * transactions.
     */
    Never;

    public static boolean isSynchronizationActive() {
        return TransactionSynchronizationSupport.isSynchronizationActive();
    }

    public static void initSynchronization() throws IllegalStateException {
        TransactionSynchronizationSupport.initSynchronization();
    }

    public static void registerSynchronization(TransactionSynchronizer synchronization) throws IllegalStateException {
        TransactionSynchronizationSupport.registerSynchronization(synchronization);
    }

    public static List<TransactionSynchronizer> getSynchronizations() throws IllegalStateException {
        return TransactionSynchronizationSupport.getSynchronizations();
    }

    public static void clearSynchronization() throws IllegalStateException {
        TransactionSynchronizationSupport.clearSynchronization();
    }

    public static boolean isActualTransactionActive() {
        return TransactionSynchronizationSupport.isActualTransactionActive();
    }

    public static void clear() {
        TransactionSynchronizationSupport.clear();
    }

}
