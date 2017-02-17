/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Provides access to the JEST client.
 *
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class ElasticsearchServiceImpl implements ElasticsearchService, Startable, Stoppable {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final String HTTP_SCHEME = "http";

	/**
	 * Prefix for accessing the client-related settings.
	 * That's currently needed as we only have a single client for all
	 * index managers.
	 * The prefix is used to have the property name in line with the other
	 * index-related property names, even though this property can not yet
	 * be overridden on a per-index basis.
	 */
	private static final String CLIENT_PROP_PREFIX = "hibernate.search.default.";

	private RestClient client;

	private Sniffer sniffer;

	@Override
	public void start(Properties properties, BuildContext context) {
		String serverUrisString = ConfigurationParseHelper.getString(
				properties,
				CLIENT_PROP_PREFIX + ElasticsearchEnvironment.SERVER_URI,
				ElasticsearchEnvironment.Defaults.SERVER_URI
		);

		String[] serverUris = serverUrisString.trim().split( "\\s" );
		HttpHost[] hosts = new HttpHost[serverUris.length];
		for ( int i = 0 ; i < serverUris.length ; ++i ) {
			hosts[i] = HttpHost.create( serverUris[i] );
		}

		this.client = RestClient.builder( hosts )
				.setRequestConfigCallback( (b) -> customizeRequestConfig( properties, b ) )
				.setHttpClientConfigCallback( (b) -> customizeHttpClientConfig( properties, serverUris, b ) )
				.build();

		boolean discoveryEnabled = ConfigurationParseHelper.getBooleanValue(
				properties,
				CLIENT_PROP_PREFIX + ElasticsearchEnvironment.DISCOVERY_ENABLED,
				ElasticsearchEnvironment.Defaults.DISCOVERY_ENABLED
		);
		if ( discoveryEnabled ) {
			this.sniffer = Sniffer.builder( client )
					.setSniffIntervalMillis(
							ConfigurationParseHelper.getIntValue(
									properties,
									CLIENT_PROP_PREFIX + ElasticsearchEnvironment.DISCOVERY_REFRESH_INTERVAL,
									ElasticsearchEnvironment.Defaults.DISCOVERY_REFRESH_INTERVAL
							)
							* 1_000 // The configured value is in seconds
					)
					.build();
		}

	}

	private HttpAsyncClientBuilder customizeHttpClientConfig(Properties properties, String[] serverUris, HttpAsyncClientBuilder builder) {
		builder = builder
				.setMaxConnTotal( ConfigurationParseHelper.getIntValue(
						properties,
						CLIENT_PROP_PREFIX + ElasticsearchEnvironment.MAX_TOTAL_CONNECTION,
						ElasticsearchEnvironment.Defaults.MAX_TOTAL_CONNECTION
				) )
				.setMaxConnPerRoute( ConfigurationParseHelper.getIntValue(
						properties,
						CLIENT_PROP_PREFIX + ElasticsearchEnvironment.MAX_TOTAL_CONNECTION_PER_ROUTE,
						ElasticsearchEnvironment.Defaults.MAX_TOTAL_CONNECTION_PER_ROUTE
				) );

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
			if ( password != null ) {
				warnPasswordsOverHttp( serverUris );
			}

			BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(
					new AuthScope( AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME ),
					new UsernamePasswordCredentials( username, password )
					);

			builder = builder.setDefaultCredentialsProvider( credentialsProvider );
		}

		return builder;
	}

	private RequestConfig.Builder customizeRequestConfig(Properties properties, RequestConfig.Builder builder) {
		return builder
				.setSocketTimeout( ConfigurationParseHelper.getIntValue(
						properties,
						CLIENT_PROP_PREFIX + ElasticsearchEnvironment.SERVER_READ_TIMEOUT,
						ElasticsearchEnvironment.Defaults.SERVER_READ_TIMEOUT
				) )
				.setConnectTimeout( ConfigurationParseHelper.getIntValue(
						properties,
						CLIENT_PROP_PREFIX + ElasticsearchEnvironment.SERVER_CONNECTION_TIMEOUT,
						ElasticsearchEnvironment.Defaults.SERVER_CONNECTION_TIMEOUT
				) );
	}

	private boolean warnPasswordsOverHttp(String[] serverUris) {
		for ( String serverUriAsString : serverUris ) {
			URI uri = URI.create( serverUriAsString );
			if ( HTTP_SCHEME.equals( uri.getScheme() ) ) {
				LOG.usingPasswordOverHttp( serverUriAsString );
			}
		}
		return false;
	}

	@Override
	public void stop() {
		try ( RestClient client = this.client;
				Sniffer sniffer = this.sniffer ) {
			/*
			 * Nothing to do: we simply take advantage of Java's auto-closing,
			 * which adds suppressed exceptions as needed and always tries
			 * to close every resource.
			 */
		}
		catch (IOException | RuntimeException e) {
			throw new SearchException( "Failed to shut down the Elasticsearch service", e );
		}
	}

	@Override
	public RestClient getClient() {
		return client;
	}
}
