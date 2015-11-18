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

import java.util.Properties;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticSearchEnvironment;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;

/**
 * Provides access to the JEST client.
 *
 * @author Gunnar Morling
 */
public class JestClientService implements Service, Startable, Stoppable {

	private JestClient client;

	@Override
	public void start(Properties properties, BuildContext context) {
		JestClientFactory factory = new ConnectionTimeoutAwareJestClientFactory();

		String serverUri = ConfigurationParseHelper.getString(
				properties, ElasticSearchEnvironment.SERVER_URI, "http://localhost:9200"
		);


		factory.setHttpClientConfig(
			new HttpClientConfig.Builder( serverUri )
				.multiThreaded( true )
				.readTimeout( 2000 )
				.connTimeout( 2000 )
				.build()
		);

		client = factory.getObject();
	}

	@Override
	public void stop() {
		client.shutdownClient();
	}

	public JestClient getClient() {
		return client;
	}
}
