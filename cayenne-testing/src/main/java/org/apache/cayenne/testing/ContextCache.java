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

package org.apache.cayenne.testing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.testing.support.CayenneRuntimeInvoker;

/**
 *
 * @since3.2
 *
 */
class ContextCache {

    private final Map<String, CayenneRuntimeInvoker> runtimeMap = new ConcurrentHashMap<String, CayenneRuntimeInvoker>();

    private int hitCount;

    private int missCount;

    void clear() {
        this.runtimeMap.clear();
    }

    void clearStatistics() {
        this.hitCount = 0;
        this.missCount = 0;
    }

    boolean contains(String key) {
        return this.runtimeMap.containsKey(key);
    }

    CayenneRuntimeInvoker get(String key) {
        CayenneRuntimeInvoker runtime = this.runtimeMap.get(key);
        if (runtime == null) {
            incrementMissCount();
        } else {
            incrementHitCount();
        }
        return runtime;
    }

    private void incrementHitCount() {
        this.hitCount++;
    }

    private void incrementMissCount() {
        this.missCount++;
    }

    int getHitCount() {
        return this.hitCount;
    }

    int getMissCount() {
        return this.missCount;
    }

    void put(String key, CayenneRuntimeInvoker runtime) {
        this.runtimeMap.put(key, runtime);
    }

    CayenneRuntimeInvoker remove(String key) {
        return this.runtimeMap.remove(key);
    }

    void setDirty(String key) {
        CayenneRuntimeInvoker runtime = remove(key);
        if (runtime != null) {
            runtime.shutdown();
        }
    }

    int size() {
        return this.runtimeMap.size();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("ContextCache:");
        str.append("size: ").append(size());
        str.append(", hitcount:").append(getHitCount());
        str.append(", missCount:").append(getMissCount());
        return str.toString();
    }

}
