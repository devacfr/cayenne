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
package org.apache.cayenne.unit.di.client;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.configuration.rop.client.ClientLocalRuntime;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.event.EventListener;
import org.apache.cayenne.di.event.RefreshContextEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ClientRuntimeProvider implements Provider<ClientRuntime> {

    private static Log LOGGER = LogFactory.getLog(ClientRuntimeProvider.class);

    @Inject
    // injecting provider to make this provider independent from scoping of
    // ServerRuntime
    protected Provider<ServerRuntime> serverRuntimeProvider;

    @Inject
    protected ClientCaseProperties clientCaseProperties;

    private ClientLocalRuntime runtime;

    @Override
    public ClientRuntime get() throws ConfigurationException {
        if (runtime == null) {
            Injector serverInjector = serverRuntimeProvider.get().getInjector();
            runtime = new ClientLocalRuntime(serverInjector, clientCaseProperties.getRuntimeProperties(),
                    new ClientExtraModule(serverInjector));
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("get(): Creating new ClientRuntime instance ["+ runtime + "]");
            }
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("get(): get ClientRuntime instance ["+ runtime + "]");
        }
        return runtime;
    }

    @EventListener
    public void onRefresh(RefreshContextEvent event) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("onRefresh(): shutdown current ClientRuntime");
        }
        if (runtime != null) {
            runtime.shutdown();
        }
        runtime = null;
    }

    class ClientExtraModule implements Module {

        private Injector serverInjector;

        ClientExtraModule(Injector serverInjector) {
            this.serverInjector = serverInjector;
        }

        @Override
        public void configure(Binder binder) {

            // these are the objects overriding standard ClientLocalModule
            // definitions or
            // dependencies needed by such overrides

            // add an interceptor between client and server parts to capture and
            // inspect
            // the traffic
            binder.bind(Key.get(DataChannel.class, ClientLocalRuntime.CLIENT_SERVER_CHANNEL_KEY)).toProviderInstance(
                    new InterceptingClientServerChannelProvider(serverInjector));
        }
    }
}
