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
import org.hibernate.search.backend.elasticsearch.impl.GsonHolder;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import io.searchbox.action.Action;
import io.searchbox.action.DocumentTargetedAction;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.BulkResult;
import io.searchbox.core.BulkResult.BulkResultItem;

/**
 * Provides access to the JEST client.
 *
 * @author Gunnar Morling
 */
public class JestClient implements Service, Startable, Stoppable {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private io.searchbox.client.JestClient client;

	@Override
	public void start(Properties properties, BuildContext context) {
		JestClientFactory factory = new JestClientFactory();

		String serverUri = ConfigurationParseHelper.getString(
				properties,
				ElasticsearchEnvironment.SERVER_URI,
				ElasticsearchEnvironment.Defaults.SERVER_URI
		);

		// TODO HSEARCH-2062 Make timeouts configurable
		factory.setHttpClientConfig(
			new HttpClientConfig.Builder( serverUri )
				.multiThreaded( true )
				.readTimeout( 60000 )
				.connTimeout( 2000 )
				.gson( GsonHolder.GSON )
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
				throw LOG.elasticsearchRequestFailed( requestToString( request ), resultToString( result ) );
			}

			return result;
		}
		catch (IOException e) {
			throw new SearchException( e );
		}
	}

	private String requestToString(Action<?> action) {
		StringBuilder sb = new StringBuilder();

		sb.append( "Operation: " ).append( action.getClass().getSimpleName() ).append( "\n" );

		if ( action instanceof DocumentTargetedAction ) {
			sb.append( "Index: " ).append( ( (DocumentTargetedAction<?>) action ).getIndex() ).append( "\n" );
			sb.append( "Type: " ).append( ( (DocumentTargetedAction<?>) action ).getType() ).append( "\n" );
			sb.append( "Id: " ).append( ( (DocumentTargetedAction<?>) action ).getId() ).append( "\n" );
		}

		sb.append( "Data:\n" );
		sb.append( action.getData( GsonHolder.GSON ) );
		sb.append( "\n" );
		return sb.toString();
	}

	private String resultToString(JestResult result) {
		StringBuilder sb = new StringBuilder();

		sb.append( "Status: " ).append( result.getResponseCode() ).append( "\n" );
		sb.append( "Error message: " ).append( result.getErrorMessage() ).append( "\n\n" );

		if ( result instanceof BulkResult ) {
			for ( BulkResultItem item : ( (BulkResult) result ).getFailedItems() ) {
				sb.append( "Operation: " ).append( item.operation ).append( "\n" );
				sb.append( "  Index: " ).append( item.index ).append( "\n" );
				sb.append( "  Type: " ).append( item.type ).append( "\n" );
				sb.append( "  Id: " ).append( item.id ).append( "\n" );
				sb.append( "  Status: " ).append( item.status ).append( "\n" );
				sb.append( "  Error: " ).append( item.error ).append( "\n" );
			}
		}

		return sb.toString();
	}
}
