/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.rest4.cfg;

import org.hibernate.search.backend.elasticsearch.client.rest4.ElasticsearchHttpClientConfigurer;

/**
 * Specific configuration properties for the Elasticsearch backend's rest client based on the Elasticsearch's low-level rest client.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key;
 * see {@link org.hibernate.search.engine.cfg.BackendSettings} for details.
 *
 * @author Gunnar Morling
 */
public final class ClientRest4ElasticsearchBackendClientSettings {

	private ClientRest4ElasticsearchBackendClientSettings() {
	}

	/**
	 * The timeout when executing a request to an Elasticsearch server.
	 * <p>
	 * This includes the time needed to establish a connection, send the request and read the response.
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as 60000,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to no request timeout.
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
	 * Expects a positive Integer value, such as {@code 40},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#MAX_CONNECTIONS}.
	 */
	public static final String MAX_CONNECTIONS = "max_connections";

	/**
	 * The maximum number of simultaneous connections to each host of the Elasticsearch cluster.
	 * <p>
	 * Expects a positive Integer value, such as {@code 20},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#MAX_CONNECTIONS_PER_ROUTE}.
	 */
	public static final String MAX_CONNECTIONS_PER_ROUTE = "max_connections_per_route";

	/**
	 * Whether automatic discovery of nodes in the Elasticsearch cluster is enabled.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed into a Boolean value.
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
	 * How long connections to the Elasticsearch cluster can be kept idle.
	 * <p>
	 * Expects a positive Long value of milliseconds, such as 60000,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * If the response from an Elasticsearch cluster contains a {@code Keep-Alive} header,
	 * then the effective max idle time will be whichever is lower:
	 * the duration from the {@code Keep-Alive} header or the value of this property (if set).
	 * <p>
	 * If this property is not set, only the {@code Keep-Alive} header is considered,
	 * and if it's absent, idle connections will be kept forever.
	 */
	public static final String MAX_KEEP_ALIVE = "max_keep_alive";

	/**
	* A {@link ElasticsearchHttpClientConfigurer} that defines custom HTTP client configuration.
	* <p>
	* It can be used for example to tune the SSL context to accept self-signed certificates.
	* It allows overriding other HTTP client settings, such as {@code USERNAME} or {@link #MAX_CONNECTIONS_PER_ROUTE}.
	* <p>
	* Expects a reference to a bean of type {@link ElasticsearchHttpClientConfigurer}.
	* <p>
	* Defaults to no value.
	*/
	public static final String CLIENT_CONFIGURER = "client.configurer";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final int READ_TIMEOUT = 30000;
		public static final int CONNECTION_TIMEOUT = 1000;
		public static final int MAX_CONNECTIONS = 40;
		public static final int MAX_CONNECTIONS_PER_ROUTE = 20;
		public static final boolean DISCOVERY_ENABLED = false;
		public static final int DISCOVERY_REFRESH_INTERVAL = 10;
	}
}
