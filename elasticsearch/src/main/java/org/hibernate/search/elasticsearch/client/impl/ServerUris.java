/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import org.apache.http.HttpHost;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

final class ServerUris {

	private static final Log log = LoggerFactory.make( Log.class );

	private final HttpHost[] hosts;
	private final boolean anyHostRequiresSSL;

	private ServerUris(HttpHost[] hosts, boolean anyHostRequiresSSL) {
		this.hosts = hosts;
		this.anyHostRequiresSSL = anyHostRequiresSSL;
	}

	static ServerUris fromString(String serverUrisString) {
		String[] serverUris = serverUrisString.trim().split( "\\s" );
		HttpHost[] hosts = new HttpHost[serverUris.length];
		boolean anyHostRequiresSSL = false;
		for ( int i = 0 ; i < serverUris.length ; ++i ) {
			HttpHost host = HttpHost.create( serverUris[i] );
			hosts[i] = host;
			String scheme = host.getSchemeName();
			if ( "https".equals( scheme ) ) {
				anyHostRequiresSSL = true;
			}
		}
		return new ServerUris( hosts, anyHostRequiresSSL );
	}

	HttpHost[] asHostsArray() {
		return hosts;
	}

	boolean isAnyRequiringSSL() {
		return anyHostRequiresSSL;
	}

	void warnPasswordsOverHttp() {
		for ( HttpHost host : hosts ) {
			if ( "http".equals( host.getSchemeName() ) ) {
				log.usingPasswordOverHttp( host.toURI() );
			}
		}
	}

}
