/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;

/**
 * @author Gunnar Morling
 */
// TODO Make service or similar
public class JestClientHolder {

	private static final JestClient CLIENT = setupClient();

	private static JestClient setupClient() {
		JestClientFactory factory = new ConnectionTimeoutAwareJestClientFactory();

		// TODO Make configurable

		factory.setHttpClientConfig(
			new HttpClientConfig.Builder( "http://192.168.59.103:9200" )
				.multiThreaded( true )
				.readTimeout( 2000 )
				.connTimeout( 2000 )
				.build()
		);

		return factory.getObject();
	}

	public static JestClient getClient() {
		return CLIENT;
	}

	// TODO shutdown
}
