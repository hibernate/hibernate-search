/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

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
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.SearchThreadFactory;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchClientFactory implements ElasticsearchClientFactory {

	private static final Log log = LoggerFactory.make( Log.class );

	private static final String HTTP_SCHEME = "http";

	/**
	 * Prefix for accessing the client-related settings.
	 * To be suffixed by a scope (currently always "default")
	 * and the name of the property to access.
	 */
	private static final String CLIENT_PROP_PREFIX = "hibernate.search.";

	@Override
	public RestClient createClient(String scopeName, Properties properties) {
		String propertyPrefix = propertyPrefix( scopeName );

		String serverUrisString = ConfigurationParseHelper.getString(
				properties,
				propertyPrefix + ElasticsearchEnvironment.SERVER_URI,
				ElasticsearchEnvironment.Defaults.SERVER_URI
		);

		String[] serverUris = serverUrisString.trim().split( "\\s" );
		HttpHost[] hosts = new HttpHost[serverUris.length];
		for ( int i = 0 ; i < serverUris.length ; ++i ) {
			hosts[i] = HttpHost.create( serverUris[i] );
		}

		return RestClient.builder( hosts )
				.setRequestConfigCallback( (b) -> customizeRequestConfig( propertyPrefix, properties, b ) )
				.setHttpClientConfigCallback( (b) -> customizeHttpClientConfig( propertyPrefix, properties, serverUris, b ) )
				.build();
	}

	@Override
	public Sniffer createSniffer(String scopeName, RestClient client, Properties properties) {
		String propertyPrefix = propertyPrefix( scopeName );

		boolean discoveryEnabled = ConfigurationParseHelper.getBooleanValue(
				properties,
				propertyPrefix + ElasticsearchEnvironment.DISCOVERY_ENABLED,
				ElasticsearchEnvironment.Defaults.DISCOVERY_ENABLED
		);
		if ( discoveryEnabled ) {
			return Sniffer.builder( client )
					.setSniffIntervalMillis(
							ConfigurationParseHelper.getIntValue(
									properties,
									propertyPrefix + ElasticsearchEnvironment.DISCOVERY_REFRESH_INTERVAL,
									ElasticsearchEnvironment.Defaults.DISCOVERY_REFRESH_INTERVAL
							)
							* 1_000 // The configured value is in seconds
					)
					.build();
		}
		else {
			return null;
		}
	}

	private HttpAsyncClientBuilder customizeHttpClientConfig(String propertyPrefix,
			Properties properties, String[] serverUris, HttpAsyncClientBuilder builder) {
		builder = builder
				.setMaxConnTotal( ConfigurationParseHelper.getIntValue(
						properties,
						propertyPrefix + ElasticsearchEnvironment.MAX_TOTAL_CONNECTION,
						ElasticsearchEnvironment.Defaults.MAX_TOTAL_CONNECTION
				) )
				.setMaxConnPerRoute( ConfigurationParseHelper.getIntValue(
						properties,
						propertyPrefix + ElasticsearchEnvironment.MAX_TOTAL_CONNECTION_PER_ROUTE,
						ElasticsearchEnvironment.Defaults.MAX_TOTAL_CONNECTION_PER_ROUTE
				) )
				.setThreadFactory( new SearchThreadFactory( "Elasticsearch transport thread" ) );

		String username = ConfigurationParseHelper.getString(
				properties,
				propertyPrefix + ElasticsearchEnvironment.SERVER_USERNAME,
				null
		);
		if ( username != null ) {
			String password = ConfigurationParseHelper.getString(
					properties,
					propertyPrefix + ElasticsearchEnvironment.SERVER_PASSWORD,
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

	private RequestConfig.Builder customizeRequestConfig(String propertyPrefix, Properties properties, RequestConfig.Builder builder) {
		return builder
				.setSocketTimeout( ConfigurationParseHelper.getIntValue(
						properties,
						propertyPrefix + ElasticsearchEnvironment.SERVER_READ_TIMEOUT,
						ElasticsearchEnvironment.Defaults.SERVER_READ_TIMEOUT
				) )
				.setConnectTimeout( ConfigurationParseHelper.getIntValue(
						properties,
						propertyPrefix + ElasticsearchEnvironment.SERVER_CONNECTION_TIMEOUT,
						ElasticsearchEnvironment.Defaults.SERVER_CONNECTION_TIMEOUT
				) );
	}

	private String propertyPrefix(String scopeName) {
		return new StringBuilder( CLIENT_PROP_PREFIX )
				.append( scopeName ).append( "." )
				.toString();
	}

	private boolean warnPasswordsOverHttp(String[] serverUris) {
		for ( String serverUriAsString : serverUris ) {
			URI uri = URI.create( serverUriAsString );
			if ( HTTP_SCHEME.equals( uri.getScheme() ) ) {
				log.usingPasswordOverHttp( serverUriAsString );
			}
		}
		return false;
	}
}
