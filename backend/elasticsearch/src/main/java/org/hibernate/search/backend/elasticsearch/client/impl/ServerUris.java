/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.http.HttpHost;

final class ServerUris {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HttpHost[] hosts;
	private final boolean sslEnabled;

	private ServerUris(HttpHost[] hosts, boolean sslEnabled) {
		this.hosts = hosts;
		this.sslEnabled = sslEnabled;
	}

	static ServerUris fromStrings(String protocol, List<String> hostAndPortStrings) {
		HttpHost[] hosts = new HttpHost[hostAndPortStrings.size()];
		// Note: protocol and URI scheme are not the same thing,
		// but for HTTP/HTTPS both the protocol and URI scheme are named HTTP/HTTPS.
		String scheme = protocol.toLowerCase( Locale.ROOT );
		for ( int i = 0 ; i < hostAndPortStrings.size() ; ++i ) {
			HttpHost host = createHttpHost( scheme, hostAndPortStrings.get( i ) );
			hosts[i] = host;
		}
		return new ServerUris( hosts, "https".equals( scheme ) );
	}

	private static HttpHost createHttpHost(String scheme, String hostAndPort) {
		if ( hostAndPort.indexOf( "://" ) >= 0 ) {
			throw log.invalidHostAndPort( hostAndPort, null );
		}
		String host;
		int port = -1;
		final int portIdx = hostAndPort.lastIndexOf( ":" );
		if ( portIdx < 0 ) {
			host = hostAndPort;
		}
		else {
			try {
				port = Integer.parseInt( hostAndPort.substring( portIdx + 1 ) );
			}
			catch (final NumberFormatException e) {
				throw log.invalidHostAndPort( hostAndPort, e );
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
