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
package org.apache.cayenne.query;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.SQLResult;

/**
 * A selecting query based on raw SQL and featuring fluent API.
 * 
 * @since 4.0
 */
public class SQLSelect<T> extends IndirectQuery implements Select<T> {

	private static final long serialVersionUID = -7074293371883740872L;

	/**
	 * Creates a query that selects DataRows and uses default routing.
	 */
	public static SQLSelect<DataRow> dataRowQuery(String sql) {
		SQLSelect<DataRow> query = new SQLSelect<DataRow>(sql);
		return query;
	}

	/**
	 * Creates a query that selects DataRows and uses routing based on the
	 * provided DataMap name.
	 */
	public static SQLSelect<DataRow> dataRowQuery(String dataMapName, String sql) {
		SQLSelect<DataRow> query = new SQLSelect<DataRow>(sql);
		query.dataMapName = dataMapName;
		return query;
	}

	/**
	 * Creates a query that selects DataObjects.
	 */
	public static <T> SQLSelect<T> query(Class<T> type, String sql) {
		return new SQLSelect<T>(type, sql);
	}

	/**
	 * Creates a query that selects scalar values and uses default routing.
	 */
	public static <T> SQLSelect<T> scalarQuery(Class<T> type, String sql) {
		SQLSelect<T> query = new SQLSelect<T>(sql);
		query.scalarType = type;
		return query;
	}

	/**
	 * Creates a query that selects scalar values and uses routing based on the
	 * provided DataMap name.
	 */
	public static <T> SQLSelect<T> scalarQuery(Class<T> type, String dataMapName, String sql) {
		SQLSelect<T> query = new SQLSelect<T>(sql);
		query.dataMapName = dataMapName;
		query.scalarType = type;
		return query;
	}

	protected Class<T> persistentType;
	protected Class<T> scalarType;
	protected String dataMapName;
	protected StringBuilder sqlBuffer;
	protected QueryCacheStrategy cacheStrategy;
	protected String[] cacheGroups;
	protected Map<String, Object> params;
	protected List<Object> positionalParams;
	protected CapsStrategy columnNameCaps;
	protected int limit;
	protected int offset;
	protected int pageSize;
	protected int statementFetchSize;

	public SQLSelect(String sql) {
		this(null, sql);
	}

	public SQLSelect(Class<T> persistentType, String sql) {
		this.persistentType = persistentType;
		this.sqlBuffer = sql != null ? new StringBuilder(sql) : new StringBuilder();
		this.limit = QueryMetadata.FETCH_LIMIT_DEFAULT;
		this.offset = QueryMetadata.FETCH_OFFSET_DEFAULT;
		this.pageSize = QueryMetadata.PAGE_SIZE_DEFAULT;
	}

	/**
	 * Selects objects using provided context. Essentially the inversion of
	 * "ObjectContext.select(query)".
	 */
	public List<T> select(ObjectContext context) {
		return context.select(this);
	}

	/**
	 * Selects a single object using provided context. Essentially the inversion
	 * of "ObjectContext.selectOne(Select)".
	 */
	public T selectOne(ObjectContext context) {
		return context.selectOne(this);
	}

	public boolean isFetchingDataRows() {
		return persistentType == null;
	}

	public boolean isFetchingScalars() {
		return scalarType != null;
	}

	public String getSql() {
		String sql = sqlBuffer.toString();
		return sql.length() > 0 ? sql : null;
	}

	/**
	 * Appends a piece of SQL to the previously stored SQL template.
	 */
	public SQLSelect<T> append(String sqlChunk) {
		sqlBuffer.append(sqlChunk);
		this.replacementQuery = null;
		return this;
	}

	public SQLSelect<T> params(String name, Object value) {
		params(Collections.singletonMap(name, value));
		return this;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SQLSelect<T> params(Map<String, ?> parameters) {

		if (this.params == null) {
			this.params = new HashMap<String, Object>(parameters);
		} else {
			Map bareMap = parameters;
			this.params.putAll(bareMap);
		}

		this.replacementQuery = null;

		// since named parameters are specified, resetting positional
		// parameters
		this.positionalParams = null;
		return this;
	}

	public SQLSelect<T> paramsArray(Object... params) {
		return paramsList(params != null ? Arrays.asList(params) : null);
	}

	public SQLSelect<T> paramsList(List<Object> params) {
		// since named parameters are specified, resetting positional
		// parameters
		this.params = null;

		this.positionalParams = params;
		return this;
	}

	/**
	 * Returns an immmutable map of parameters that will be bound to SQL. A
	 * caller is free to add/remove parameters from the returned map as needed.
	 * Alternatively one may use chained {@link #params(String, Object)}
	 */
	public Map<String, Object> getParams() {
		return params != null ? params : Collections.<String, Object> emptyMap();
	}

	@Override
	protected Query createReplacementQuery(EntityResolver resolver) {

		Object root;

		if (persistentType != null) {
			root = persistentType;
		} else if (dataMapName != null) {
			DataMap map = resolver.getDataMap(dataMapName);
			if (map == null) {
				throw new CayenneRuntimeException("Invalid dataMapName '%s'", dataMapName);
			}

			root = map;
		} else {
			// will route via default node. TODO: allow explicit node name?
			root = null;
		}

		SQLTemplate template = new SQLTemplate();
		template.setFetchingDataRows(isFetchingDataRows());
		template.setRoot(root);
		template.setDefaultTemplate(getSql());
		template.setCacheGroups(cacheGroups);
		template.setCacheStrategy(cacheStrategy);

		if (positionalParams != null) {
			template.setParamsArray(positionalParams);
		} else {
			template.setParams(params);
		}

		template.setColumnNamesCapitalization(columnNameCaps);
		template.setFetchLimit(limit);
		template.setFetchOffset(offset);
		template.setPageSize(pageSize);
		template.setStatementFetchSize(statementFetchSize);

		if (isFetchingScalars()) {
			SQLResult resultMap = new SQLResult();
			resultMap.addColumnResult("x");
			template.setResult(resultMap);
		}

		return template;
	}

	/**
	 * Instructs Cayenne to look for query results in the "local" cache when
	 * running the query. This is a short-hand notation for:
	 * 
	 * <pre>
	 * query.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
	 * query.setCacheGroups(&quot;group1&quot;, &quot;group2&quot;);
	 * </pre>
	 * 
	 * @since 4.0
	 */
	public void useLocalCache(String... cacheGroups) {
		cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
		cacheGroups(cacheGroups);
	}

	/**
	 * Instructs Cayenne to look for query results in the "shared" cache when
	 * running the query. This is a short-hand notation for:
	 * 
	 * <pre>
	 * query.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
	 * query.setCacheGroups(&quot;group1&quot;, &quot;group2&quot;);
	 * </pre>
	 */
	public SQLSelect<T> useSharedCache(String... cacheGroups) {
		return cacheStrategy(QueryCacheStrategy.SHARED_CACHE).cacheGroups(cacheGroups);
	}

	public QueryCacheStrategy getCacheStrategy() {
		return cacheStrategy;
	}

	public SQLSelect<T> cacheStrategy(QueryCacheStrategy strategy) {
		if (this.cacheStrategy != strategy) {
			this.cacheStrategy = strategy;
			this.replacementQuery = null;
		}

		return this;
	}

	public String[] getCacheGroups() {
		return cacheGroups;
	}

	public SQLSelect<T> cacheGroups(String... cacheGroups) {
		this.cacheGroups = cacheGroups;
		this.replacementQuery = null;
		return this;
	}

	/**
	 * Returns a column name capitalization policy applied to selecting queries.
	 * This is used to simplify mapping of the queries like "SELECT * FROM ...",
	 * ensuring that a chosen Cayenne column mapping strategy (e.g. all column
	 * names in uppercase) is portable across database engines that can have
	 * varying default capitalization. Default (null) value indicates that
	 * column names provided in result set are used unchanged.
	 */
	public CapsStrategy getColumnNameCaps() {
		return columnNameCaps;
	}

	/**
	 * Sets a column name capitalization policy applied to selecting queries.
	 * This is used to simplify mapping of the queries like "SELECT * FROM ...",
	 * ensuring that a chosen Cayenne column mapping strategy (e.g. all column
	 * names in uppercase) is portable across database engines that can have
	 * varying default capitalization. Default (null) value indicates that
	 * column names provided in result set are used unchanged.
	 * <p>
	 * Note that while a non-default setting is useful for queries that do not
	 * rely on a #result directive to describe columns, it works for all
	 * SQLTemplates the same way.
	 */
	public SQLSelect<T> columnNameCaps(CapsStrategy columnNameCaps) {
		if (this.columnNameCaps != columnNameCaps) {
			this.columnNameCaps = columnNameCaps;
			this.replacementQuery = null;
		}

		return this;
	}

	/**
	 * Equivalent of setting {@link CapsStrategy#UPPER}
	 */
	public SQLSelect<T> upperColumnNames() {
		return columnNameCaps(CapsStrategy.UPPER);
	}

	/**
	 * Equivalent of setting {@link CapsStrategy#LOWER}
	 */
	public SQLSelect<T> lowerColumnNames() {
		return columnNameCaps(CapsStrategy.LOWER);
	}

	public int getLimit() {
		return limit;
	}

	public SQLSelect<T> limit(int fetchLimit) {
		if (this.limit != fetchLimit) {
			this.limit = fetchLimit;
			this.replacementQuery = null;
		}

		return this;
	}

	public int getOffset() {
		return offset;
	}

	public SQLSelect<T> offset(int fetchOffset) {
		if (this.offset != fetchOffset) {
			this.offset = fetchOffset;
			this.replacementQuery = null;
		}

		return this;
	}

	public int getPageSize() {
		return pageSize;
	}

	public SQLSelect<T> pageSize(int pageSize) {
		if (this.pageSize != pageSize) {
			this.pageSize = pageSize;
			this.replacementQuery = null;
		}

		return this;
	}

	/**
	 * Sets JDBC statement's fetch size (0 for no default size)
	 */
	public SQLSelect<T> statementFetchSize(int size) {
		if (this.statementFetchSize != size) {
			this.statementFetchSize = size;
			this.replacementQuery = null;
		}

		return this;
	}

	/**
	 * @return JBDC statement's fetch size
	 */
	public int getStatementFetchSize() {
		return statementFetchSize;
	}
}
