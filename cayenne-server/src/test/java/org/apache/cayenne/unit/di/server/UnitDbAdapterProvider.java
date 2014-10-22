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
package org.apache.cayenne.unit.di.server;

import java.lang.reflect.Constructor;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.util.Util;

@Singleton
public class UnitDbAdapterProvider implements Provider<UnitDbAdapter> {

    static final String TEST_ADAPTERS_MAP = "org.apache.cayenne.unit.di.server.CayenneResourcesAccessStackAdapterProvider.adapters";

    private Injector injector;
    private DbAdapter adapter;
    private DataSourceInfo dataSourceInfo;
    private Map<String, String> adapterTypesMap;

    private UnitDbAdapter unitDbAdapter;

    @Inject
    public UnitDbAdapterProvider(@Named(TEST_ADAPTERS_MAP) Map<String, String> adapterTypesMap,
            DataSourceInfo dataSourceInfo, DbAdapter adapter, Injector injector) {
        this.dataSourceInfo = dataSourceInfo;
        this.adapterTypesMap = adapterTypesMap;
        this.adapter = adapter;
        this.injector = injector;
    }

    @Override
    public UnitDbAdapter get() throws ConfigurationException {
        if (unitDbAdapter == null) {
            String testAdapterType = adapterTypesMap.get(dataSourceInfo.getAdapterClassName());
            if (testAdapterType == null) {
                throw new IllegalStateException("Unmapped adapter type: " + dataSourceInfo.getAdapterClassName());
            }

            Class<UnitDbAdapter> type;
            try {
                type = (Class<UnitDbAdapter>) Util.getJavaClass(testAdapterType);
            } catch (ClassNotFoundException e) {
                throw new CayenneRuntimeException("Invalid class %s of type AccessStackAdapter", e, testAdapterType);
            }

            if (!UnitDbAdapter.class.isAssignableFrom(type)) {
                throw new CayenneRuntimeException("Class %s is not assignable to AccessStackAdapter", testAdapterType);
            }

            try {
                Constructor<UnitDbAdapter> c = type.getConstructor(DbAdapter.class);
                UnitDbAdapter instance = c.newInstance(adapter);
                injector.injectMembers(instance);
                unitDbAdapter = instance;
            } catch (Exception e) {
                throw new ConfigurationException("Error instantiating " + testAdapterType, e);
            }
        }
        return unitDbAdapter;
    }
}
