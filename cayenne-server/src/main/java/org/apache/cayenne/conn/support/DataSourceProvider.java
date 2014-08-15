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
package org.apache.cayenne.conn.support;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * @author devacfr<christophefriederich@mac.com>
 *
 */
public interface DataSourceProvider {

    /**
     * Obtains a Connection from the given DataSource.
     * 
     * @param dataSource
     *            the DataSource to obtain Connections from.
     * @return a JDBC Connection from the given DataSource.
     * @throws SQLException
     *             if the attempt to get a Connection failed.
     */
    Connection getConnection(final DataSource dataSource) throws SQLException;

    /**
     * Closes the given Connection, obtained from the given DataSource, if it is
     * not managed externally (that is, not bound to the thread).
     * 
     * @param connection
     *            the Connection to close if necessary (if this is
     *            <code>null</code>, the call will be ignored).
     * @param dataSource
     *            the DataSource that the Connection was obtained from (may be
     *            <code>null</code>)
     * @throws SQLException
     *             if thrown by JDBC methods.
     */
    void releaseConnection(final Connection connection, final DataSource dataSource) throws SQLException;

}
