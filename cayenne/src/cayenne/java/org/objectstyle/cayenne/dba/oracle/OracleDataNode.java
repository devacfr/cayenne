/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group
 * and individual authors of the software.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne"
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */
package org.objectstyle.cayenne.dba.oracle;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.trans.BatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.DeleteBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.InsertBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.UpdateBatchQueryBuilder;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.access.util.ResultDescriptor;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Query;

/**
 * DataNode subclass customized for Oracle database engine.
 * 
 * @author Andrei Adamchik
 */
public class OracleDataNode extends DataNode {

	public OracleDataNode() {
		super();
	}

	public OracleDataNode(String name) {
		super(name);
	}

	/**
	 * Implements Oracle-specific handling of StoredProcedure OUT parameters reading.
	 */
	protected void readStoredProcedureOutParameters(
		CallableStatement statement,
		ResultDescriptor descriptor,
		Query query,
		OperationObserver delegate)
		throws SQLException, Exception {

		long t1 = System.currentTimeMillis();

		int resultSetType = OracleAdapter.getOracleCursorType();
		int resultWidth = descriptor.getResultWidth();
		if (resultWidth > 0) {
			Map dataRow = new HashMap(resultWidth * 2, 0.75f);
			ExtendedType[] converters = descriptor.getConverters();
			int[] jdbcTypes = descriptor.getJdbcTypes();
			String[] names = descriptor.getNames();
			int[] outParamIndexes = descriptor.getOutParamIndexes();

			// process result row columns,
			for (int i = 0; i < outParamIndexes.length; i++) {
				int index = outParamIndexes[i];

				if (jdbcTypes[index] == resultSetType) {
					// note: jdbc column indexes start from 1, not 0 unlike everywhere else
					ResultSet rs = (ResultSet) statement.getObject(index + 1);
					ResultDescriptor nextDesc =
						ResultDescriptor.createDescriptor(
							rs,
							getAdapter().getExtendedTypes());

					readResultSet(
						rs,
						nextDesc,
						(GenericSelectQuery) query,
						delegate);
				} else {
					// note: jdbc column indexes start from 1, not 0 unlike everywhere else
					Object val =
						converters[index].materializeObject(
							statement,
							index + 1,
							jdbcTypes[index]);
					dataRow.put(names[index], val);
				}
			}

			if (!dataRow.isEmpty()) {
				QueryLogger.logSelectCount(
					query.getLoggingLevel(),
					1,
					System.currentTimeMillis() - t1);
				delegate.nextDataRows(
					query,
					Collections.singletonList(dataRow));
			}
		}
	}

	/**
	 * Implements Oracle-specific tweaks for BatchQuery processing. 
	 * Namely, Oracle requires trimming CHAR columns that are used in joins,
	 * special LOB handling, etc. 
	 */
	protected void runBatchUpdate(
		Connection con,
		BatchQuery query,
		OperationObserver delegate)
		throws SQLException, Exception {

		// Oracle customization: supply trim functionfor batch queries
		BatchQueryBuilder queryBuilder;
		switch (query.getQueryType()) {
			case Query.INSERT_QUERY :
				queryBuilder = new InsertBatchQueryBuilder(getAdapter());
				break;
			case Query.UPDATE_QUERY :
				queryBuilder =
					new UpdateBatchQueryBuilder(
						getAdapter(),
						OracleAdapter.TRIM_FUNCTION);
				break;
			case Query.DELETE_QUERY :
				queryBuilder =
					new DeleteBatchQueryBuilder(
						getAdapter(),
						OracleAdapter.TRIM_FUNCTION);
				break;
			default :
				throw new CayenneException(
					"Unsupported batch type: " + query.getQueryType());
		}

		if (adapter.supportsBatchUpdates()) {
			runBatchUpdateAsBatch(con, query, queryBuilder, delegate);
		} else {
			runBatchUpdateAsIndividualQueries(
				con,
				query,
				queryBuilder,
				delegate);
		}
	}
}
