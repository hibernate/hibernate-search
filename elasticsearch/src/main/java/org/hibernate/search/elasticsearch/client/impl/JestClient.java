/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;

import io.searchbox.action.Action;
import io.searchbox.action.BulkableAction;
import io.searchbox.action.DocumentTargetedAction;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Bulk;
import io.searchbox.core.Bulk.Builder;
import io.searchbox.core.BulkResult;
import io.searchbox.core.BulkResult.BulkResultItem;
import io.searchbox.params.Parameters;

/**
 * Provides access to the JEST client.
 *
 * @author Gunnar Morling
 */
public class JestClient implements Service, Startable, Stoppable {

	private static final Log LOG = LoggerFactory.make( Log.class );

	/**
	 * HTTP response code for a request timed out.
	 */
	private static final int TIME_OUT = 408;

	/**
	 * Prefix for accessing the {@link ElasticsearchEnvironment#SERVER_URI} variable. That's currently needed as we
	 * don't access this one in the context of a specific index manager. The prefix is used to have the property name
	 * in line with the other index-related property names, even though this property can not yet be override on
	 * a per-index base.
	 */
	private static final String SERVER_URI_PROP_PREFIX = "hibernate.search.default.";

	private io.searchbox.client.JestClient client;

	private ServiceManager serviceManager;
	private GsonService gsonService;

	@Override
	public void start(Properties properties, BuildContext context) {
		serviceManager = context.getServiceManager();
		gsonService = serviceManager.requestService( GsonService.class );

		JestClientFactory factory = new JestClientFactory();

		String serverUri = ConfigurationParseHelper.getString(
				properties,
				SERVER_URI_PROP_PREFIX + ElasticsearchEnvironment.SERVER_URI,
				ElasticsearchEnvironment.Defaults.SERVER_URI
		);

		// TODO HSEARCH-2062 Make timeouts configurable
		factory.setHttpClientConfig(
			new HttpClientConfig.Builder( serverUri )
				.multiThreaded( true )
				.readTimeout( 60000 )
				.connTimeout( 2000 )
				.gson( gsonService.getGson() )
				.build()
		);

		client = factory.getObject();
	}

	@Override
	public void stop() {
		client.shutdownClient();
		client = null;

		gsonService = null;
		serviceManager.releaseService( GsonService.class );
		serviceManager = null;
	}

	public <T extends JestResult> T executeRequest(Action<T> request, int... ignoredErrorStatuses) {
		return executeRequest( request, asSet( ignoredErrorStatuses ) );
	}

	public <T extends JestResult> T executeRequest(Action<T> request, Set<Integer> ignoredErrorStatuses) {
		try {
			T result = client.execute( request );

			// The request failed with a status that's not ignore-able
			if ( !result.isSucceeded() && !isIgnored( result.getResponseCode(), ignoredErrorStatuses ) ) {
				if ( result.getResponseCode() == TIME_OUT ) {
					throw LOG.elasticsearchRequestTimeout( requestToString( request ), resultToString( result ) );
				}
				else {
					throw LOG.elasticsearchRequestFailed( requestToString( request ), resultToString( result ), null );
				}
			}

			return result;
		}
		catch (IOException e) {
			throw LOG.elasticsearchRequestFailed( requestToString( request ), null, e );
		}
	}

	/**
	 * Creates a bulk action from the given list and executes it.
	 */
	public void executeBulkRequest(List<BackendRequest<?>> actions, boolean refresh) {
		Builder bulkBuilder = new Bulk.Builder()
				.setParameter( Parameters.REFRESH, refresh );

		for ( BackendRequest<?> action : actions ) {
			bulkBuilder.addAction( (BulkableAction<?>) action.getAction() );
		}

		Bulk request = bulkBuilder.build();

		try {
			BulkResult response = client.execute( request );

			// Ideally I could just check on the status of the bulk, but for some reason the ES response is not
			// always set to erroneous also if there is an erroneous item; I suppose that's a bug in ES? For now we are
			// examining the result items and check if there is any erroneous
			List<BackendRequest<?>> erroneousItems = getErroneousItems( actions, response );

			if ( !erroneousItems.isEmpty() ) {
				throw LOG.elasticsearchBulkRequestFailed(
						requestToString( request ),
						resultToString( response ),
						erroneousItems
				);
			}
		}
		catch (IOException e) {
			throw LOG.elasticsearchRequestFailed( requestToString( request ), null, e );
		}
	}

	private List<BackendRequest<?>> getErroneousItems(List<BackendRequest<?>> actions, BulkResult response) {
		int i = 0;

		List<BackendRequest<?>> erroneousItems = new ArrayList<>();

		for ( BulkResultItem resultItem : response.getItems() ) {
			// When getting a 404 for a DELETE, the error is null :(, so checking both
			if ( resultItem.error != null || resultItem.status >= 400 ) {
				BackendRequest<?> action = actions.get( i );
				if ( !action.getIgnoredErrorStatuses().contains( resultItem.status ) ) {
					erroneousItems.add( action );
				}
			}
			i++;
		}

		return erroneousItems;
	}

	private boolean isIgnored(int responseCode, Set<Integer> ignoredStatuses) {
		if ( ignoredStatuses == null ) {
			return true;
		}
		else {
			return ignoredStatuses.contains( responseCode );
		}
	}

	private Set<Integer> asSet(int... ignoredErrorStatuses) {
		Set<Integer> ignored;

		if ( ignoredErrorStatuses == null || ignoredErrorStatuses.length == 0 ) {
			ignored = Collections.emptySet();
		}
		else if ( ignoredErrorStatuses.length == 1 ) {
			ignored = Collections.singleton( ignoredErrorStatuses[0] );
		}
		else {
			ignored = new HashSet<>();
			for ( int ignoredErrorStatus : ignoredErrorStatuses ) {
				ignored.add( ignoredErrorStatus );
			}
		}

		return ignored;
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
		sb.append( action.getData( gsonService.getGson() ) );
		sb.append( "\n" );
		return sb.toString();
	}

	private String resultToString(JestResult result) {
		StringBuilder sb = new StringBuilder();
		sb.append( "Status: " ).append( result.getResponseCode() ).append( "\n" );
		sb.append( "Error message: " ).append( result.getErrorMessage() ).append( "\n" );
		sb.append( "Cluster name: " ).append( property( result, "cluster_name" ) ).append( "\n" );
		sb.append( "Cluster status: " ).append( property( result, "status" ) ).append( "\n" );
		sb.append( "\n" );

		if ( result instanceof BulkResult ) {
			for ( BulkResultItem item : ( (BulkResult) result ).getItems() ) {
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

	private String property(JestResult result, String name) {
		if ( result.getJsonObject() == null ) {
			return null;
		}
		JsonElement clusterName = result.getJsonObject().get( name );
		if ( clusterName == null ) {
			return null;
		}
		return clusterName.getAsString();
	}
}
