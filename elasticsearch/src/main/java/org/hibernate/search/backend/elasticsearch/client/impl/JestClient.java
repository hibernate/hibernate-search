/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.io.IOException;
import java.util.Properties;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.searchbox.action.Action;
import io.searchbox.client.AbstractJestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;

/**
 * Provides access to the JEST client.
 *
 * @author Gunnar Morling
 */
public class JestClient implements Service, Startable, Stoppable {

	private io.searchbox.client.JestClient client;

	@Override
	public void start(Properties properties, BuildContext context) {
		JestClientFactory factory = new JestClientFactory();

		String serverUri = ConfigurationParseHelper.getString(
				properties, ElasticsearchEnvironment.SERVER_URI, "http://localhost:9200"
		);

		Gson gson = new GsonBuilder()
				.setDateFormat( AbstractJestClient.ELASTIC_SEARCH_DATE_FORMAT )
				.serializeNulls()
				.create();

		factory.setHttpClientConfig(
			new HttpClientConfig.Builder( serverUri )
				.multiThreaded( true )
				.readTimeout( 2000 )
				.connTimeout( 2000 )
				.gson( gson )
				.build()
		);

		client = factory.getObject();
	}

	@Override
	public void stop() {
		client.shutdownClient();
	}

	public <T extends JestResult> T executeRequest(Action<T> request) {
		return executeRequest( request, true );
	}

	public <T extends JestResult> T executeRequest(Action<T> request, boolean failOnError) {
		T result;
		try {
			result = client.execute( request );

			if ( failOnError && !result.isSucceeded() ) {
				throw new SearchException( result.getErrorMessage() );
			}

			return result;
		}
		catch (IOException e) {
			throw new SearchException( e );
		}
	}
}
