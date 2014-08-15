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

/**
 * JDBC Connection handler.
 *
 * @author devacfr<christophefriederich@mac.com>
 * 
 * @since 3.2
 */
public class ConnectionHandle  {

	private final Connection connection;


	/**
	 * Create a new ConnectionHandle for the given Connection.
	 * @param connection the JDBC Connection (never <code>null</code>).
	 */
	public ConnectionHandle(Connection connection) {
	    if (connection == null) {
            throw new IllegalArgumentException("connection argument is required");
        }
		this.connection = connection;
	}

	/**
	 * Gets the specified Connection.
	 * @return Returns a {@link Connection} (never <code>null</code>).
	 */
	public Connection getConnection() {
		return this.connection;
	}



	@Override
	public String toString() {
		return "ConnectionHandle: " + this.connection;
	}

}