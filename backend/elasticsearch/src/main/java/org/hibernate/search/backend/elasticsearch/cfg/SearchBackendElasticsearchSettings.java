/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

import java.util.Collections;
import java.util.List;

/**
 * Configuration properties for Elasticsearch,
 *
 * @author Gunnar Morling
 */
public final class SearchBackendElasticsearchSettings {

	private SearchBackendElasticsearchSettings() {
	}

	public static final String HOST = "host";

	public static final String USERNAME = "username";

	public static final String PASSWORD = "password";

	public static final String REQUEST_TIMEOUT = "request_timeout";

	public static final String READ_TIMEOUT = "read_timeout";

	public static final String CONNECTION_TIMEOUT = "connection_timeout";

	public static final String MAX_TOTAL_CONNECTION = "max_total_connection";

	public static final String MAX_TOTAL_CONNECTION_PER_ROUTE = "max_total_connection_per_route";

	public static final String DISCOVERY_ENABLED = "discovery.enabled";

	public static final String DISCOVERY_REFRESH_INTERVAL = "discovery.refresh_interval";

	public static final String DISCOVERY_SCHEME = "discovery.default_scheme";

	public static final String LOG_JSON_PRETTY_PRINTING = "log.json_pretty_printing";

	public static final String MULTI_TENANCY_STRATEGY = "multi_tenancy_strategy";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final List<String> HOST = Collections.singletonList( "http://localhost:9200" );
		public static final int REQUEST_TIMEOUT = 60000;
		public static final int READ_TIMEOUT = 60000;
		public static final int CONNECTION_TIMEOUT = 3000;
		public static final int MAX_TOTAL_CONNECTION = 20;
		public static final int MAX_TOTAL_CONNECTION_PER_ROUTE = 2;
		public static final boolean DISCOVERY_ENABLED = false;
		public static final int DISCOVERY_REFRESH_INTERVAL = 10;
		public static final String DISCOVERY_SCHEME = "http";
		public static final boolean LOG_JSON_PRETTY_PRINTING = false;
		public static final MultiTenancyStrategyConfiguration MULTI_TENANCY_STRATEGY = MultiTenancyStrategyConfiguration.NONE;
	}
}
