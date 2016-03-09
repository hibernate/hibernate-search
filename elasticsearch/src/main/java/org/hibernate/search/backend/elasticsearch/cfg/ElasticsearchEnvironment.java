/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

/**
 * Configuration properties of the ES backend.
 *
 * @author Gunnar Morling
 */
public final class ElasticsearchEnvironment {

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {
		public static final String SERVER_URI = "http://localhost:9200";
		public static final IndexManagementStrategy INDEX_MANAGEMENT_STRATEGY = IndexManagementStrategy.NONE;
		public static final int INDEX_MANAGEMENT_WAIT_TIMEOUT = 10_000;
	}

	/**
	 * Property for specifying the host name of the Elasticsearch server to connect to. Only a single server (i.e. no
	 * clusters) is supported at this point.
	 * <p>
	 * An URI such as http://myeshost.com:9200 is expected.
	 * <p>
	 * Defaults to {@link Defaults#SERVER_URI}.
	 */
	public static final String SERVER_URI = "hibernate.search.elasticsearch.host";

	/**
	 * Property for specifying the strategy for maintaining the Elasticsearch index.
	 * <p>
	 * The name of one of the {@link IndexManagementStrategy} constants is expected, e.g. MERGE.
	 * <p>
	 *
	 */
	public static final String INDEX_MANAGEMENT_STRATEGY = "hibernate.search.elasticsearch.index_management_strategy";

	/**
	 * Property for specifying the timeout for index management operations (index creation etc.) in milli-seconds.
	 * <p>
	 * A numeric value such as 1000 is expected.
	 * <p>
	 * Defaults to {@link Defaults#INDEX_MANAGEMENT_WAIT_TIMEOUT} ms.
	 */
	public static final String INDEX_MANAGEMENT_WAIT_TIMEOUT = "hibernate.search.elasticsearch.index_management_wait_timeout";

	private ElasticsearchEnvironment() {
	}
}
