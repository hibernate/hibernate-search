/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.util.impl.integrationtest.common.TestContainerLock;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

public final class SearchBackendContainer {

	private SearchBackendContainer() {
	}

	private static final List<GenericContainer<?>> SEARCH_CONTAINERS;
	private static final int CONTAINER_COUNT;
	private static final Path CONTAINER_LOCK_FILE;

	static {
		ElasticsearchDistributionName distributionName = ElasticsearchDistributionName
				.of( System.getProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.distribution", "" ) );
		String tag = System.getProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.version" );
		Path containers = Path.of( System.getProperty( "org.hibernate.search.integrationtest.container.directory", "" ) );

		CONTAINER_COUNT = Integer.parseInt(
				System.getProperty( "test.parallel.elasticsearch.container.count", "1" ) );
		CONTAINER_LOCK_FILE = containers.getParent().getParent().resolve( "target" )
				.resolve( "hs-test-es.lock" );

		try {
			DockerImageNameAndVersion dockerImageName = parseDockerImageName( containers.resolve( "search-backend" )
					.resolve( distributionName.externalRepresentation() + ".Dockerfile" ), tag );

			List<GenericContainer<?>> list = new ArrayList<>( CONTAINER_COUNT );
			for ( int i = 0; i < CONTAINER_COUNT; i++ ) {
				GenericContainer<?> container = switch ( distributionName ) {
					case ELASTIC -> elasticsearch( dockerImageName );
					case OPENSEARCH, AMAZON_OPENSEARCH_SERVERLESS -> opensearch( dockerImageName );
				};
				container.withLabel( "hs-es-instance", String.valueOf( i ) );
				list.add( container );
			}
			SEARCH_CONTAINERS = List.copyOf( list );
		}
		catch (IOException e) {
			throw new IllegalStateException(
					"Unable to initialize Search Engine containers [" + distributionName + ", " + tag + ", " + containers
							+ "]",
					e );
		}
	}

	public static String connectionUrl() {
		String uris = System.getProperty( "test.elasticsearch.connection.uris" );
		if ( uris == null || uris.trim().isEmpty() ) {
			GenericContainer<?> container = containerForCurrentFork();
			startIfNeeded();
			return container.getHost() + ":" + container.getMappedPort( 9200 );
		}
		return uris;
	}

	private static GenericContainer<?> containerForCurrentFork() {
		String forkNumber = System.getProperty( "test.parallel.fork.number",
				System.getProperty( "surefire.forkNumber", "" ) );
		int index = 0;
		try {
			index = ( Integer.parseInt( forkNumber ) - 1 ) % CONTAINER_COUNT;
		}
		catch (NumberFormatException e) {
			// single-fork mode, use first container
		}
		return SEARCH_CONTAINERS.get( index );
	}

	private static void startIfNeeded() {
		for ( GenericContainer<?> container : SEARCH_CONTAINERS ) {
			if ( !container.isRunning() ) {
				TestContainerLock.startContainersWithLock(
						SEARCH_CONTAINERS, CONTAINER_LOCK_FILE,
						SearchBackendContainer::applyTestIndexDefaults
				);
				return;
			}
		}
	}

	private static void applyTestIndexDefaults(GenericContainer<?> container) {
		String url = "http://" + container.getHost()
				+ ":" + container.getMappedPort( 9200 )
				+ "/_index_template/test-optimization-defaults";
		String body = """
				{
				"index_patterns": ["*"],
				"priority": 0,
				"template": {
					"settings": {
					"index.translog.durability": "async",
					"index.translog.sync_interval": "120s",
					"number_of_replicas": 0
					}
				}
				}
				""";
		HttpClient client = HttpClient.newHttpClient();

		// to let jdk 17 pass but actually close things on 21+
		// need to cast to Object because of EJC
		try ( var c = ( (Object) client ) instanceof AutoCloseable ac ? ac : null ) {
			HttpRequest request = HttpRequest.newBuilder()
					.uri( URI.create( url ) )
					.header( "Content-Type", "application/json" )
					.PUT( HttpRequest.BodyPublishers.ofString( body ) )
					.build();
			HttpResponse<String> response = client.send( request, HttpResponse.BodyHandlers.ofString() );

			if ( response.statusCode() >= 300 ) {
				throw new IllegalStateException(
						"Failed to apply test index template: HTTP " + response.statusCode() + " - " + response.body() );
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException( "Interrupted while waiting for a response from a search backend.", e );
		}
		catch (Exception e) {
			throw new IllegalStateException( "Failed to apply test index template", e );
		}
	}

	private static GenericContainer<?> elasticsearch(DockerImageNameAndVersion dockerImageName) {
		GenericContainer<?> container = common( dockerImageName.dockerImageName() )
				.withEnv( "logger.level", "WARN" )
				.withEnv( "discovery.type", "single-node" )
				// Older images require HTTP authentication for all requests;
				// it's not practical for testing, so we disable that:
				.withEnv( "xpack.security.enabled", "false" )
				// Prevent swapping
				.withEnv( "bootstrap.memory_lock", "true" )
				// Limit the RAM usage.
				// Recent versions of ES limit themselves to 50% of the total available RAM,
				// but on CI this is sometimes too much, as we also have the Maven JVM
				// and the JVM that runs tests taking up a significant amount of RAM,
				// leaving too little for filesystem caches and resulting in freezes:
				.withEnv( "ES_JAVA_OPTS", "-Xms1500m -Xmx1500m" )
				// Disable disk-based shard allocation thresholds: on large, relatively full disks (>90% used),
				// it will lead to index creation to get stuck waiting for other nodes to join the cluster,
				// which will never happen since we only have one node.
				// See https://www.elastic.co/guide/en/elasticsearch/reference/7.17/modules-cluster.html#disk-based-shard-allocation
				.withEnv( "cluster.routing.allocation.disk.threshold_enabled", "false" );

		ElasticsearchVersion version =
				ElasticsearchVersion.of( ElasticsearchDistributionName.ELASTIC, dockerImageName.version() );

		// Disable a few features that we don't use and that just slow up container startup.
		if ( version.majorOptional().orElse( Integer.MIN_VALUE ) == 7 ) {
			if ( version.minor().orElse( Integer.MAX_VALUE ) > 15 ) {
				container.withEnv( "cluster.deprecation_indexing.enabled", "false" )
						.withEnv( "slm.history_index_enabled", "false" )
						// We do not really use the geoip processing and it may attempt to download info on startup/periodically..
						// let's disable it:
						.withEnv( "ingest.geoip.downloader.enabled", "false" );
			}
			return container.withEnv( "indices.lifecycle.history_index_enabled", "false" )
					.withEnv( "stack.templates.enabled", "false" )
					.withEnv( "xpack.ml.enabled", "false" )
					.withEnv( "xpack.watcher.enabled", "false" );
		}

		if ( version.majorOptional().orElse( Integer.MIN_VALUE ) > 8
				|| ( version.majorOptional().orElse( Integer.MIN_VALUE ) == 8
						&& version.minor().orElse( Integer.MAX_VALUE ) > 7 ) ) {
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

	private static GenericContainer<?> opensearch(DockerImageNameAndVersion dockerImageName) {
		GenericContainer<?> container = common( dockerImageName.dockerImageName() )
				.withEnv( "logger.level", "WARN" )
				.withEnv( "discovery.type", "single-node" )
				// Prevent swapping
				.withEnv( "bootstrap.memory_lock", "true" )
				// OpenSearch expects SSL and uses a self-signed certificate by default;
				// it's not practical for testing, so we disable that.
				.withEnv( "plugins.security.disabled", "true" )
				// Limit the RAM usage.
				.withEnv( "OPENSEARCH_JAVA_OPTS", "-Xms1500m -Xmx1500m" )
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
				ElasticsearchVersion.of( ElasticsearchDistributionName.OPENSEARCH, dockerImageName.version() );

		if ( ( version.majorOptional().orElse( Integer.MIN_VALUE ) == 2
				&& version.minor().orElse( Integer.MAX_VALUE ) > 11 )
				|| version.majorOptional().orElse( Integer.MIN_VALUE ) > 2 ) {
			// Note: For OpenSearch 2.12 and later, a custom password for the admin user is required to be passed to set-up and utilize demo configuration.
			container.withEnv( "OPENSEARCH_INITIAL_ADMIN_PASSWORD", "NotActua11y$trongPa$$word" )
					.withEnv( "DISABLE_INSTALL_DEMO_CONFIG", "true" );
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
				.withReuse( true )
				.withTmpFs( Map.of( "/usr/share/elasticsearch/data", "rw,size=1g" ) );
	}


	private static DockerImageNameAndVersion parseDockerImageName(Path dockerfile, String tag) throws IOException {
		Pattern DOCKERFILE_FROM_LINE_PATTERN = Pattern.compile( "FROM ([^@:\\s]+)(?::([^@\\s]+))?(@.+)?" );

		DockerImageNameAndVersion dockerImageName = Files.lines( dockerfile )
				.map( DOCKERFILE_FROM_LINE_PATTERN::matcher )
				.filter( Matcher::matches )
				.map( DockerImageNameAndVersion::of )
				.findAny().orElseThrow( () -> new IllegalStateException(
						"Dockerfile " + dockerfile + " has unexpected structure. It *must* contain a single FROM line." ) );

		if ( tag != null && !tag.trim().isEmpty() ) {
			return dockerImageName.withTag( tag );
		}

		return dockerImageName;
	}

	private record DockerImageNameAndVersion(DockerImageName dockerImageName, String version) {
		static DockerImageNameAndVersion of(Matcher matcher) {
			String name = matcher.group( 1 );
			String maybeTag = matcher.group( 2 );
			String maybeHash = matcher.group( 3 );

			if ( maybeHash == null ) {
				return new DockerImageNameAndVersion( DockerImageName.parse( name ).withTag( maybeTag ), maybeTag );
			}
			else if ( maybeTag == null ) {
				return new DockerImageNameAndVersion( DockerImageName.parse( name + maybeHash ), null );
			}
			else {
				return new DockerImageNameAndVersion( DockerImageName.parse( name + maybeHash ), maybeTag );
			}
		}

		public DockerImageNameAndVersion withTag(String tag) {
			return new DockerImageNameAndVersion( dockerImageName.withTag( tag ), tag );
		}
	}
}
