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
package org.apache.cayenne.di.mock;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.cayenne.di.Injector;

public class Mock_Implementation1_Provider_Lifecyle implements Provider<MockInterface1> {

    private final MockInterface1 interface1;

    /**
     *
     */
    @Inject
    public Mock_Implementation1_Provider_Lifecyle(Injector injector) {
        this.interface1 = new Mock_Implementation1_MultiPostConstruct();
        injector.injectMembers(interface1);
    }

    @Override
    public MockInterface1 get() {
        return interface1;
    }
}
