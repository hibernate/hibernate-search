/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.cfg;

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
		public static final int SERVER_READ_TIMEOUT = 60000;
		public static final int SERVER_CONNECTION_TIMEOUT = 3000;
		public static final int MAX_TOTAL_CONNECTION = 20;
		public static final int MAX_TOTAL_CONNECTION_PER_ROUTE = 2;
		public static final boolean DISCOVERY_ENABLED = false;
		public static final long DISCOVERY_REFRESH_INTERVAL = 10L;
		public static final IndexSchemaManagementStrategy INDEX_SCHEMA_MANAGEMENT_STRATEGY = IndexSchemaManagementStrategy.CREATE;
		public static final int INDEX_MANAGEMENT_WAIT_TIMEOUT = 10_000;
		public static final ElasticsearchIndexStatus REQUIRED_INDEX_STATUS = ElasticsearchIndexStatus.GREEN;
		public static final boolean REFRESH_AFTER_WRITE = false;
		public static final int SCROLL_BACKTRACKING_WINDOW_SIZE = 10_000;
		public static final int SCROLL_FETCH_SIZE = 1_000;
		public static final int SCROLL_TIMEOUT = 60;
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
	 * Can only be given <b>globally</b> (e.g.
	 * {@code hibernate.search.default.elasticsearch.host=http://myeshost.com:9200}), i.e. only a single Elasticsearch
	 * cluster is supported for all the indexed entities. This limitation will be removed in a future version of
	 * Hibernate Search.
	 * <p>
	 */
	public static final String SERVER_URI = "elasticsearch.host";

	/**
	 * Property for specifying the timeout when reading responses from an Elasticsearch server.
	 * <p>
	 * A numeric value in milliseconds, such as 60000 is expected.
	 * <p>
	 * Defaults to {@link Defaults#SERVER_READ_TIMEOUT}.
	 * <p>
	 * Can only be given <b>globally</b> (e.g.
	 * {@code hibernate.search.default.elasticsearch.read_timeout=60000}), because only a single Elasticsearch
	 * cluster is supported for all the indexed entities. This limitation will be removed in a future version of
	 * Hibernate Search.
	 */
	public static final String SERVER_READ_TIMEOUT = "elasticsearch.read_timeout";

	/**
	 * Property for specifying the timeout when connecting to an Elasticsearch server.
	 * <p>
	 * A numeric value in milliseconds, such as 2000 is expected.
	 * <p>
	 * Defaults to {@link Defaults#SERVER_CONNECTION_TIMEOUT}.
	 * <p>
	 * Can only be given <b>globally</b> (e.g.
	 * {@code hibernate.search.default.elasticsearch.connection_timeout=2000}), because only a single Elasticsearch
	 * cluster is supported for all the indexed entities. This limitation will be removed in a future version of
	 * Hibernate Search.
	 */
	public static final String SERVER_CONNECTION_TIMEOUT = "elasticsearch.connection_timeout";

	/**
	 * Property for specifying the maximum number of simultaneous connections to the Elasticsearch cluster.
	 * <p>
	 * A positive numeric value is expected.
	 * <p>
	 * Defaults to {@link Defaults#MAX_TOTAL_CONNECTION}.
	 * <p>
	 * Can only be given <b>globally</b> (e.g.
	 * {@code hibernate.search.default.elasticsearch.max_total_connection=30}), because only a single Elasticsearch
	 * cluster is supported for all the indexed entities. This limitation will be removed in a future version of
	 * Hibernate Search.
	 */
	public static final String MAX_TOTAL_CONNECTION = "elasticsearch.max_total_connection";

	/**
	 * Property for specifying the maximum number of simultaneous connections to a single Elasticsearch server.
	 * <p>
	 * A positive numeric value is expected.
	 * <p>
	 * Defaults to {@link Defaults#MAX_TOTAL_CONNECTION_PER_ROUTE}.
	 * <p>
	 * Can only be given <b>globally</b> (e.g.
	 * {@code hibernate.search.default.elasticsearch.max_total_connection_per_route=3}), because only a single Elasticsearch
	 * cluster is supported for all the indexed entities. This limitation will be removed in a future version of
	 * Hibernate Search.
	 */
	public static final String MAX_TOTAL_CONNECTION_PER_ROUTE = "elasticsearch.max_total_connection_per_route";

	/**
	 * Property for specifying whether automatic discovery of nodes in the Elasticsearch cluster is enabled.
	 * <p>
	 * Either {@code true} or {@code false} is expected.
	 * <p>
	 * Defaults to {@link Defaults#DISCOVERY_ENABLED}.
	 * <p>
	 * Can only be given <b>globally</b> (e.g.
	 * {@code hibernate.search.default.elasticsearch.discovery.enabled=true}), because only a single Elasticsearch
	 * cluster is supported for all the indexed entities. This limitation will be removed in a future version of
	 * Hibernate Search.
	 */
	public static final String DISCOVERY_ENABLED = "elasticsearch.discovery.enabled";

	/**
	 * Property for specifying the time interval between two executions of the automatic discovery, if enabled.
	 * <p>
	 * A positive numeric value in seconds is expected.
	 * <p>
	 * Defaults to {@link Defaults#DISCOVERY_REFRESH_INTERVAL}.
	 * <p>
	 * Can only be given <b>globally</b> (e.g.
	 * {@code hibernate.search.default.elasticsearch.discovery.refresh_interval=20}), because only a single Elasticsearch
	 * cluster is supported for all the indexed entities. This limitation will be removed in a future version of
	 * Hibernate Search.
	 */
	public static final String DISCOVERY_REFRESH_INTERVAL = "elasticsearch.discovery.refresh_interval";

	/**
	 * Property for specifying the strategy for maintaining the Elasticsearch index.
	 * <p>
	 * The name of one of the {@link IndexSchemaManagementStrategy} constants is expected, e.g. MERGE.
	 * <p>
	 * Can be given globally (e.g. {@code hibernate.search.default.elasticsearch.index_schema_management_strategy=MERGE}) or
	 * for specific indexes (e.g. {@code hibernate.search.someindex.elasticsearch.index_schema_management_strategy=RECREATE}).
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
	 * Equivalent to elasticsearch "dynamic", define what to do when a document contains a field which was not declared
	 * in the index schema.
	 * <p>
	 * Possible values are:
	 * <p>
	 * <ul>
	 * <li>{@code true}: Add new fields dynamically</li>
	 * <li>{@code false}: Ignore new fields</li>
	 * <li>{@code strict}: Throw an exception</li>
	 * </ul>
	 * <p>
	 * Defaults to {@code strict}.
	 */
	public static final String DYNAMIC_MAPPING = "elasticsearch.dynamic_mapping";

	private ElasticsearchEnvironment() {
	}
}
