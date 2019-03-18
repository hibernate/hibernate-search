/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

/**
 * Configuration properties for Elasticsearch backends.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key;
 * see {@link org.hibernate.search.engine.cfg.BackendSettings} for details.
 *
 * @author Gunnar Morling
 */
public final class ElasticsearchBackendSettings {

	private ElasticsearchBackendSettings() {
	}

	/**
	 * The name to use for the {@link org.hibernate.search.engine.cfg.BackendSettings#TYPE backend type}
	 * configuration property so that an Elasticsearch backend is instantiated by Hibernate Search.
	 */
	public static final String TYPE_NAME = "elasticsearch";

	/**
	 * The protocol, host name and HTTP ports of the Elasticsearch servers to connect to.
	 * <p>
	 * Expects either a String representing an URI such as {@code http://localhost}
	 * or {@code https://es.mycompany.com:4400},
	 * or a String containing multiple such URIs separated by whitespace characters,
	 * or a {@code Collection<String>} containing such URIs.
	 * <p>
	 * Defaults to {@link Defaults#HOSTS}.
	 * <p>
	 * Multiple servers may be specified for load-balancing: requests will be assigned to each host in turns.
	 */
	public static final String HOSTS = "hosts";

	/**
	 * The dialect to use when interacting with the remote Elasticseearch cluster.
	 * <p>
	 * See {@link ElasticsearchDialectName} for accepted values.
	 * <p>
	 * Defaults to {@link ElasticsearchDialectName#AUTO}.
	 */
	public static final String DIALECT = "dialect";

	/**
	 * The username to send when connecting to the Elasticsearch servers (HTTP authentication).
	 * <p>
	 * Expects a String.
	 * <p>
	 * Defaults to no username (anonymous access).
	 */
	public static final String USERNAME = "username";

	/**
	 * The password to send when connecting to the Elasticsearch servers (HTTP authentication).
	 * <p>
	 * Expects a String.
	 * <p>
	 * Defaults to no username (anonymous access).
	 */
	public static final String PASSWORD = "password";

	/**
	 * The timeout when executing a request to an Elasticsearch server.
	 * <p>
	 * This includes the time needed to establish a connection, send the request and read the response.
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as 60000,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#REQUEST_TIMEOUT}.
	 */
	public static final String REQUEST_TIMEOUT = "request_timeout";

	/**
	 * The timeout when reading responses from an Elasticsearch server.
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 60000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#READ_TIMEOUT}.
	 */
	public static final String READ_TIMEOUT = "read_timeout";

	/**
	 * The timeout when establishing a connection to an Elasticsearch server.
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 3000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#CONNECTION_TIMEOUT}.
	 */
	public static final String CONNECTION_TIMEOUT = "connection_timeout";

	/**
	 * The maximum number of simultaneous connections to the Elasticsearch cluster,
	 * all hosts taken together.
	 * <p>
	 * Expects a positive Integer value, such as {@code 20},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#MAX_CONNECTIONS}.
	 */
	public static final String MAX_CONNECTIONS = "max_connections";

	/**
	 * The maximum number of simultaneous connections to each host of the Elasticsearch cluster.
	 * <p>
	 * Expects a positive Integer value, such as {@code 10},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#MAX_CONNECTIONS_PER_ROUTE}.
	 */
	public static final String MAX_CONNECTIONS_PER_ROUTE = "max_connections_per_route";

	/**
	 * Whether automatic discovery of nodes in the Elasticsearch cluster is enabled.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#DISCOVERY_ENABLED}.
	 */
	public static final String DISCOVERY_ENABLED = "discovery.enabled";

	/**
	 * The time interval between two executions of the automatic discovery, if enabled.
	 * <p>
	 * Expects a positive Integer value in seconds, such as {@code 2},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#DISCOVERY_REFRESH_INTERVAL}.
	 */
	public static final String DISCOVERY_REFRESH_INTERVAL = "discovery.refresh_interval";

	/**
	 * The default scheme to use when connecting to automatically discovered nodes.
	 * <p>
	 * Expects a String: either "http" or "https".
	 * <p>
	 * Defaults to {@link Defaults#DISCOVERY_SCHEME}.
	 */
	public static final String DISCOVERY_SCHEME = "discovery.default_scheme";

	/**
	 * Whether JSON included in logs should be pretty-printed (indented, with line breaks).
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#LOG_JSON_PRETTY_PRINTING}.
	 */
	public static final String LOG_JSON_PRETTY_PRINTING = "log.json_pretty_printing";

	/**
	 * The multi-tenancy strategy to use.
	 * <p>
	 * Expects a {@link MultiTenancyStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#MULTI_TENANCY_STRATEGY}.
	 */
	public static final String MULTI_TENANCY_STRATEGY = "multi_tenancy_strategy";

	/**
	 * The analysis configurer to use.
	 * <p>
	 * Expects a reference to a bean of type {@link ElasticsearchAnalysisConfigurer}.
	 * <p>
	 * Defaults to no value.
	 *
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "bean reference" properties and accepted values.
	 */
	public static final String ANALYSIS_CONFIGURER = "analysis_configurer";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final List<String> HOSTS = Collections.singletonList( "http://localhost:9200" );
		public static final ElasticsearchDialectName DIALECT = ElasticsearchDialectName.AUTO;
		public static final int REQUEST_TIMEOUT = 60000;
		public static final int READ_TIMEOUT = 60000;
		public static final int CONNECTION_TIMEOUT = 3000;
		public static final int MAX_CONNECTIONS = 20;
		public static final int MAX_CONNECTIONS_PER_ROUTE = 10;
		public static final boolean DISCOVERY_ENABLED = false;
		public static final int DISCOVERY_REFRESH_INTERVAL = 10;
		public static final String DISCOVERY_SCHEME = "http";
		public static final boolean LOG_JSON_PRETTY_PRINTING = false;
		public static final MultiTenancyStrategyName MULTI_TENANCY_STRATEGY = MultiTenancyStrategyName.NONE;
	}
}
