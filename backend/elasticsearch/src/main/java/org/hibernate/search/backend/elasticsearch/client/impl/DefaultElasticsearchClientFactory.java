/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.cfg.SearchBackendElasticsearchSettings;
import org.hibernate.search.backend.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.util.impl.common.SearchThreadFactory;

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

/**
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchClientFactory implements ElasticsearchClientFactory {

	private static final ConfigurationProperty<List<String>> HOST =
			ConfigurationProperty.forKey( SearchBackendElasticsearchSettings.HOST )
					.asString().multivalued( Pattern.compile( "\\s+" ) )
					.withDefault( SearchBackendElasticsearchSettings.Defaults.HOST )
					.build();

	private static final ConfigurationProperty<Optional<String>> USERNAME =
			ConfigurationProperty.forKey( SearchBackendElasticsearchSettings.USERNAME )
					.asString()
					.build();

	private static final ConfigurationProperty<Optional<String>> PASSWORD =
			ConfigurationProperty.forKey( SearchBackendElasticsearchSettings.PASSWORD )
					.asString()
					.build();

	private static final ConfigurationProperty<Integer> REQUEST_TIMEOUT =
			ConfigurationProperty.forKey( SearchBackendElasticsearchSettings.REQUEST_TIMEOUT )
					.asInteger()
					.withDefault( SearchBackendElasticsearchSettings.Defaults.REQUEST_TIMEOUT )
					.build();

	private static final ConfigurationProperty<Integer> READ_TIMEOUT =
			ConfigurationProperty.forKey( SearchBackendElasticsearchSettings.READ_TIMEOUT )
					.asInteger()
					.withDefault( SearchBackendElasticsearchSettings.Defaults.READ_TIMEOUT )
					.build();

	private static final ConfigurationProperty<Integer> CONNECTION_TIMEOUT =
			ConfigurationProperty.forKey( SearchBackendElasticsearchSettings.CONNECTION_TIMEOUT )
					.asInteger()
					.withDefault( SearchBackendElasticsearchSettings.Defaults.CONNECTION_TIMEOUT )
					.build();

	private static final ConfigurationProperty<Integer> MAX_TOTAL_CONNECTION =
			ConfigurationProperty.forKey( SearchBackendElasticsearchSettings.MAX_TOTAL_CONNECTION )
					.asInteger()
					.withDefault( SearchBackendElasticsearchSettings.Defaults.MAX_TOTAL_CONNECTION )
					.build();

	private static final ConfigurationProperty<Integer> MAX_TOTAL_CONNECTION_PER_ROUTE =
			ConfigurationProperty.forKey( SearchBackendElasticsearchSettings.MAX_TOTAL_CONNECTION_PER_ROUTE )
					.asInteger()
					.withDefault( SearchBackendElasticsearchSettings.Defaults.MAX_TOTAL_CONNECTION_PER_ROUTE )
					.build();

	private static final ConfigurationProperty<Boolean> DISCOVERY_ENABLED =
			ConfigurationProperty.forKey( SearchBackendElasticsearchSettings.DISCOVERY_ENABLED )
					.asBoolean()
					.withDefault( SearchBackendElasticsearchSettings.Defaults.DISCOVERY_ENABLED )
					.build();

	private static final ConfigurationProperty<Integer> DISCOVERY_REFRESH_INTERVAL =
			ConfigurationProperty.forKey( SearchBackendElasticsearchSettings.DISCOVERY_REFRESH_INTERVAL )
					.asInteger()
					.withDefault( SearchBackendElasticsearchSettings.Defaults.DISCOVERY_REFRESH_INTERVAL )
					.build();

	private static final ConfigurationProperty<String> DISCOVERY_SCHEME =
			ConfigurationProperty.forKey( SearchBackendElasticsearchSettings.DISCOVERY_SCHEME )
					.asString()
					.withDefault( SearchBackendElasticsearchSettings.Defaults.DISCOVERY_SCHEME )
					.build();

	/* TODO ElasticsearchHttpClientConfigurer
	private ServiceManager serviceManager;

	@Override
	public void start(ConfigurationPropertySource propertySource, BackendBuildContext context) {
		this.serviceManager = context.getServiceManager();
	}

	@Override
	public void stop() {
		this.serviceManager = null;
	}
	*/

	@Override
	public ElasticsearchClientImplementor create(ConfigurationPropertySource propertySource,
			GsonProvider initialGsonProvider) {
		int requestTimeoutMs = REQUEST_TIMEOUT.get( propertySource );

		RestClient restClient = createClient( propertySource, requestTimeoutMs );
		Sniffer sniffer = createSniffer( restClient, propertySource );

		return new DefaultElasticsearchClient( restClient, sniffer, requestTimeoutMs, TimeUnit.MILLISECONDS, initialGsonProvider );
	}

	private RestClient createClient(ConfigurationPropertySource propertySource, int maxRetryTimeoutMillis) {
		ServerUris hosts = ServerUris.fromStrings( HOST.get( propertySource ) );

		return RestClient.builder( hosts.asHostsArray() )
				/*
				 * Note: this timeout is currently only used on retries,
				 * but should we start using the synchronous methods of RestClient,
				 * it would be applied to synchronous requests too.
				 * See https://github.com/elastic/elasticsearch/issues/21789#issuecomment-287399115
				 */
				.setMaxRetryTimeoutMillis( maxRetryTimeoutMillis )
				.setRequestConfigCallback( b -> customizeRequestConfig( propertySource, b ) )
				.setHttpClientConfigCallback( b -> customizeHttpClientConfig( propertySource, hosts, b ) )
				.build();
	}

	private Sniffer createSniffer(RestClient client, ConfigurationPropertySource propertySource) {
		boolean discoveryEnabled = DISCOVERY_ENABLED.get( propertySource );
		if ( discoveryEnabled ) {
			SnifferBuilder builder = Sniffer.builder( client )
					.setSniffIntervalMillis(
							DISCOVERY_REFRESH_INTERVAL.get( propertySource )
							* 1_000 // The configured value is in seconds
					);
			String scheme = DISCOVERY_SCHEME.get( propertySource );

			// https discovery support
			if ( scheme.equals( ElasticsearchHostsSniffer.Scheme.HTTPS.toString() ) ) {
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

	private HttpAsyncClientBuilder customizeHttpClientConfig(ConfigurationPropertySource propertySource,
			ServerUris hosts, HttpAsyncClientBuilder builder) {
		builder = builder
				.setMaxConnTotal( MAX_TOTAL_CONNECTION.get( propertySource ) )
				.setMaxConnPerRoute( MAX_TOTAL_CONNECTION_PER_ROUTE.get( propertySource ) )
				.setThreadFactory( new SearchThreadFactory( "Elasticsearch transport thread" ) );
		if ( !hosts.isAnyRequiringSSL() ) {
			// In this case disable the SSL capability as it might have an impact on
			// bootstrap time, for example consuming entropy for no reason
			builder.setSSLStrategy( NoopIOSessionStrategy.INSTANCE );
		}

		Optional<String> username = USERNAME.get( propertySource );
		if ( username.isPresent() ) {
			Optional<String> password = PASSWORD.get( propertySource );
			if ( password.isPresent() ) {
				hosts.warnPasswordsOverHttp();
			}

			BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(
					new AuthScope( AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME ),
					new UsernamePasswordCredentials( username.get(), password.orElse( null ) )
					);

			builder = builder.setDefaultCredentialsProvider( credentialsProvider );
		}

		/* TODO ElasticsearchHttpClientConfigurer
		Iterable<ElasticsearchHttpClientConfigurer> configurers =
				serviceManager.getClassLoaderService().loadJavaServices( ElasticsearchHttpClientConfigurer.class );
		for ( ElasticsearchHttpClientConfigurer configurer : configurers ) {
			configurer.configure( builder, properties );
		}
		*/

		return builder;
	}

	private RequestConfig.Builder customizeRequestConfig(ConfigurationPropertySource propertySource,
			RequestConfig.Builder builder) {
		return builder
				.setConnectionRequestTimeout( 0 ) //Disable lease handling for the connection pool! See also HSEARCH-2681
				.setSocketTimeout( READ_TIMEOUT.get( propertySource ) )
				.setConnectTimeout( CONNECTION_TIMEOUT.get( propertySource ) );
	}

}
