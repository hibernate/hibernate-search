/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.sniff.ElasticsearchHostsSniffer;
import org.elasticsearch.client.sniff.HostsSniffer;
import org.elasticsearch.client.sniff.Sniffer;
import org.elasticsearch.client.sniff.SnifferBuilder;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.client.spi.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.elasticsearch.gson.impl.DefaultGsonProvider;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.SearchThreadFactory;

import com.google.gson.GsonBuilder;

/**
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchClientFactory implements ElasticsearchClientFactory, Startable, Stoppable {

	private ServiceManager serviceManager;

	@Override
	public void start(Properties properties, BuildContext context) {
		this.serviceManager = context.getServiceManager();
	}

	@Override
	public void stop() {
		this.serviceManager = null;
	}

	@Override
	public ElasticsearchClientImplementor create(Properties properties) {
		int requestTimeoutMs = ConfigurationParseHelper.getIntValue(
				properties,
				ElasticsearchEnvironment.SERVER_REQUEST_TIMEOUT,
				ElasticsearchEnvironment.Defaults.SERVER_REQUEST_TIMEOUT
		);

		RestClient restClient = createClient( properties, requestTimeoutMs );
		Sniffer sniffer = createSniffer( restClient, properties );

		boolean logPrettyPrinting = ConfigurationParseHelper.getBooleanValue( properties,
				ElasticsearchEnvironment.LOG_JSON_PRETTY_PRINTING, ElasticsearchEnvironment.Defaults.LOG_JSON_PRETTY_PRINTING );
		GsonProvider initialGsonProvider = DefaultGsonProvider.create( GsonBuilder::new, logPrettyPrinting );

		return new DefaultElasticsearchClient( restClient, sniffer, requestTimeoutMs, TimeUnit.MILLISECONDS, initialGsonProvider );
	}

	private RestClient createClient(Properties properties, int maxRetryTimeoutMillis) {
		String serverUrisString = ConfigurationParseHelper.getString(
				properties,
				ElasticsearchEnvironment.SERVER_URI,
				ElasticsearchEnvironment.Defaults.SERVER_URI
		);
		ServerUris hosts = ServerUris.fromString( serverUrisString );

		return RestClient.builder( hosts.asHostsArray() )
				/*
				 * Note: this timeout is currently only used on retries,
				 * but should we start using the synchronous methods of RestClient,
				 * it would be applied to synchronous requests too.
				 * See https://github.com/elastic/elasticsearch/issues/21789#issuecomment-287399115
				 */
				.setMaxRetryTimeoutMillis( maxRetryTimeoutMillis )
				.setRequestConfigCallback( (b) -> customizeRequestConfig( properties, b ) )
				.setHttpClientConfigCallback( (b) -> customizeHttpClientConfig( properties, hosts, b ) )
				.build();
	}

	private Sniffer createSniffer(RestClient client, Properties properties) {
		boolean discoveryEnabled = ConfigurationParseHelper.getBooleanValue(
				properties,
				ElasticsearchEnvironment.DISCOVERY_ENABLED,
				ElasticsearchEnvironment.Defaults.DISCOVERY_ENABLED
		);
		if ( discoveryEnabled ) {
			SnifferBuilder builder = Sniffer.builder( client )
					.setSniffIntervalMillis(
							ConfigurationParseHelper.getIntValue(
									properties,
									ElasticsearchEnvironment.DISCOVERY_REFRESH_INTERVAL,
									ElasticsearchEnvironment.Defaults.DISCOVERY_REFRESH_INTERVAL
							)
							* 1_000 // The configured value is in seconds
					);
			String scheme = ConfigurationParseHelper.getString(properties, ElasticsearchEnvironment.DISCOVERY_SCHEME, "http");

			// https discovery support
			if ( scheme.equals(ElasticsearchHostsSniffer.Scheme.HTTPS.toString()) ) {
				HostsSniffer hostsSniffer = new ElasticsearchHostsSniffer(
						client,
						ElasticsearchHostsSniffer.DEFAULT_SNIFF_REQUEST_TIMEOUT, // 1sec
						ElasticsearchHostsSniffer.Scheme.HTTPS );
				builder.setHostsSniffer( hostsSniffer );
			}
			return builder.build();
		}
		else {
			return null;
		}
	}

	private HttpAsyncClientBuilder customizeHttpClientConfig(Properties properties,
			ServerUris hosts, HttpAsyncClientBuilder builder) {
		builder = builder
				.setMaxConnTotal( ConfigurationParseHelper.getIntValue(
						properties,
						ElasticsearchEnvironment.MAX_TOTAL_CONNECTION,
						ElasticsearchEnvironment.Defaults.MAX_TOTAL_CONNECTION
				) )
				.setMaxConnPerRoute( ConfigurationParseHelper.getIntValue(
						properties,
						ElasticsearchEnvironment.MAX_TOTAL_CONNECTION_PER_ROUTE,
						ElasticsearchEnvironment.Defaults.MAX_TOTAL_CONNECTION_PER_ROUTE
				) )
				.setThreadFactory( new SearchThreadFactory( "Elasticsearch transport thread" ) );
		if ( hosts.isAnyRequiringSSL() == false ) {
			// In this case disable the SSL capability as it might have an impact on
			// bootstrap time, for example consuming entropy for no reason
			builder.setSSLStrategy( NoopIOSessionStrategy.INSTANCE );
		}

		String username = ConfigurationParseHelper.getString(
				properties,
				ElasticsearchEnvironment.SERVER_USERNAME,
				null
		);
		if ( username != null ) {
			String password = ConfigurationParseHelper.getString(
					properties,
					ElasticsearchEnvironment.SERVER_PASSWORD,
					null
			);
			if ( password != null ) {
				hosts.warnPasswordsOverHttp();
			}

			BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(
					new AuthScope( AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME ),
					new UsernamePasswordCredentials( username, password )
					);

			builder = builder.setDefaultCredentialsProvider( credentialsProvider );
		}

		Iterable<ElasticsearchHttpClientConfigurer> configurers =
				serviceManager.getClassLoaderService().loadJavaServices( ElasticsearchHttpClientConfigurer.class );
		for ( ElasticsearchHttpClientConfigurer configurer : configurers ) {
			configurer.configure( builder, properties );
		}

		return builder;
	}

	private RequestConfig.Builder customizeRequestConfig(Properties properties, RequestConfig.Builder builder) {
		return builder
				.setConnectionRequestTimeout( 0 ) //Disable lease handling for the connection pool! See also HSEARCH-2681
				.setSocketTimeout( ConfigurationParseHelper.getIntValue(
						properties,
						ElasticsearchEnvironment.SERVER_READ_TIMEOUT,
						ElasticsearchEnvironment.Defaults.SERVER_READ_TIMEOUT
				) )
				.setConnectTimeout( ConfigurationParseHelper.getIntValue(
						properties,
						ElasticsearchEnvironment.SERVER_CONNECTION_TIMEOUT,
						ElasticsearchEnvironment.Defaults.SERVER_CONNECTION_TIMEOUT
				) );
	}

}
