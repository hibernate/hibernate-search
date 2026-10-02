package org.hibernate.search.backend.elasticsearch.client.impl;

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
				String scheme = actual.getScheme().toLowerCase( Locale.ROOT );
				serverNodes.add( createServerNode( scheme, host, pathPrefix ) );
				boolean currentHttps = "https".equals( scheme );
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
		private final String baseUrl;

		public ServerNode(String protocol, String host, String basePath, int port) {
			this.baseUrl = buildBaseUrl( protocol, host, normalizeBasePath( basePath ), port );
		}

		/**
		 * Builds the invariant part of request URIs: scheme, authority and base path.
		 * <p>
		 * Only the authority really needs to be built through the multi-argument {@link URI} constructor,
		 * which takes care of escaping and of IPv6 literals in particular.
		 * The base path, on the other hand, is treated as already URL-encoded,
		 * just like the request paths it gets prepended to,
		 * and consistently with how the REST clients handle their path prefix.
		 */
		private static String buildBaseUrl(String protocol, String host, String basePath, int port) {
			String schemeAndAuthority;
			try {
				schemeAndAuthority = new URI( protocol, null, host, port, null, null, null ).toString();
			}
			catch (URISyntaxException e) {
				throw ConfigurationLog.INSTANCE.invalidUri( protocol + "://" + host, e.getMessage(), e );
			}
			try {
				// Validate eagerly, so that a malformed path prefix is reported on startup
				// rather than on the first request.
				return new URI( schemeAndAuthority + basePath ).toString();
			}
			catch (URISyntaxException e) {
				throw ConfigurationLog.INSTANCE.invalidUri( schemeAndAuthority + basePath, e.getMessage(), e );
			}
		}

		private static String normalizeBasePath(String basePath) {
			if ( basePath.endsWith( "/" ) ) {
				basePath = basePath.substring( 0, basePath.length() - 1 );
			}
			// Also covers a base path consisting of a single '/', which would otherwise
			// result in a double slash once a request path gets appended.
			if ( basePath.isEmpty() ) {
				return "";
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
			String queryString = buildQueryString( parameters );
			// Request paths are already URL-encoded (see URLEncodedString) and query string parameters
			// are encoded in buildQueryString, so the URI is parsed as-is:
			// the multi-argument URI constructor would escape the '%' characters a second time.
			return URI.create( queryString.isEmpty() ? baseUrl + path : baseUrl + path + "?" + queryString );
		}

		@Override
		public boolean equals(Object o) {
			if ( !( o instanceof ServerNode that ) ) {
				return false;
			}
			return Objects.equals( baseUrl, that.baseUrl );
		}

		@Override
		public int hashCode() {
			return Objects.hash( baseUrl );
		}

		@Override
		public String toString() {
			return baseUrl;
		}
	}

	public enum Status {
		ACTIVE, FAILING;
	}
}
