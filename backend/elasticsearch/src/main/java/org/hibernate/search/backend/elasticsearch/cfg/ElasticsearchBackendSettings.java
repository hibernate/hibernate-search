/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.cfg;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.SimpleIndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.mapping.TypeNameMappingStrategyName;
import org.hibernate.search.backend.elasticsearch.multitenancy.MultiTenancyStrategyName;
import org.hibernate.search.engine.environment.bean.BeanReference;

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
	 * The value to set the {@link org.hibernate.search.engine.cfg.BackendSettings#TYPE backend type}
	 * configuration property
	 * in order to get an Elasticsearch backend instantiated by Hibernate Search.
	 * <p>
	 * Only useful if you have more than one backend technology in the classpath;
	 * otherwise the backend type is automatically detected.
	 */
	public static final String TYPE_NAME = "elasticsearch";

	/**
	 * The hostname and ports of the Elasticsearch servers to connect to.
	 * <p>
	 * Expects a String representing a hostname and port such as {@code localhost} or {@code es.mycompany.com:4400},
	 * or a String containing multiple such hostname-and-port strings separated by commas,
	 * or a {@code Collection<String>} containing such hostname-and-port strings.
	 * <p>
	 * Multiple servers may be specified for load-balancing: requests will be assigned to each host in turns.
	 * <p>
	 * Setting this property at the same time as {@link #URIS} will lead to an exception being thrown on startup.
	 * <p>
	 * Defaults to {@link Defaults#HOSTS}.
	 */
	public static final String HOSTS = "hosts";

	/**
	 * The protocol to use when connecting to the Elasticsearch servers.
	 * <p>
	 * Expects a String: either {@code http} or {@code https}.
	 * <p>
	 * Setting this property at the same time as {@link #URIS} will lead to an exception being thrown on startup.
	 * <p>
	 * Defaults to {@link Defaults#PROTOCOL}.
	 */
	public static final String PROTOCOL = "protocol";

	/**
	 * The protocol, hostname and ports of the Elasticsearch servers to connect to.
	 * <p>
	 * Expects either a String representing an URI such as {@code http://localhost}
	 * or {@code https://es.mycompany.com:4400},
	 * or a String containing multiple such URIs separated by commas,
	 * or a {@code Collection<String>} containing such URIs.
	 * <p>
	 * All the URIs must specify the same protocol.
	 * <p>
	 * Setting this property at the same time as {@link #HOSTS} or {@link #PROTOCOL} will lead to an exception being thrown on startup.
	 * <p>
	 * Defaults to {@code http://localhost:9200}, unless {@link #HOSTS} or {@link #PROTOCOL} are set, in which case they take precedence.
	 */
	public static final String URIS = "uris";

	/**
	 * Property for specifying the path prefix prepended to the request end point.
	 * Use the path prefix if your Elasticsearch instance is located at a specific context path.
	 * <p>
	 * Defaults to {@link Defaults#PATH_PREFIX}.
	 */
	public static final String PATH_PREFIX = "path_prefix";

	/**
	 * The version of Elasticsearch running on the Elasticsearch cluster.
	 * <p>
	 * Expects either an {@link ElasticsearchVersion} object,
	 * or a String that can be {@link ElasticsearchVersion#of(String) parsed} in such an object.
	 * <p>
	 * No default: if not provided, the version will be resolved automatically
	 * by sending a request to the Elasticsearch cluster on startup.
	 */
	public static final String VERSION = "version";

	/**
	 * Whether check version of the Elasticsearch cluster is enabled.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed into a Boolean value.
	 * <p>
	 * Defaults to {@code true} when the {@link #VERSION} is unconfigured
	 * or set to a distribution that supports version checking,
	 * and to {@code false} when the {@link #VERSION} is set
	 * to a distribution that does not support version checking (like Amazon OpenSearch Serverless).
	 */
	public static final String VERSION_CHECK_ENABLED = "version_check.enabled";

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
	 * A {@link ElasticsearchHttpClientConfigurer} that defines custom HTTP client configuration.
	 * <p>
	 * It can be used for example to tune the SSL context to accept self-signed certificates.
	 * It allows overriding other HTTP client settings, such as {@link #USERNAME} or {@link #MAX_CONNECTIONS_PER_ROUTE}.
	 * <p>
	 * Expects a reference to a bean of type {@link ElasticsearchHttpClientConfigurer}.
	 * <p>
	 * Defaults to no value.
	 */
	public static final String CLIENT_CONFIGURER = "client.configurer";

	/**
	 * Whether JSON included in logs should be pretty-printed (indented, with line breaks).
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed into a Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#LOG_JSON_PRETTY_PRINTING}.
	 */
	public static final String LOG_JSON_PRETTY_PRINTING = "log.json_pretty_printing";

	/**
	 * How to implement multi-tenancy.
	 * <p>
	 * Expects a {@link MultiTenancyStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#MULTI_TENANCY_STRATEGY}.
	 */
	public static final String MULTI_TENANCY_STRATEGY = "multi_tenancy.strategy";

	/**
	 * How to map documents to their type name,
	 * i.e. how to determine the type name of a document in search hits.
	 * <p>
	 * Expects a {@link TypeNameMappingStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#MAPPING_TYPE_NAME_STRATEGY}.
	 */
	public static final String MAPPING_TYPE_NAME_STRATEGY = "mapping.type_name.strategy";

	/**
	 * How to determine index names and aliases.
	 * <p>
	 * Expects a reference to a bean of type {@link IndexLayoutStrategy}.
	 * <p>
	 * Defaults to the {@code simple} strategy:
	 * <ul>
	 *     <li>The non-alias name follows the format {@code <hibernateSearchIndexName>-<6 digits>}</li>
	 *     <li>The write alias follows the format {@code <hibernateSearchIndexName>-write}</li>
	 *     <li>The read alias follows the format {@code <hibernateSearchIndexName>-read}</li>
	 * </ul>
	 *
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "bean reference" properties and accepted values.
	 */
	public static final String LAYOUT_STRATEGY = "layout.strategy";

	/**
	 * The size of the thread pool assigned to the backend.
	 * <p>
	 * Expects a strictly positive integer value,
	 * or a string that can be parsed into an integer value.
	 * <p>
	 * See the reference documentation, section "Elasticsearch backend - Threads",
	 * for more information about this setting and its implications.
	 * <p>
	 * Defaults to the number of processor cores available to the JVM on startup.
	 */
	public static final String THREAD_POOL_SIZE = "thread_pool.size";

	/**
	 * Property for specifying the maximum duration a
	 * {@link org.hibernate.search.engine.search.query.SearchFetchable#scroll(int) scroll} will be usable if no
	 * other results are fetched from Elasticsearch.
	 * <p>
	 * Expects a positive Integer value in seconds, such as 60,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#SCROLL_TIMEOUT}.
	 */
	public static final String SCROLL_TIMEOUT = "scroll_timeout";

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
	 * This property defines if partial shard failures are ignored.
	 * <p>
	 * In case all shards fail, Elasticsearch cluster will return a 400 status code itself,
	 * but if only some of the shards fail, then the client will receive a successful partial response from the shards
	 * that were successful.
	 * <p>
	 * To prevent getting any partial results this setting can be set to {@code false}.
	 * While if the partial failures should be ignored and considered as valid results then the value should be set to {@code true}.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed into a Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#QUERY_SHARD_FAILURE_IGNORE}.
	 */
	public static final String QUERY_SHARD_FAILURE_IGNORE = "query.shard_failure.ignore";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final List<String> HOSTS = Collections.singletonList( "localhost:9200" );
		public static final String PROTOCOL = "http";
		public static final String PATH_PREFIX = "";
		public static final int READ_TIMEOUT = 30000;
		public static final int CONNECTION_TIMEOUT = 1000;
		public static final int MAX_CONNECTIONS = 20;
		public static final int MAX_CONNECTIONS_PER_ROUTE = 10;
		public static final boolean DISCOVERY_ENABLED = false;
		public static final int DISCOVERY_REFRESH_INTERVAL = 10;
		public static final boolean LOG_JSON_PRETTY_PRINTING = false;
		/**
		 * @deprecated The default for the {@link ElasticsearchBackendSettings#VERSION_CHECK_ENABLED} property
		 * is now dynamic and depends on the value of the {@link ElasticsearchBackendSettings#VERSION} property.
		 * @see ElasticsearchBackendSettings#VERSION_CHECK_ENABLED
		 */
		@Deprecated
		public static final boolean VERSION_CHECK_ENABLED = true;

		/**
		 * @deprecated The default for this property is now dynamic and depends on the mapper.
		 * If the multi-tenancy is enabled in the mapper, the default is {@link MultiTenancyStrategyName#DISCRIMINATOR}.
		 * Otherwise, the default is still {@link MultiTenancyStrategyName#NONE}.
		 */
		@Deprecated
		public static final MultiTenancyStrategyName MULTI_TENANCY_STRATEGY = MultiTenancyStrategyName.NONE;

		public static final TypeNameMappingStrategyName MAPPING_TYPE_NAME_STRATEGY = TypeNameMappingStrategyName.DISCRIMINATOR;
		public static final BeanReference<IndexLayoutStrategy> LAYOUT_STRATEGY =
				BeanReference.of( IndexLayoutStrategy.class, SimpleIndexLayoutStrategy.NAME );
		public static final int SCROLL_TIMEOUT = 60;
		public static final boolean QUERY_SHARD_FAILURE_IGNORE = false;
	}
}
