/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.logging.impl.ConfigurationLog;

import org.apache.http.HttpHost;

final class ServerUris {

	private final HttpHost[] hosts;
	private final boolean sslEnabled;

	private ServerUris(HttpHost[] hosts, boolean sslEnabled) {
		this.hosts = hosts;
		this.sslEnabled = sslEnabled;
	}

	static ServerUris fromOptionalStrings(Optional<String> protocol, Optional<List<String>> hostAndPortStrings,
			Optional<List<String>> uris) {
		if ( !uris.isPresent() ) {
			String protocolValue = ( protocol.isPresent() ) ? protocol.get() : ElasticsearchBackendSettings.Defaults.PROTOCOL;
			List<String> hostAndPortValues =
					( hostAndPortStrings.isPresent() ) ? hostAndPortStrings.get() : ElasticsearchBackendSettings.Defaults.HOSTS;
			return fromStrings( protocolValue, hostAndPortValues );
		}

		if ( protocol.isPresent() ) {
			throw ConfigurationLog.INSTANCE.uriAndProtocol( uris.get(), protocol.get() );
		}

		if ( hostAndPortStrings.isPresent() ) {
			throw ConfigurationLog.INSTANCE.uriAndHosts( uris.get(), hostAndPortStrings.get() );
		}

		return fromStrings( uris.get() );
	}

	private static ServerUris fromStrings(List<String> serverUrisStrings) {
		if ( serverUrisStrings.isEmpty() ) {
			throw ConfigurationLog.INSTANCE.emptyListOfUris();
		}

		HttpHost[] hosts = new HttpHost[serverUrisStrings.size()];
		Boolean https = null;
		for ( int i = 0; i < serverUrisStrings.size(); ++i ) {
			HttpHost host = HttpHost.create( serverUrisStrings.get( i ) );
			hosts[i] = host;
			String scheme = host.getSchemeName();
			boolean currentHttps = "https".equals( scheme );
			if ( https == null ) {
				https = currentHttps;
			}
			else if ( currentHttps != https ) {
				throw ConfigurationLog.INSTANCE.differentProtocolsOnUris( serverUrisStrings );
			}
		}

		return new ServerUris( hosts, https );
	}

	private static ServerUris fromStrings(String protocol, List<String> hostAndPortStrings) {
		if ( hostAndPortStrings.isEmpty() ) {
			throw ConfigurationLog.INSTANCE.emptyListOfHosts();
		}

		HttpHost[] hosts = new HttpHost[hostAndPortStrings.size()];
		// Note: protocol and URI scheme are not the same thing,
		// but for HTTP/HTTPS both the protocol and URI scheme are named HTTP/HTTPS.
		String scheme = protocol.toLowerCase( Locale.ROOT );
		for ( int i = 0; i < hostAndPortStrings.size(); ++i ) {
			HttpHost host = createHttpHost( scheme, hostAndPortStrings.get( i ) );
			hosts[i] = host;
		}
		return new ServerUris( hosts, "https".equals( scheme ) );
	}

	private static HttpHost createHttpHost(String scheme, String hostAndPort) {
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
		return new HttpHost( host, port, scheme );
	}

	HttpHost[] asHostsArray() {
		return hosts;
	}

	boolean isSslEnabled() {
		return sslEnabled;
	}

}
