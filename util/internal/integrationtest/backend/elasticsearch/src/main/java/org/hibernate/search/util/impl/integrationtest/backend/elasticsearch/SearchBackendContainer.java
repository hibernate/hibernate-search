/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		ElasticsearchDistributionName distributionName = ElasticsearchDistributionName
				.of( System.getProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.distribution", "" ) );
		String tag = System.getProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.version" );
		Path containers = Path.of( System.getProperty( "org.hibernate.search.integrationtest.container.directory", "" ) );

		try {
			DockerImageName dockerImageName = parseDockerImageName( containers.resolve( "search-backend" )
					.resolve( distributionName.externalRepresentation() + ".Dockerfile" ), tag );
			switch ( distributionName ) {
				case ELASTIC:
					SEARCH_CONTAINER = elasticsearch( dockerImageName );
					break;
				case OPENSEARCH:
				case AMAZON_OPENSEARCH_SERVERLESS:
					SEARCH_CONTAINER = opensearch( dockerImageName );
					break;
				default:
					throw new IllegalStateException( "Unknown distribution " + distributionName );
			}
		}
		catch (IOException e) {
			throw new IllegalStateException(
					"Unable to initialize a Search Engine container [" + distributionName + ", " + tag + ", " + containers
							+ "]",
					e );
		}
	}

	public static int mappedPort(int port) {
		startIfNeeded();
		return SEARCH_CONTAINER.getMappedPort( port );
	}

	public static String host() {
		startIfNeeded();
		return SEARCH_CONTAINER.getHost();
	}

	public static String connectionUrl() {
		String uris = System.getProperty( "test.elasticsearch.connection.uris" );
		if ( uris == null || uris.trim().isEmpty() ) {
			// need to start a container:
			uris = host() + ":" + mappedPort( 9200 );
		}
		return uris;
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

	private static GenericContainer<?> elasticsearch(DockerImageName dockerImageName) {
		GenericContainer<?> container = common( dockerImageName )
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

		ElasticsearchVersion version =
				ElasticsearchVersion.of( ElasticsearchDistributionName.ELASTIC, dockerImageName.getVersionPart() );

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

	private static GenericContainer<?> opensearch(DockerImageName dockerImageName) {
		GenericContainer<?> container = common( dockerImageName )
				.withEnv( "logger.level", "WARN" )
				.withEnv( "discovery.type", "single-node" )
				// Prevent swapping
				.withEnv( "bootstrap.memory_lock", "true" )
				// OpenSearch expects SSL and uses a self-signed certificate by default;
				// it's not practical for testing, so we disable that.
				.withEnv( "plugins.security.disabled", "true" )
				// Limit the RAM usage.
				.withEnv( "OPENSEARCH_JAVA_OPTS", "-Xms1g -Xmx1g" )
				// ISM floods the logs with errors, and we don't need it.
				// See https://docs-beta.opensearch.org/im-plugin/ism/settings/
				.withEnv( "plugins.index_state_management.enabled", "false" )
				// Disable disk-based shard allocation thresholds: on large, relatively full disks (>90% used),
				// it will lead to index creation to get stuck waiting for other nodes to join the cluster,
				// which will never happen since we only have one node.
				// See https://www.elastic.co/guide/en/elasticsearch/reference/7.17/modules-cluster.html#disk-based-shard-allocation
				// See https://opensearch.org/docs/latest/opensearch/popular-api/#change-disk-watermarks-or-other-cluster-settings
				.withEnv( "cluster.routing.allocation.disk.threshold_enabled", "false" );

		ElasticsearchVersion version =
				ElasticsearchVersion.of( ElasticsearchDistributionName.OPENSEARCH, dockerImageName.getVersionPart() );

		if ( version.majorOptional().orElse( Integer.MIN_VALUE ) == 2
				&& version.minor().orElse( Integer.MAX_VALUE ) > 11 ) {
			// Note: For OpenSearch 2.12 and later, a custom password for the admin user is required to be passed to set-up and utilize demo configuration.
			container.withEnv( "OPENSEARCH_INITIAL_ADMIN_PASSWORD", "NotActua11y$trongPa$$word" );
		}
		return container;
	}

	/*
	 * Suppress "Resource leak: '<unassigned Closeable value>' is never closed". Testcontainers take care of closing
	 * this resource in the end.
	 */
	@SuppressWarnings("resource")
	private static GenericContainer<?> common(DockerImageName dockerImageName) {
		return new GenericContainer<>( dockerImageName )
				.withExposedPorts( 9200, 9300 )
				.waitingFor( new HttpWaitStrategy().forPort( 9200 ).forStatusCode( 200 ) )
				.withStartupTimeout( Duration.ofMinutes( 5 ) )
				.withReuse( true );
	}


	private static DockerImageName parseDockerImageName(Path dockerfile, String tag) throws IOException {
		Pattern DOCKERFILE_FROM_LINE_PATTERN = Pattern.compile( "FROM (.+)" );

		DockerImageName dockerImageName = Files.lines( dockerfile )
				.map( DOCKERFILE_FROM_LINE_PATTERN::matcher )
				.filter( Matcher::matches )
				.map( m -> m.group( 1 ) )
				.map( DockerImageName::parse )
				.findAny().orElseThrow( () -> new IllegalStateException(
						"Dockerfile " + dockerfile + " has unexpected structure. It *must* contain a single FROM line." ) );

		if ( tag != null && !tag.trim().isEmpty() ) {
			return dockerImageName.withTag( tag );
		}

		return dockerImageName;
	}
}
