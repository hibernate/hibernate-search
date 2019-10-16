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

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.sniff.ElasticsearchNodesSniffer;
import org.elasticsearch.client.sniff.NodesSniffer;
import org.elasticsearch.client.sniff.Sniffer;
import org.elasticsearch.client.sniff.SnifferBuilder;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;

/**
 * @author Gunnar Morling
 */
public class ElasticsearchClientFactoryImpl implements ElasticsearchClientFactory {

	public static final BeanReference<ElasticsearchClientFactory> REFERENCE = (BeanResolver beanResolver) -> {
		BeanHolder<List<ElasticsearchHttpClientConfigurer>> httpClientConfigurerHolders =
			beanResolver.resolveRole( ElasticsearchHttpClientConfigurer.class );
		ElasticsearchClientFactoryImpl factory = new ElasticsearchClientFactoryImpl( httpClientConfigurerHolders.get() );
		return BeanHolder.<ElasticsearchClientFactory>of( factory )
			.withDependencyAutoClosing( httpClientConfigurerHolders );
	};

	private static final ConfigurationProperty<List<String>> HOST =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.HOSTS )
					.asString().multivalued( Pattern.compile( "\\s+" ) )
					.withDefault( ElasticsearchBackendSettings.Defaults.HOSTS )
					.build();

	private static final OptionalConfigurationProperty<String> USERNAME =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.USERNAME )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> PASSWORD =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.PASSWORD )
					.asString()
					.build();

	private static final ConfigurationProperty<Integer> REQUEST_TIMEOUT =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.REQUEST_TIMEOUT )
					.asInteger()
					.withDefault( ElasticsearchBackendSettings.Defaults.REQUEST_TIMEOUT )
					.build();

	private static final ConfigurationProperty<Integer> READ_TIMEOUT =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.READ_TIMEOUT )
					.asInteger()
					.withDefault( ElasticsearchBackendSettings.Defaults.READ_TIMEOUT )
					.build();

	private static final ConfigurationProperty<Integer> CONNECTION_TIMEOUT =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.CONNECTION_TIMEOUT )
					.asInteger()
					.withDefault( ElasticsearchBackendSettings.Defaults.CONNECTION_TIMEOUT )
					.build();

	private static final ConfigurationProperty<Integer> MAX_TOTAL_CONNECTION =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.MAX_CONNECTIONS )
					.asInteger()
					.withDefault( ElasticsearchBackendSettings.Defaults.MAX_CONNECTIONS )
					.build();

	private static final ConfigurationProperty<Integer> MAX_TOTAL_CONNECTION_PER_ROUTE =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.MAX_CONNECTIONS_PER_ROUTE )
					.asInteger()
					.withDefault( ElasticsearchBackendSettings.Defaults.MAX_CONNECTIONS_PER_ROUTE )
					.build();

	private static final ConfigurationProperty<Boolean> DISCOVERY_ENABLED =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.DISCOVERY_ENABLED )
					.asBoolean()
					.withDefault( ElasticsearchBackendSettings.Defaults.DISCOVERY_ENABLED )
					.build();

	private static final ConfigurationProperty<Integer> DISCOVERY_REFRESH_INTERVAL =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.DISCOVERY_REFRESH_INTERVAL )
					.asInteger()
					.withDefault( ElasticsearchBackendSettings.Defaults.DISCOVERY_REFRESH_INTERVAL )
					.build();

	private static final ConfigurationProperty<String> DISCOVERY_SCHEME =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.DISCOVERY_SCHEME )
					.asString()
					.withDefault( ElasticsearchBackendSettings.Defaults.DISCOVERY_SCHEME )
					.build();

	private final List<ElasticsearchHttpClientConfigurer> httpClientConfigurers;

	ElasticsearchClientFactoryImpl(List<ElasticsearchHttpClientConfigurer> httpClientConfigurers) {
		this.httpClientConfigurers = httpClientConfigurers;
	}

	@Override
	public ElasticsearchClientImplementor create(ConfigurationPropertySource propertySource,
			ThreadPoolProvider threadPoolProvider, GsonProvider gsonProvider) {
		int requestTimeoutMs = REQUEST_TIMEOUT.get( propertySource );

		RestClient restClient = createClient( propertySource, threadPoolProvider.getThreadProvider() );
		Sniffer sniffer = createSniffer( restClient, propertySource );

		return new ElasticsearchClientImpl(
				restClient, sniffer, threadPoolProvider,
				requestTimeoutMs, TimeUnit.MILLISECONDS,
				gsonProvider.getGson(), gsonProvider.getLogHelper()
		);
	}

	private RestClient createClient(ConfigurationPropertySource propertySource,
			ThreadProvider threadProvider) {
		ServerUris hosts = ServerUris.fromStrings( HOST.get( propertySource ) );

		return RestClient.builder( hosts.asHostsArray() )
				.setRequestConfigCallback( b -> customizeRequestConfig( b, propertySource ) )
				.setHttpClientConfigCallback(
						b -> customizeHttpClientConfig( b, httpClientConfigurers, propertySource, hosts, threadProvider )
				)
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
			if ( scheme.equals( ElasticsearchNodesSniffer.Scheme.HTTPS.toString() ) ) {
				NodesSniffer hostsSniffer = new ElasticsearchNodesSniffer(
						client,
						ElasticsearchNodesSniffer.DEFAULT_SNIFF_REQUEST_TIMEOUT, // 1sec
						ElasticsearchNodesSniffer.Scheme.HTTPS );
				builder.setNodesSniffer( hostsSniffer );
			}
			return builder.build();
		}
		else {
			return null;
		}
	}

	private HttpAsyncClientBuilder customizeHttpClientConfig(HttpAsyncClientBuilder builder,
			Iterable<ElasticsearchHttpClientConfigurer> configurers,
			ConfigurationPropertySource propertySource, ServerUris hosts,
			ThreadProvider threadProvider) {
		builder.setMaxConnTotal( MAX_TOTAL_CONNECTION.get( propertySource ) )
				.setMaxConnPerRoute( MAX_TOTAL_CONNECTION_PER_ROUTE.get( propertySource ) )
				.setThreadFactory( threadProvider.createThreadFactory( "Elasticsearch transport thread" ) );
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

			builder.setDefaultCredentialsProvider( credentialsProvider );
		}

		for ( ElasticsearchHttpClientConfigurer configurer : configurers ) {
			configurer.configure( builder, propertySource );
		}

		return builder;
	}

	private RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder builder,
			ConfigurationPropertySource propertySource) {
		return builder
				.setConnectionRequestTimeout( 0 ) //Disable lease handling for the connection pool! See also HSEARCH-2681
				.setSocketTimeout( READ_TIMEOUT.get( propertySource ) )
				.setConnectTimeout( CONNECTION_TIMEOUT.get( propertySource ) );
	}

}
