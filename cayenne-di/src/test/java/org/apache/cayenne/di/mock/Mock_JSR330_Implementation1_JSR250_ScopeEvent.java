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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.cayenne.di.BeforeScopeEnd;

public class Mock_JSR330_Implementation1_JSR250_ScopeEvent extends
        MockImplementation1_EventAnnotationsBase implements MockInterface1 {



    @Override
	public String getName() {
        return "XuI";
    }

    @PostConstruct
    public void initialize() {
    	initialize1 = true;
    }

    @PreDestroy
    public void onShutdown1() {
        shutdown1 = true;
    }

    @BeforeScopeEnd
    public void onShutdown2() {
        shutdown2 = true;
    }
}
