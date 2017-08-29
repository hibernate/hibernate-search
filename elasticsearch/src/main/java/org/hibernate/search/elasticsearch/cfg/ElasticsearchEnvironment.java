/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.cfg;

import org.hibernate.search.elasticsearch.analyzer.definition.ElasticsearchAnalysisDefinitionProvider;

/**
 * Configuration properties for Elasticsearch,
 *
 * @author Gunnar Morling
 */
public final class ElasticsearchEnvironment {

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		public static final DynamicType DYNAMIC_MAPPING = DynamicType.STRICT;
		public static final String SERVER_URI = "http://localhost:9200";
		public static final int SERVER_REQUEST_TIMEOUT = 60000;
		public static final int SERVER_READ_TIMEOUT = 60000;
		public static final int SERVER_CONNECTION_TIMEOUT = 3000;
		public static final int MAX_TOTAL_CONNECTION = 20;
		public static final int MAX_TOTAL_CONNECTION_PER_ROUTE = 2;
		public static final boolean DISCOVERY_ENABLED = false;
		public static final int DISCOVERY_REFRESH_INTERVAL = 10;
		public static final String DISCOVERY_SCHEME = "http";
		public static final IndexSchemaManagementStrategy INDEX_SCHEMA_MANAGEMENT_STRATEGY = IndexSchemaManagementStrategy.CREATE;
		public static final int INDEX_MANAGEMENT_WAIT_TIMEOUT = 10_000;
		public static final ElasticsearchIndexStatus REQUIRED_INDEX_STATUS = ElasticsearchIndexStatus.GREEN;
		public static final boolean REFRESH_AFTER_WRITE = false;
		public static final int SCROLL_BACKTRACKING_WINDOW_SIZE = 10_000;
		public static final int SCROLL_FETCH_SIZE = 1_000;
		public static final int SCROLL_TIMEOUT = 60;
		public static final boolean LOG_JSON_PRETTY_PRINTING = false;
	}

	/**
	 * Property for specifying the host names and HTTP ports of the Elasticsearch servers to connect to.
	 * <p>
	 * URIs such as http://myeshost.com:9200 are expected, separated by whitespace characters.
	 * <p>
	 * Defaults to {@link Defaults#SERVER_URI}.
	 * <p>
	 * Multiple servers may be specified for load-balancing: requests will be assigned to each host in turns.
	 * Failover is not supported yet.
	 * <p>
	 * To be given <b>globally</b> only (i.e. prefixed with {@code hibernate.search.default.}).
	 * <b>Cannot</b> be specified per index (e.g. {@code hibernate.search.myIndex.elasticsearch.host}).
	 * This limitation will be removed in a future version of Hibernate Search.
	 */
	public static final String SERVER_URI = "elasticsearch.host";

	/**
	 * Property for specifying the username to send when connecting to the Elasticsearch servers.
	 * <p>
	 * A string is expected.
	 * <p>
	 * Defaults to no username (anonymous access).
	 * <p>
	 * To be given <b>globally</b> only (i.e. prefixed with {@code hibernate.search.default.}).
	 * <b>Cannot</b> be specified per index (e.g. {@code hibernate.search.myIndex.elasticsearch.username}).
	 * This limitation will be removed in a future version of Hibernate Search.
	 */
	public static final String SERVER_USERNAME = "elasticsearch.username";

	/**
	 * Property for specifying the password to send when connecting to the Elasticsearch servers.
	 * <p>
	 * A string is expected.
	 * <p>
	 * Defaults to no password at all.
	 * <p>
	 * To be given <b>globally</b> only (i.e. prefixed with {@code hibernate.search.default.}).
	 * <b>Cannot</b> be specified per index (e.g. {@code hibernate.search.myIndex.elasticsearch.password}).
	 * This limitation will be removed in a future version of Hibernate Search.
	 */
	public static final String SERVER_PASSWORD = "elasticsearch.password";

	/**
	 * Property for specifying the timeout when executing a request to an Elasticsearch server.
	 * <p>
	 * This includes the time needed to establish a connection, send the request and receive the whole response,
	 * optionally re-trying multiple times in case of node failure.
	 * <p>
	 * A numeric value in milliseconds, such as 60000 is expected.
	 * <p>
	 * Defaults to {@link Defaults#SERVER_REQUEST_TIMEOUT}.
	 * <p>
	 * To be given <b>globally</b> only (i.e. prefixed with {@code hibernate.search.default.}).
	 * <b>Cannot</b> be specified per index (e.g. {@code hibernate.search.myIndex.elasticsearch.request_timeout}).
	 * This limitation will be removed in a future version of Hibernate Search.
	 */
	public static final String SERVER_REQUEST_TIMEOUT = "elasticsearch.request_timeout";

	/**
	 * Property for specifying the timeout when reading responses from an Elasticsearch server.
	 * <p>
	 * A numeric value in milliseconds, such as 60000 is expected.
	 * <p>
	 * Defaults to {@link Defaults#SERVER_READ_TIMEOUT}.
	 * <p>
	 * To be given <b>globally</b> only (i.e. prefixed with {@code hibernate.search.default.}).
	 * <b>Cannot</b> be specified per index (e.g. {@code hibernate.search.myIndex.elasticsearch.read_timeout}).
	 * This limitation will be removed in a future version of Hibernate Search.
	 */
	public static final String SERVER_READ_TIMEOUT = "elasticsearch.read_timeout";

	/**
	 * Property for specifying the timeout when connecting to an Elasticsearch server.
	 * <p>
	 * A numeric value in milliseconds, such as 2000 is expected.
	 * <p>
	 * Defaults to {@link Defaults#SERVER_CONNECTION_TIMEOUT}.
	 * <p>
	 * To be given <b>globally</b> only (i.e. prefixed with {@code hibernate.search.default.}).
	 * <b>Cannot</b> be specified per index (e.g. {@code hibernate.search.myIndex.elasticsearch.connection_timeout}).
	 * This limitation will be removed in a future version of Hibernate Search.
	 */
	public static final String SERVER_CONNECTION_TIMEOUT = "elasticsearch.connection_timeout";

	/**
	 * Property for specifying the maximum number of simultaneous connections to the Elasticsearch cluster.
	 * <p>
	 * A positive numeric value is expected.
	 * <p>
	 * Defaults to {@link Defaults#MAX_TOTAL_CONNECTION}.
	 * <p>
	 * To be given <b>globally</b> only (i.e. prefixed with {@code hibernate.search.default.}).
	 * <b>Cannot</b> be specified per index (e.g. {@code hibernate.search.myIndex.elasticsearch.max_total_connection}).
	 * This limitation will be removed in a future version of Hibernate Search.
	 */
	public static final String MAX_TOTAL_CONNECTION = "elasticsearch.max_total_connection";

	/**
	 * Property for specifying the maximum number of simultaneous connections to a single Elasticsearch server.
	 * <p>
	 * A positive numeric value is expected.
	 * <p>
	 * Defaults to {@link Defaults#MAX_TOTAL_CONNECTION_PER_ROUTE}.
	 * <p>
	 * To be given <b>globally</b> only (i.e. prefixed with {@code hibernate.search.default.}).
	 * <b>Cannot</b> be specified per index (e.g. {@code hibernate.search.myIndex.elasticsearch.max_total_connection_per_route}).
	 * This limitation will be removed in a future version of Hibernate Search.
	 */
	public static final String MAX_TOTAL_CONNECTION_PER_ROUTE = "elasticsearch.max_total_connection_per_route";

	/**
	 * Property for specifying whether automatic discovery of nodes in the Elasticsearch cluster is enabled.
	 * <p>
	 * Either {@code true} or {@code false} is expected.
	 * <p>
	 * Defaults to {@link Defaults#DISCOVERY_ENABLED}.
	 * <p>
	 * To be given <b>globally</b> only (i.e. prefixed with {@code hibernate.search.default.}).
	 * <b>Cannot</b> be specified per index (e.g. {@code hibernate.search.myIndex.elasticsearch.discovery.enabled}).
	 * This limitation will be removed in a future version of Hibernate Search.
	 */
	public static final String DISCOVERY_ENABLED = "elasticsearch.discovery.enabled";

	/**
	 * Property for specifying the time interval between two executions of the automatic discovery, if enabled.
	 * <p>
	 * A positive numeric value in seconds is expected.
	 * <p>
	 * Defaults to {@link Defaults#DISCOVERY_REFRESH_INTERVAL}.
	 * <p>
	 * To be given <b>globally</b> only (i.e. prefixed with {@code hibernate.search.default.}).
	 * <b>Cannot</b> be specified per index (e.g. {@code hibernate.search.myIndex.elasticsearch.discovery.refresh_interval}).
	 * This limitation will be removed in a future version of Hibernate Search.
	 */
	public static final String DISCOVERY_REFRESH_INTERVAL = "elasticsearch.discovery.refresh_interval";

	/**
	 * Property for specifying the default scheme to use when connecting to automatically discovered nodes.
	 * <p>
	 * Either "http" or "https" is expected.
	 * <p>
	 * Defaults to {@link Defaults#DISCOVERY_SCHEME}.
	 * <p>
	 * To be given <b>globally</b> only (i.e. prefixed with {@code hibernate.search.default.}).
	 * <b>Cannot</b> be specified per index (e.g. {@code hibernate.search.myIndex.elasticsearch.discovery.default_scheme}).
	 * This limitation will be removed in a future version of Hibernate Search.
	 */
	public static final String DISCOVERY_SCHEME = "elasticsearch.discovery.default_scheme";

	/**
	 * Property for specifying the strategy for maintaining the Elasticsearch index.
	 * <p>
	 * The external name of one of the {@link IndexSchemaManagementStrategy} constants is expected, e.g. 'update'
	 * (the external names can be retrieved programmatically using {@link IndexSchemaManagementStrategy#getExternalName}).
	 * <p>
	 * Can be given globally (e.g. {@code hibernate.search.default.elasticsearch.index_schema_management_strategy=update}) or
	 * for specific indexes (e.g. {@code hibernate.search.someindex.elasticsearch.index_schema_management_strategy=drop-and-create}).
	 */
	public static final String INDEX_SCHEMA_MANAGEMENT_STRATEGY = "elasticsearch.index_schema_management_strategy";

	/**
	 * Property for specifying the timeout for index management operations (index creation etc.) in milli-seconds.
	 * <p>
	 * A numeric value such as 1000 is expected.
	 * <p>
	 * Defaults to {@link Defaults#INDEX_MANAGEMENT_WAIT_TIMEOUT} ms.
	 * <p>
	 * Can be given globally (e.g. {@code hibernate.search.default.elasticsearch.index_management_wait_timeout=5000}) or
	 * for specific indexes (e.g. {@code hibernate.search.someindex.elasticsearch.index_management_wait_timeout=2000}).
	 */
	public static final String INDEX_MANAGEMENT_WAIT_TIMEOUT = "elasticsearch.index_management_wait_timeout";

	/**
	 * Property for specifying the status an index must at least have in order for Hibernate Search to work with it.
	 * <p>
	 * One of 'green', 'yellow' or 'red' is expected.
	 * <p>
	 * Defaults to {@link Defaults#REQUIRED_INDEX_STATUS}.
	 * <p>
	 * Can be given globally (e.g. {@code hibernate.search.default.elasticsearch.required_index_status=green}) or for
	 * specific indexes (e.g. {@code hibernate.search.someindex.elasticsearch.required_index_status=yellow}).
	 */
	public static final String REQUIRED_INDEX_STATUS = "elasticsearch.required_index_status";

	/**
	 * Property for specifying whether an explicit index refresh should be issued after a set of operations targeting a
	 * given index has been executed or not.
	 * <p>
	 * A boolean value (true, false) is expected.
	 * <p>
	 * Defaults to {@link Defaults#REFRESH_AFTER_WRITE}.
	 * <p>
	 * Can be given globally (e.g. {@code hibernate.search.default.elasticsearch.refresh_after_write=false}) or for
	 * specific indexes (e.g. {@code hibernate.search.someindex.elasticsearch.refresh_after_write=true}).
	 */
	public static final String REFRESH_AFTER_WRITE = "elasticsearch.refresh_after_write";

	/**
	 * Property for specifying the the minimum number of previous results kept in memory at any time when scrolling.
	 * <p>
	 * This determines the number of positions one will be able to scroll backward (<em>backtracking</em>) without
	 * starting over the scrolling.
	 * <p>
	 * A strictly positive value is expected.
	 * <p>
	 * Defaults to {@link Defaults#SCROLL_BACKTRACKING_WINDOW_SIZE}.
	 * <p>
	 * Can only be given <b>globally</b> (e.g.
	 * {@code hibernate.search.elasticsearch.scroll_backtracking_window_size=10000}).
	 */
	public static final String SCROLL_BACKTRACKING_WINDOW_SIZE = "elasticsearch.scroll_backtracking_window_size";

	/**
	 * Property for specifying the the number of results fetched by each Elasticsearch call when scrolling.
	 * <p>
	 * A strictly positive value is expected.
	 * <p>
	 * Defaults to {@link Defaults#SCROLL_FETCH_SIZE}.
	 * <p>
	 * Can only be given <b>globally</b> (e.g.
	 * {@code hibernate.search.elasticsearch.scroll_fetch_size=1000}).
	 */
	public static final String SCROLL_FETCH_SIZE = "elasticsearch.scroll_fetch_size";

	/**
	 * Property for specifying the maximum duration {@code ScrollableResults} will be usable if no
	 * other results are fetched from Elasticsearch, in seconds.
	 * <p>
	 * A strictly positive value is expected.
	 * <p>
	 * Defaults to {@link Defaults#SCROLL_TIMEOUT}.
	 * <p>
	 * Can only be given <b>globally</b> (e.g.
	 * {@code hibernate.search.elasticsearch.scroll_timeout=60}).
	 */
	public static final String SCROLL_TIMEOUT = "elasticsearch.scroll_timeout";

	/**
	 * Equivalent to elasticsearch "dynamic" mapping attribute, define what to do when an indexed document
	 * contains a field which was not declared in the index schema.
	 * <p>
	 * Possible values are:
	 * <ul>
	 * <li>{@code true}: Add unknown fields to the schema dynamically</li>
	 * <li>{@code false}: Ignore unknown fields</li>
	 * <li>{@code strict}: Throw an exception on unknown fields</li>
	 * </ul>
	 * <p>
	 * Defaults to {@code strict}.
	 * <p>
	 * Can be given globally (e.g. {@code hibernate.search.default.elasticsearch.dynamic_mapping=false}) or for
	 * specific indexes (e.g. {@code hibernate.search.someindex.elasticsearch.dynamic_mapping=true}).
	 */
	public static final String DYNAMIC_MAPPING = "elasticsearch.dynamic_mapping";

	/**
	 * Provider of default analysis-related definitions for Elasticsearch.
	 * <p>
	 * The value must be the fully-qualified name of a class implementing {@link ElasticsearchAnalysisDefinitionProvider}.
	 */
	public static final String ANALYSIS_DEFINITION_PROVIDER = "hibernate.search.elasticsearch.analysis_definition_provider";

	/**
	 * Whether JSON included in logs should be pretty-printed (indented, with line breaks).
	 * <p>
	 * A boolean value (true, false) is expected.
	 * <p>
	 * Defaults to {@link Defaults#LOG_JSON_PRETTY_PRINTING}.
	 * <p>
	 * Can only be given <b>globally</b> (e.g.
	 * {@code hibernate.search.elasticsearch.log.json_pretty_printing=true}).
	 */
	public static final String LOG_JSON_PRETTY_PRINTING = "elasticsearch.log.json_pretty_printing";

	private ElasticsearchEnvironment() {
	}
}
