/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch;

import java.time.Duration;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

public final class SearchBackendContainer {

	private SearchBackendContainer() {
	}

	private static final GenericContainer<?> SEARCH_CONTAINER;

	static {
		String name = System.getProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.name", "" );
		String tag = System.getProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.tag" );

		SEARCH_CONTAINER = name.contains( "elastic" )
				? elasticsearch( name, tag, ElasticsearchVersion.of( ElasticsearchDistributionName.ELASTIC, tag ) )
				: opensearch( name, tag );
	}

	public static int mappedPort(int port) {
		startIfNeeded();
		return SEARCH_CONTAINER.getMappedPort( port );
	}

	public static String host() {
		startIfNeeded();
		return SEARCH_CONTAINER.getHost();
	}

	private static void startIfNeeded() {
		if ( !SEARCH_CONTAINER.isRunning() ) {
			synchronized (SEARCH_CONTAINER) {
				if ( !SEARCH_CONTAINER.isRunning() ) {
					SEARCH_CONTAINER.start();
				}
			}
		}
	}

	private static GenericContainer<?> elasticsearch(String name, String tag, ElasticsearchVersion version) {
		GenericContainer<?> container = common( name, tag )
				.withEnv( "logger.level", "WARN" )
				.withEnv( "discovery.type", "single-node" )
				// Older images require HTTP authentication for all requests;
				// it's not practical for testing, so we disable that:
				.withEnv( "xpack.security.enabled", "false" )
				// Limit the RAM usage.
				// Recent versions of ES limit themselves to 50% of the total available RAM,
				// but on CI this is sometimes too much, as we also have the Maven JVM
				// and the JVM that runs tests taking up a significant amount of RAM,
				// leaving too little for filesystem caches and resulting in freezes:
				.withEnv( "ES_JAVA_OPTS", "-Xms1g -Xmx1g" )
				// Disable disk-based shard allocation thresholds: on large, relatively full disks (>90% used),
				// it will lead to index creation to get stuck waiting for other nodes to join the cluster,
				// which will never happen since we only have one node.
				// See https://www.elastic.co/guide/en/elasticsearch/reference/7.17/modules-cluster.html#disk-based-shard-allocation
				.withEnv( "cluster.routing.allocation.disk.threshold_enabled", "false" );

		// Disable a few features that we don't use and that just slow up container startup.
		if ( version.majorOptional().orElse( Integer.MIN_VALUE ) == 8 ) {
			if ( version.minor().orElse( Integer.MAX_VALUE ) > 7 ) {
				container.withEnv( "cluster.deprecation_indexing.enabled", "false" )
						.withEnv( "xpack.profiling.enabled", "false" )
						.withEnv( "xpack.ent_search.enabled", "false" );
			}
			return container.withEnv( "cluster.deprecation_indexing.enabled", "false" )
					.withEnv( "indices.lifecycle.history_index_enabled", "false" )
					.withEnv( "slm.history_index_enabled", "false" )
					.withEnv( "stack.templates.enabled", "false" )
					.withEnv( "xpack.ml.enabled", "false" )
					.withEnv( "xpack.monitoring.templates.enabled", "false" )
					.withEnv( "xpack.watcher.enabled", "false" );
		}

		if ( version.majorOptional().orElse( Integer.MIN_VALUE ) == 7 ) {
			if ( version.minor().orElse( Integer.MAX_VALUE ) > 15 ) {
				container.withEnv( "cluster.deprecation_indexing.enabled", "false" )
						.withEnv( "slm.history_index_enabled", "false" );
			}
			return container.withEnv( "indices.lifecycle.history_index_enabled", "false" )
					.withEnv( "stack.templates.enabled", "false" )
					.withEnv( "xpack.ml.enabled", "false" )
					.withEnv( "xpack.watcher.enabled", "false" );
		}

		return container;
	}

	private static GenericContainer<?> opensearch(String name, String tag) {
		return common( name, tag )
				.withEnv( "logger.level", "WARN" )
				.withEnv( "discovery.type", "single-node" )
				// Prevent swapping
				.withEnv( "bootstrap.memory_lock", "true" )
				// OpenSearch expects SSL and uses a self-signed certificate by default;
				// it's not practical for testing, so we disable that.
				.withEnv( "plugins.security.disabled", "true" )
				// ISM floods the logs with errors, and we don't need it.
				// See https://docs-beta.opensearch.org/im-plugin/ism/settings/
				.withEnv( "plugins.index_state_management.enabled", "false" )
				// Disable disk-based shard allocation thresholds: on large, relatively full disks (>90% used),
				// it will lead to index creation to get stuck waiting for other nodes to join the cluster,
				// which will never happen since we only have one node.
				// See https://www.elastic.co/guide/en/elasticsearch/reference/7.17/modules-cluster.html#disk-based-shard-allocation
				// See https://opensearch.org/docs/latest/opensearch/popular-api/#change-disk-watermarks-or-other-cluster-settings
				.withEnv( "cluster.routing.allocation.disk.threshold_enabled", "false" );
	}

	/*
	 * Suppress "Resource leak: '<unassigned Closeable value>' is never closed". Testcontainers take care of closing
	 * this resource in the end.
	 */
	@SuppressWarnings("resource")
	private static GenericContainer<?> common(String image, String tag) {
		return new GenericContainer<>( DockerImageName.parse( image ).withTag( tag ) )
				.withExposedPorts( 9200, 9300 )
				.waitingFor( new HttpWaitStrategy().forPort( 9200 ).forStatusCode( 200 ) )
				.withStartupTimeout( Duration.ofMinutes( 5 ) )
				.withReuse( true );
	}
}
