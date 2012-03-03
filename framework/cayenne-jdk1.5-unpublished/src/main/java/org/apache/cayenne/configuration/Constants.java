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
package org.apache.cayenne.configuration;

/**
 * Defines the names of runtime properties and DI collections used in DI modules used to
 * configure server and client runtime.
 * 
 * @since 3.1
 */
public interface Constants {

    // DI "collections"

    /**
     * A DI container key for the Map<String, String> storing properties used by built-in
     * Cayenne service.
     */
    public static final String PROPERTIES_MAP = "cayenne.properties";

    /**
     * A DI container key for the List<DbAdapterDetector> that contains objects that can
     * discover the type of current database and install the correct DbAdapter in runtime.
     */
    public static final String SERVER_ADAPTER_DETECTORS_LIST = "cayenne.server.adapter_detectors";

    /**
     * A DI container key for the List<DataChannelFilter> storing DataDomain filters.
     */
    public static final String SERVER_DOMAIN_FILTERS_LIST = "cayenne.server.domain_filters";

    /**
     * A DI container key for the List<String> storing locations of the one of more
     * project configuration files.
     */
    public static final String SERVER_PROJECT_LOCATIONS_LIST = "cayenne.server.project_locations";

    /**
     * A DI container key for the List<ExtendedType> storing default adapter-agnostic
     * ExtendedTypes.
     */
    public static final String SERVER_DEFAULT_TYPES_LIST = "cayenne.server.default_types";

    /**
     * A DI container key for the List<ExtendedType> storing a user-provided
     * ExtendedTypes.
     */
    public static final String SERVER_USER_TYPES_LIST = "cayenne.server.user_types";

    /**
     * A DI container key for the List<ExtendedTypeFactory> storing default and
     * user-provided ExtendedTypeFactories.
     */
    public static final String SERVER_TYPE_FACTORIES_LIST = "cayenne.server.type_factories";

    // Runtime properties

    public static final String JDBC_DRIVER_PROPERTY = "cayenne.jdbc.driver";

    public static final String JDBC_URL_PROPERTY = "cayenne.jdbc.url";

    public static final String JDBC_USERNAME_PROPERTY = "cayenne.jdbc.username";

    public static final String JDBC_PASSWORD_PROPERTY = "cayenne.jdbc.password";

    public static final String JDBC_MIN_CONNECTIONS_PROPERTY = "cayenne.jdbc.min_connections";

    public static final String JDBC_MAX_CONNECTIONS_PROPERTY = "cayenne.jdbc.max_connections";

    /**
     * An integer property defining the maximum number of entries in the query cache. Note
     * that not all QueryCache providers may respect this property. MapQueryCache uses it,
     * but the rest would use alternative configuration methods.
     */
    public static final String QUERY_CACHE_SIZE_PROPERTY = "cayenne.querycache.size";

    /**
     * A boolean property defining whether cross-contexts synchronization is enabled.
     * Possible values are "true" or "false".
     */
    public static final String SERVER_CONTEXTS_SYNC_PROPERTY = "cayenne.server.contexts_sync_strategy";

    /**
     * A String property that defines how ObjectContexts should retain cached committed
     * objects. Possible values are "weak", "soft", "hard".
     */
    public static final String SERVER_OBJECT_RETAIN_STRATEGY_PROPERTY = "cayenne.server.object_retain_strategy";

    public static final String ROP_SERVICE_URL_PROPERTY = "cayenne.rop.service_url";

    public static final String ROP_SERVICE_USERNAME_PROPERTY = "cayenne.rop.service_username";

    public static final String ROP_SERVICE_PASSWORD_PROPERTY = "cayenne.rop.service_password";

    public static final String ROP_SERVICE_SHARED_SESSION_PROPERTY = "cayenne.rop.shared_session_name";

    public static final String ROP_SERVICE_TIMEOUT_PROPERTY = "cayenne.rop.service.timeout";

    public static final String ROP_CHANNEL_EVENTS_PROPERTY = "cayenne.rop.channel_events";

    public static final String ROP_CONTEXT_CHANGE_EVENTS_PROPERTY = "cayenne.rop.context_change_events";

    public static final String ROP_CONTEXT_LIFECYCLE_EVENTS_PROPERTY = "cayenne.rop.context_lifecycle_events";

}
