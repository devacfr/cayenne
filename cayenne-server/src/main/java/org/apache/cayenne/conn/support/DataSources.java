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

package org.apache.cayenne.conn.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class that provides static methods for obtaining JDBC Connections
 * from a {@link javax.sql.DataSource} supporting transactional Connections as
 * well.
 * 
 * @author devacfr<christophefriederich@mac.com>
 * @since 3.2
 */
public abstract class DataSources {

    /**
     * Static log instance.
     */
    private static final Log logger = LogFactory.getLog(DataSources.class);

    /**
     * Singletong instance implementing DataSource utilities method.
     */
    private static AtomicReference<DataSourceProvider> INSTANCE = new AtomicReference<DataSourceProvider>();

    /**
     * Registers the singleton's DataSource provider
     * 
     * @param provider
     */
    public synchronized static void registerProvider(DataSourceProvider provider) {
        if (provider == null) {
            return;
        }
        if (!INSTANCE.compareAndSet(null, provider)) {
            logger.warn("DataSourceProvider is exposed as final singleton and can not be change");
            return;
        }
    }

    /**
     * @return Gets the singleton provider.
     */
    private static DataSourceProvider getProvider() {
        final DataSourceProvider provider = INSTANCE.get();
        if (provider == null) {
            throw new CayenneRuntimeException("A DataSourceProvider must be declare before use");
        }
        return provider;
    }

    /**
     * Obtains a Connection from the given DataSource.
     * 
     * @param dataSource
     *            the DataSource to obtain Connections from.
     * @return a JDBC Connection from the given DataSource.
     * @throws DataSourceAccessException
     *             if the attempt to get a Connection failed.
     */
    public static Connection getConnection(final DataSource dataSource) throws DataSourceAccessException {
        try {
            return getProvider().getConnection(dataSource);
        } catch (SQLException ex) {
            throw new DataSourceAccessException("Could not get JDBC Connection", ex);
        }
    }

    /**
     * Closes the given Connection, obtained from the given DataSource, if it is
     * not managed externally (that is, not bound to the thread).
     * 
     * @param connection
     *            the Connection to close if necessary (if this is
     *            <code>null</code>, the call will be ignored).
     * @param dataSource
     *            the DataSource that the Connection was obtained from (may be
     *            <code>null</code>).
     */
    public static void releaseConnection(final Connection connection, final DataSource dataSource) {
        try {
            getProvider().releaseConnection(connection, dataSource);
        } catch (SQLException ex) {
            logger.debug("Could not close JDBC Connection", ex);
        } catch (Throwable ex) {
            logger.debug("Unexpected exception on closing JDBC Connection", ex);
        }
    }

}