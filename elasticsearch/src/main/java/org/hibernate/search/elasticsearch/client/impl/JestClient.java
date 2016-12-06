/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.CollectionHelper;
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
	 * Prefix for accessing the client-related settings.
	 * That's currently needed as we only have a single client for all
	 * index managers.
	 * The prefix is used to have the property name in line with the other
	 * index-related property names, even though this property can not yet
	 * be overridden on a per-index basis.
	 */
	private static final String CLIENT_PROP_PREFIX = "hibernate.search.default.";

	private io.searchbox.client.JestClient client;

	private ServiceManager serviceManager;
	private GsonService gsonService;

	@Override
	public void start(Properties properties, BuildContext context) {
		serviceManager = context.getServiceManager();
		gsonService = serviceManager.requestService( GsonService.class );

		JestClientFactory factory = new JestClientFactory();

		String serverUrisString = ConfigurationParseHelper.getString(
				properties,
				CLIENT_PROP_PREFIX + ElasticsearchEnvironment.SERVER_URI,
				ElasticsearchEnvironment.Defaults.SERVER_URI
		);

		Collection<String> serverUris = Arrays.asList( serverUrisString.trim().split( "\\s" ) );

		factory.setHttpClientConfig(
			new HttpClientConfig.Builder( serverUris )
				.multiThreaded( true )
				.readTimeout( ConfigurationParseHelper.getIntValue(
						properties,
						CLIENT_PROP_PREFIX + ElasticsearchEnvironment.SERVER_READ_TIMEOUT,
						ElasticsearchEnvironment.Defaults.SERVER_READ_TIMEOUT
				) )
				.connTimeout( ConfigurationParseHelper.getIntValue(
						properties,
						CLIENT_PROP_PREFIX + ElasticsearchEnvironment.SERVER_CONNECTION_TIMEOUT,
						ElasticsearchEnvironment.Defaults.SERVER_CONNECTION_TIMEOUT
				) )
				.maxTotalConnection( ConfigurationParseHelper.getIntValue(
						properties,
						CLIENT_PROP_PREFIX + ElasticsearchEnvironment.MAX_TOTAL_CONNECTION,
						ElasticsearchEnvironment.Defaults.MAX_TOTAL_CONNECTION
				) )
				.defaultMaxTotalConnectionPerRoute( ConfigurationParseHelper.getIntValue(
						properties,
						CLIENT_PROP_PREFIX + ElasticsearchEnvironment.MAX_TOTAL_CONNECTION_PER_ROUTE,
						ElasticsearchEnvironment.Defaults.MAX_TOTAL_CONNECTION_PER_ROUTE
				) )
				.discoveryEnabled( ConfigurationParseHelper.getBooleanValue(
						properties,
						CLIENT_PROP_PREFIX + ElasticsearchEnvironment.DISCOVERY_ENABLED,
						ElasticsearchEnvironment.Defaults.DISCOVERY_ENABLED
				) )
				.discoveryFrequency(
						ConfigurationParseHelper.getLongValue(
								properties,
								CLIENT_PROP_PREFIX + ElasticsearchEnvironment.DISCOVERY_REFRESH_INTERVAL,
								ElasticsearchEnvironment.Defaults.DISCOVERY_REFRESH_INTERVAL
						),
						TimeUnit.SECONDS
				)
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

	/**
	 * Just to remove ambiguity between {@link #executeRequest(Action, int...)} and {@link #executeRequest(Action, String...)}
	 * when the vararg is empty.
	 */
	public <T extends JestResult> T executeRequest(Action<T> request) {
		return executeRequest( request, Collections.<Integer>emptySet(), Collections.<String>emptySet() );
	}

	public <T extends JestResult> T executeRequest(Action<T> request, int... ignoredErrorStatuses) {
		return executeRequest( request, asSet( ignoredErrorStatuses ) );
	}

	public <T extends JestResult> T executeRequest(Action<T> request, String... ignoredErrorTypes) {
		return executeRequest( request, Collections.<Integer>emptySet(), CollectionHelper.asImmutableSet( ignoredErrorTypes ) );
	}

	public <T extends JestResult> T executeRequest(Action<T> request, Set<Integer> ignoredErrorStatuses) {
		return executeRequest( request, ignoredErrorStatuses, Collections.<String>emptySet() );
	}

	public <T extends JestResult> T executeRequest(Action<T> request, Set<Integer> ignoredErrorStatuses, Set<String> ignoredErrorTypes) {
		try {
			T result = client.execute( request );

			// The request failed with a status that's not ignore-able
			if ( !result.isSucceeded() && !isResponseCode( result.getResponseCode(), ignoredErrorStatuses )
					&& !isErrorType( result, ignoredErrorTypes ) ) {
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
	public Map<BackendRequest<?>, BulkResultItem> executeBulkRequest(List<BackendRequest<?>> backendRequests, boolean refresh) {
		Builder bulkBuilder = new Bulk.Builder()
				.setParameter( Parameters.REFRESH, refresh );

		for ( BackendRequest<?> backendRequest : backendRequests ) {
			bulkBuilder.addAction( (BulkableAction<?>) backendRequest.getAction() );
		}

		Bulk request = bulkBuilder.build();

		try {
			BulkResult response = client.execute( request );

			Map<BackendRequest<?>, BulkResultItem> successfulItems =
					CollectionHelper.newHashMap( backendRequests.size() );

			/*
			 * We can't rely on the status of the bulk, since each backend request may consider specific
			 * status codes as a success regardless of their usual meaning, which Elasticsearch doesn't
			 * know about when computing the status of the bulk.
			 */
			List<BackendRequest<?>> erroneousItems = new ArrayList<>();
			int i = 0;
			for ( BulkResultItem resultItem : response.getItems() ) {
				BackendRequest<?> backendRequest = backendRequests.get( i );

				if ( isErrored( backendRequest, resultItem ) ) {
					erroneousItems.add( backendRequest );
				}
				else {
					successfulItems.put( backendRequest, resultItem );
				}
				++i;
			}

			if ( !erroneousItems.isEmpty() ) {
				throw LOG.elasticsearchBulkRequestFailed(
						requestToString( request ),
						resultToString( response ),
						successfulItems,
						erroneousItems
				);
			}
			else {
				return successfulItems;
			}
		}
		catch (IOException e) {
			throw LOG.elasticsearchRequestFailed( requestToString( request ), null, e );
		}
	}

	private boolean isErrored(BackendRequest<?> backendRequest, BulkResultItem resultItem) {
		// When getting a 404 for a DELETE, the error is null :(, so checking both
		return (resultItem.error != null || resultItem.status >= 400 )
			&& !isResponseCode( resultItem.status, backendRequest.getIgnoredErrorStatuses() );
	}

	private boolean isResponseCode(int responseCode, Set<Integer> codes) {
		if ( codes == null ) {
			return false;
		}
		else {
			return codes.contains( responseCode );
		}
	}

	private boolean isErrorType(JestResult result, Set<String> errorTypes) {
		if ( errorTypes == null ) {
			return false;
		}
		else {
			return errorTypes.contains( getErrorType(result) );
		}
	}

	private String getErrorType(JestResult result) {
		JsonElement error = result.getJsonObject().get( "error" );
		if ( error == null || !error.isJsonObject() ) {
			return null;
		}

		JsonElement errorType = error.getAsJsonObject().get( "type" );
		if ( errorType == null || !errorType.isJsonPrimitive() ) {
			return null;
		}

		return errorType.getAsString();
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
		sb.append( "URI:" ).append( action.getURI() ).append( "\n" );

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
