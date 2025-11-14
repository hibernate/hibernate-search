/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.jdk.cfg.spi;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.logging.spi.ConfigurationLog;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public class NodeProvider {

	public static NodeProvider fromOptionalStrings(Optional<String> protocol, Optional<List<String>> hostAndPortStrings,
			Optional<List<String>> uris, String pathPrefix) {
		if ( !uris.isPresent() ) {
			String protocolValue =
					( protocol.isPresent() ) ? protocol.get() : ElasticsearchBackendSettings.Defaults.PROTOCOL;
			List<String> hostAndPortValues =
					( hostAndPortStrings.isPresent() )
							? hostAndPortStrings.get()
							: ElasticsearchBackendSettings.Defaults.HOSTS;
			return fromStrings( protocolValue, hostAndPortValues, pathPrefix );
		}

		if ( protocol.isPresent() ) {
			throw ConfigurationLog.INSTANCE.uriAndProtocol( uris.get(), protocol.get() );
		}

		if ( hostAndPortStrings.isPresent() ) {
			throw ConfigurationLog.INSTANCE.uriAndHosts( uris.get(), hostAndPortStrings.get() );
		}

		return fromStrings( uris.get(), pathPrefix );
	}

	private final AtomicInteger currentNode = new AtomicInteger( 0 );
	private final int numberOfNodes;
	private final List<ServerNode> serverNodes;
	private final boolean httpsEnabled;

	public NodeProvider(List<ServerNode> serverNodes, boolean httpsEnabled) {
		this.serverNodes = serverNodes;
		this.numberOfNodes = serverNodes.size();
		this.httpsEnabled = httpsEnabled;
		// TODO: we can add a discovery of other nodes here later?
	}

	public ServerNode nextNode() {
		return serverNodes.get( currentNode.getAndUpdate( this::updateCounter ) );
	}

	private int updateCounter(int i) {
		return ( i + 1 ) % numberOfNodes;
	}

	private static NodeProvider fromStrings(String protocol, List<String> hostAndPortStrings, String pathPrefix) {
		if ( hostAndPortStrings.isEmpty() ) {
			throw ConfigurationLog.INSTANCE.emptyListOfHosts();
		}

		List<ServerNode> serverNodes = new ArrayList<>( hostAndPortStrings.size() );
		// Note: protocol and URI scheme are not the same thing,
		// but for HTTP/HTTPS both the protocol and URI scheme are named HTTP/HTTPS.
		String scheme = protocol.toLowerCase( Locale.ROOT );
		for ( int i = 0; i < hostAndPortStrings.size(); ++i ) {
			serverNodes.add( createServerNode( scheme, hostAndPortStrings.get( i ), pathPrefix ) );
		}
		return new NodeProvider( serverNodes, "https".equals( scheme ) );
	}

	private static ServerNode createServerNode(String scheme, String hostAndPort, String pathPrefix) {
		if ( hostAndPort.indexOf( "://" ) >= 0 ) {
			throw ConfigurationLog.INSTANCE.invalidHostAndPort( hostAndPort, null );
		}
		String host;
		int port = -1;
		final int portIdx = hostAndPort.lastIndexOf( ':' );
		if ( portIdx < 0 ) {
			host = hostAndPort;
		}
		else {
			try {
				port = Integer.parseInt( hostAndPort.substring( portIdx + 1 ) );
			}
			catch (final NumberFormatException e) {
				throw ConfigurationLog.INSTANCE.invalidHostAndPort( hostAndPort, e );
			}
			host = hostAndPort.substring( 0, portIdx );
		}
		return new ServerNode( scheme, host, pathPrefix, port );
	}

	private static NodeProvider fromStrings(List<String> serverUrisStrings, String pathPrefix) {
		if ( serverUrisStrings.isEmpty() ) {
			throw ConfigurationLog.INSTANCE.emptyListOfUris();
		}

		List<ServerNode> serverNodes = new ArrayList<>( serverUrisStrings.size() );
		Boolean https = null;
		for ( int i = 0; i < serverUrisStrings.size(); ++i ) {
			String uri = serverUrisStrings.get( i );
			try {
				final int schemeIdx = uri.indexOf( "://" );
				if ( schemeIdx < 0 ) {
					uri = "http://" + uri;
				}
				URI actual = URI.create( uri );
				String host = actual.getHost();
				if ( actual.getPort() != -1 ) {
					host = host + ":" + actual.getPort();
				}
				serverNodes.add( createServerNode( actual.getScheme(), host, pathPrefix ) );
				boolean currentHttps = "https".equals( actual.getScheme() );
				if ( https == null ) {
					https = currentHttps;
				}
				else if ( currentHttps != https ) {
					throw ConfigurationLog.INSTANCE.differentProtocolsOnUris( serverUrisStrings );
				}
			}
			catch (IllegalArgumentException e) {
				throw ConfigurationLog.INSTANCE.invalidUri( uri, e.getMessage(), e );
			}
		}

		return new NodeProvider( serverNodes, https );
	}

	public boolean isSslEnabled() {
		return httpsEnabled;
	}

	public static final class ServerNode {
		private static final long RETRY_DELAY_MILLISECONDS = 1_000L;
		private final String protocol;
		private final String host;
		private final String basePath;
		private final int port;
		private volatile Status status;
		private volatile long retryAfterMillis;
		private ServerNode next;

		public ServerNode(String protocol, String host, String basePath, int port) {
			this.protocol = protocol;
			this.host = host;
			this.basePath = normalizeBasePath( basePath );
			this.port = port;
			this.status = Status.ACTIVE;
			this.retryAfterMillis = -1;
		}

		private String normalizeBasePath(String basePath) {
			if ( "".equals( basePath ) ) {
				return "";
			}
			else if ( basePath.endsWith( "/" ) ) {
				basePath = basePath.substring( 0, basePath.length() - 1 );
			}
			if ( !basePath.startsWith( "/" ) ) {
				basePath = "/" + basePath;
			}
			return basePath;
		}

		private String buildQueryString(Map<String, String> parameters) {
			if ( parameters == null || parameters.isEmpty() ) {
				return "";
			}

			StringBuilder queryString = new StringBuilder();
			boolean first = true;

			for ( Map.Entry<String, String> entry : parameters.entrySet() ) {
				if ( !first ) {
					queryString.append( "&" );
				}

				String encodedKey = URLEncoder.encode( entry.getKey(), StandardCharsets.UTF_8 );
				String encodedValue = URLEncoder.encode( entry.getValue(), StandardCharsets.UTF_8 );

				queryString.append( encodedKey ).append( "=" ).append( encodedValue );
				first = false;
			}

			return queryString.toString();
		}

		public URI createRequestURI(String path, Map<String, String> parameters) {
			if ( !path.isEmpty() && !path.startsWith( "/" ) ) {
				throw new IllegalArgumentException( "Path must start with '/': " + path );
			}
			String fullpath = basePath + path;
			String queryString = buildQueryString( parameters );
			try {
				return new URI( protocol, null, host, port, fullpath, queryString.isEmpty() ? null : queryString, null );
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException( "Invalid URI: " + e.getMessage(), e );
			}
		}

		public void failing() {
			this.status = Status.FAILING;
			this.retryAfterMillis = System.currentTimeMillis() + RETRY_DELAY_MILLISECONDS;
		}

		private void setNext(ServerNode next) {
			this.next = next;
		}

		@Override
		public boolean equals(Object o) {
			if ( !( o instanceof ServerNode that ) ) {
				return false;
			}
			return port == that.port
					&& Objects.equals( protocol, that.protocol ) && Objects.equals( host, that.host )
					&& Objects.equals( basePath, that.basePath );
		}

		@Override
		public int hashCode() {
			return Objects.hash( protocol, host, basePath, port );
		}
	}

	public enum Status {
		ACTIVE, FAILING;
	}
}
