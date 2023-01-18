/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.util.common.impl.HibernateSearchConfiguration;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

/**
 * Configuration properties for Elasticsearch backends.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key;
 * see {@link org.hibernate.search.engine.cfg.BackendSettings} for details.
 *
 * @author Gunnar Morling
 */
@HibernateSearchConfiguration(
		prefix = { "hibernate.search.backend.", "hibernate.search.backends.<backend name>." },
		title = "Hibernate Search Backend - Elasticsearch",
		anchorPrefix = "hibernate-search-backend-elasticsearch-"
)
public final class ElasticsearchBackendSettings {

	private ElasticsearchBackendSettings() {
	}

	/**
	 * The name to use for the {@link org.hibernate.search.engine.cfg.BackendSettings#TYPE backend type}
	 * configuration property so that an Elasticsearch backend is instantiated by Hibernate Search.
	 * <p>
	 * Only useful if you have more than one backend technology in the classpath;
	 * otherwise the backend type is automatically detected.
	 */
	@HibernateSearchConfiguration(ignore = true)
	public static final String TYPE_NAME = "elasticsearch";

	/**
	 * The host name and ports of the Elasticsearch servers to connect to.
	 * <p>
	 * Expects a String representing a host and port such as {@code localhost} or {@code es.mycompany.com:4400},
	 * or a String containing multiple such host-and-port strings separated by commas,
	 * or a {@code Collection<String>} containing such host-and-port strings.
	 * <p>
	 * Cannot be used if {@link #URIS} is set.
	 * <p>
	 * Defaults to {@link Defaults#HOSTS}.
	 * <p>
	 * Multiple servers may be specified for load-balancing: requests will be assigned to each host in turns.
	 * <p>
	 * This property is ignored when {@value #CLIENT_INSTANCE} is set.
	 */
	public static final String HOSTS = "hosts";

	/**
	 * The protocol to use when connecting to the Elasticsearch servers.
	 * <p>
	 * Expects a String: either {@code http} or {@code https}.
	 * <p>
	 * Cannot be used if {@link #URIS} is set.
	 * <p>
	 * Defaults to {@link Defaults#PROTOCOL}.
	 * <p>
	 * This property is ignored when {@value #CLIENT_INSTANCE} is set.
	 */
	public static final String PROTOCOL = "protocol";

	/**
	 * Alternatively to {@link #HOSTS} and {@link #PROTOCOL},
	 * it is possible to define both the protocol and hosts as one or more URIs using this property.
	 * <p>
	 * Expects either a String representing an URI such as {@code http://localhost}
	 * or {@code https://es.mycompany.com:4400},
	 * or a String containing multiple such URIs separated by commas,
	 * or a {@code Collection<String>} containing such URIs.
	 * <p>
	 * All the URIs must have the same protocol.
	 * Cannot be used if {@link #HOSTS} or {@link #PROTOCOL} are set.
	 * <p>
	 * Defaults to {@code http://localhost:9200}, unless {@link #HOSTS} or {@link #PROTOCOL} are set, in which case they take precedence.
	 * <p>
	 * This property is ignored when {@value #CLIENT_INSTANCE} is set.
	 */
	public static final String URIS = "uris";

	/**
	 * Property for specifying the path prefix prepended to the request end point.
	 * Use the path prefix if your Elasticsearch instance is located at a specific context path.
	 * <p>
	 * Defaults to {@link Defaults#PATH_PREFIX}.
	 * <p>
	 * This property is ignored when {@value #CLIENT_INSTANCE} is set.
	 */
	public static final String PATH_PREFIX = "path_prefix";

	/**
	 * The version of Elasticsearch running on the Elasticsearch cluster.
	 * <p>
	 * Expects either an {@link ElasticsearchVersion} object,
	 * or a String that can be {{@link ElasticsearchVersion#of(String) parsed} in such an object.
	 * <p>
	 * No default: if not provided, the version will be resolved automatically
	 * by sending a request to the Elasticsearch cluster on startup.
	 */
	public static final String VERSION = "version";

	/**
	 * Whether check version of the Elasticsearch cluster is enabled.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#VERSION_CHECK_ENABLED}.
	 */
	public static final String VERSION_CHECK_ENABLED = "version_check.enabled";

	/**
	 * The username to send when connecting to the Elasticsearch servers (HTTP authentication).
	 * <p>
	 * Expects a String.
	 * <p>
	 * Defaults to no username (anonymous access).
	 * <p>
	 * This property is ignored when {@value #CLIENT_INSTANCE} is set.
	 */
	public static final String USERNAME = "username";

	/**
	 * The password to send when connecting to the Elasticsearch servers (HTTP authentication).
	 * <p>
	 * Expects a String.
	 * <p>
	 * Defaults to no username (anonymous access).
	 * <p>
	 * This property is ignored when {@value #CLIENT_INSTANCE} is set.
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
	 * <p>
	 * This property is ignored when {@value #CLIENT_INSTANCE} is set.
	 */
	public static final String REQUEST_TIMEOUT = "request_timeout";

	/**
	 * The timeout when reading responses from an Elasticsearch server.
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 60000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#READ_TIMEOUT}.
	 * <p>
	 * This property is ignored when {@value #CLIENT_INSTANCE} is set.
	 */
	public static final String READ_TIMEOUT = "read_timeout";

	/**
	 * The timeout when establishing a connection to an Elasticsearch server.
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 3000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#CONNECTION_TIMEOUT}.
	 * <p>
	 * This property is ignored when {@value #CLIENT_INSTANCE} is set.
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
	 * <p>
	 * This property is ignored when {@value #CLIENT_INSTANCE} is set.
	 */
	public static final String MAX_CONNECTIONS = "max_connections";

	/**
	 * The maximum number of simultaneous connections to each host of the Elasticsearch cluster.
	 * <p>
	 * Expects a positive Integer value, such as {@code 10},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#MAX_CONNECTIONS_PER_ROUTE}.
	 * <p>
	 * This property is ignored when {@value #CLIENT_INSTANCE} is set.
	 */
	public static final String MAX_CONNECTIONS_PER_ROUTE = "max_connections_per_route";

	/**
	 * Whether automatic discovery of nodes in the Elasticsearch cluster is enabled.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#DISCOVERY_ENABLED}.
	 * <p>
	 * This property is ignored when {@value #CLIENT_INSTANCE} is set.
	 */
	public static final String DISCOVERY_ENABLED = "discovery.enabled";

	/**
	 * The time interval between two executions of the automatic discovery, if enabled.
	 * <p>
	 * Expects a positive Integer value in seconds, such as {@code 2},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#DISCOVERY_REFRESH_INTERVAL}.
	 * <p>
	 * This property is ignored when {@value #CLIENT_INSTANCE} is set.
	 */
	public static final String DISCOVERY_REFRESH_INTERVAL = "discovery.refresh_interval";

	/**
	 * Allows to define a {@link ElasticsearchHttpClientConfigurer},
	 * that can be used for instance to set custom HTTP client configurations,
	 * using an instance of {@link HttpAsyncClientBuilder}.
	 * <p>
	 * It can be used for example to tune the SSL context to accept self-signed certificates.
	 * It allows to override other HTTP client settings, such as {@link #USERNAME} or {@link #MAX_CONNECTIONS_PER_ROUTE}.
	 * <p>
	 * Expects a reference to a bean of type {@link ElasticsearchHttpClientConfigurer}.
	 * <p>
	 * Defaults to no value.
	 * <p>
	 * This property is ignored when {@value #CLIENT_INSTANCE} is set.
	 */
	public static final String CLIENT_CONFIGURER = "client.configurer";

	/**
	 * A external Elasticsearch client instance that Hibernate Search should use for all requests to Elasticsearch.
	 * <p>
	 * If this is set, Hibernate Search will not attempt to create its own Elasticsearch,
	 * and all other client-related configuration properties
	 * (hosts/uris, authentication, discovery, timeouts, max connections, configurer, ...)
	 * will be ignored.
	 * <p>
	 * Expects a reference to a bean of type {@link org.elasticsearch.client.RestClient}.
	 * <p>
	 * Defaults to nothing: if no client instance is provided, Hibernate Search will create its own.
	 * <p>
	 * <strong>WARNING - Incubating API:</strong> the underlying client class may change without notice.
	 *
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "bean reference" properties and accepted values.
	 */
	public static final String CLIENT_INSTANCE = "client.instance";

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
	public static final String MULTI_TENANCY_STRATEGY = "multi_tenancy.strategy";

	/**
	 * The strategy for mapping documents to their type name,
	 * i.e. to determine the type name of a document in search hits.
	 * <p>
	 * Expects a {@link TypeNameMappingStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#MAPPING_TYPE_NAME_STRATEGY}.
	 */
	public static final String MAPPING_TYPE_NAME_STRATEGY = "mapping.type_name.strategy";

	/**
	 * The layout strategy for indexes and their aliases.
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
	 * or a string that can be parsed to such integer value.
	 * <p>
	 * Defaults to the number of processor cores available to the JVM on startup.
	 * <p>
	 * See the reference documentation, section "Elasticsearch backend - Threads",
	 * for more information about this setting and its implications.
	 */
	public static final String THREAD_POOL_SIZE = "thread_pool.size";

	/**
	 * Property for specifying the maximum duration a {@code Scroll} will be usable if no
	 * other results are fetched from Elasticsearch.
	 * <p>
	 * Expects a positive Integer value in seconds, such as 60,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#SCROLL_TIMEOUT}.
	 */
	public static final String SCROLL_TIMEOUT = "scroll_timeout";

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
	}
}
