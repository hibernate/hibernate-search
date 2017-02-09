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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.impl.DefaultBackendRequestResultAssessor;
import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.impl.JestAPIFormatter;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import io.searchbox.action.Action;
import io.searchbox.action.BulkableAction;
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
	private JestAPIFormatter jestAPIFormatter;
	private DefaultBackendRequestResultAssessor defaultResultAssessor;

	@Override
	public void start(Properties properties, BuildContext context) {
		serviceManager = context.getServiceManager();
		gsonService = serviceManager.requestService( GsonService.class );
		jestAPIFormatter = serviceManager.requestService( JestAPIFormatter.class );
		defaultResultAssessor = DefaultBackendRequestResultAssessor.builder( jestAPIFormatter ).build();

		JestClientFactory factory = new JestClientFactory();

		String serverUrisString = ConfigurationParseHelper.getString(
				properties,
				CLIENT_PROP_PREFIX + ElasticsearchEnvironment.SERVER_URI,
				ElasticsearchEnvironment.Defaults.SERVER_URI
		);

		Collection<String> serverUris = Arrays.asList( serverUrisString.trim().split( "\\s" ) );

		HttpClientConfig.Builder builder = new HttpClientConfig.Builder( serverUris )
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
				.gson( gsonService.getGson() );

		String username = ConfigurationParseHelper.getString(
				properties,
				CLIENT_PROP_PREFIX + ElasticsearchEnvironment.SERVER_USERNAME,
				null
		);
		if ( username != null ) {
			String password = ConfigurationParseHelper.getString(
					properties,
					CLIENT_PROP_PREFIX + ElasticsearchEnvironment.SERVER_PASSWORD,
					null
			);
			builder = builder.defaultCredentials( username, password );
		}

		factory.setHttpClientConfig( builder.build() );

		client = factory.getObject();
	}

	@Override
	public void stop() {
		client.shutdownClient();
		client = null;

		jestAPIFormatter = null;
		serviceManager.releaseService( JestAPIFormatter.class );
		gsonService = null;
		serviceManager.releaseService( GsonService.class );
		serviceManager = null;
	}

	public <T extends JestResult> T executeRequest(Action<T> request) {
		return executeRequest( request, defaultResultAssessor );
	}

	public <T extends JestResult> T executeRequest(Action<T> request, BackendRequestResultAssessor<? super T> resultAssessor) {
		try {
			T result = client.execute( request );

			resultAssessor.checkSuccess( request, result );

			return result;
		}
		catch (IOException e) {
			throw LOG.elasticsearchRequestFailed( jestAPIFormatter.formatRequest( request ), null, e );
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

				if ( backendRequest.getResultAssessor().isSuccess( resultItem ) ) {
					successfulItems.put( backendRequest, resultItem );
				}
				else {
					erroneousItems.add( backendRequest );
				}
				++i;
			}

			if ( !erroneousItems.isEmpty() ) {
				throw LOG.elasticsearchBulkRequestFailed(
						jestAPIFormatter.formatRequest( request ),
						jestAPIFormatter.formatResult( response ),
						successfulItems,
						erroneousItems
				);
			}
			else {
				return successfulItems;
			}
		}
		catch (IOException e) {
			throw LOG.elasticsearchRequestFailed( jestAPIFormatter.formatRequest( request ), null, e );
		}
	}
}
