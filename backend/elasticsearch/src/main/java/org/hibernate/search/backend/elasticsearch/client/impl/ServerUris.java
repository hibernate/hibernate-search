/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

import org.apache.http.HttpHost;

final class ServerUris {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HttpHost[] hosts;
	private final boolean anyHostRequiresSSL;

	private ServerUris(HttpHost[] hosts, boolean anyHostRequiresSSL) {
		this.hosts = hosts;
		this.anyHostRequiresSSL = anyHostRequiresSSL;
	}

	static ServerUris fromStrings(List<String> serverUrisStrings) {
		HttpHost[] hosts = new HttpHost[serverUrisStrings.size()];
		boolean anyHostRequiresSSL = false;
		for ( int i = 0 ; i < serverUrisStrings.size() ; ++i ) {
			HttpHost host = HttpHost.create( serverUrisStrings.get( i ) );
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
