/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.sniff.ElasticsearchNodesSniffer;
import org.elasticsearch.client.sniff.NodesSniffer;
import org.elasticsearch.client.sniff.Sniffer;
import org.elasticsearch.client.sniff.SnifferBuilder;

/**
 * @author Gunnar Morling
 */
public class ElasticsearchClientFactoryImpl implements ElasticsearchClientFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final OptionalConfigurationProperty<BeanReference<? extends RestClient>> CLIENT_INSTANCE =
			ConfigurationProperty.forKey( ElasticsearchBackendSpiSettings.CLIENT_INSTANCE )
					.asBeanReference( RestClient.class )
					.build();

	private static final OptionalConfigurationProperty<List<String>> HOSTS =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.HOSTS )
					.asString().multivalued()
					.build();

	private static final OptionalConfigurationProperty<String> PROTOCOL =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.PROTOCOL )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<List<String>> URIS =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.URIS )
					.asString().multivalued()
					.build();

	private static final ConfigurationProperty<String> PATH_PREFIX =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.PATH_PREFIX )
					.asString()
					.withDefault( ElasticsearchBackendSettings.Defaults.PATH_PREFIX )
					.build();

	private static final OptionalConfigurationProperty<String> USERNAME =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.USERNAME )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> PASSWORD =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.PASSWORD )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<Integer> REQUEST_TIMEOUT =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.REQUEST_TIMEOUT )
					.asIntegerStrictlyPositive()
					.build();

	private static final ConfigurationProperty<Integer> READ_TIMEOUT =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.READ_TIMEOUT )
					.asIntegerPositiveOrZeroOrNegative()
					.withDefault( ElasticsearchBackendSettings.Defaults.READ_TIMEOUT )
					.build();

	private static final ConfigurationProperty<Integer> CONNECTION_TIMEOUT =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.CONNECTION_TIMEOUT )
					.asIntegerPositiveOrZeroOrNegative()
					.withDefault( ElasticsearchBackendSettings.Defaults.CONNECTION_TIMEOUT )
					.build();

	private static final ConfigurationProperty<Integer> MAX_TOTAL_CONNECTION =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.MAX_CONNECTIONS )
					.asIntegerStrictlyPositive()
					.withDefault( ElasticsearchBackendSettings.Defaults.MAX_CONNECTIONS )
					.build();

	private static final ConfigurationProperty<Integer> MAX_TOTAL_CONNECTION_PER_ROUTE =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.MAX_CONNECTIONS_PER_ROUTE )
					.asIntegerStrictlyPositive()
					.withDefault( ElasticsearchBackendSettings.Defaults.MAX_CONNECTIONS_PER_ROUTE )
					.build();

	private static final ConfigurationProperty<Boolean> DISCOVERY_ENABLED =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.DISCOVERY_ENABLED )
					.asBoolean()
					.withDefault( ElasticsearchBackendSettings.Defaults.DISCOVERY_ENABLED )
					.build();

	private static final ConfigurationProperty<Integer> DISCOVERY_REFRESH_INTERVAL =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.DISCOVERY_REFRESH_INTERVAL )
					.asIntegerStrictlyPositive()
					.withDefault( ElasticsearchBackendSettings.Defaults.DISCOVERY_REFRESH_INTERVAL )
					.build();

	private static final OptionalConfigurationProperty<
			BeanReference<? extends ElasticsearchHttpClientConfigurer>> CLIENT_CONFIGURER =
					ConfigurationProperty.forKey( ElasticsearchBackendSettings.CLIENT_CONFIGURER )
							.asBeanReference( ElasticsearchHttpClientConfigurer.class )
							.build();

	private static final OptionalConfigurationProperty<Long> MAX_KEEP_ALIVE =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.MAX_KEEP_ALIVE )
					.asLongStrictlyPositive()
					.build();

	@Override
	public ElasticsearchClientImplementor create(BeanResolver beanResolver, ConfigurationPropertySource propertySource,
			ThreadProvider threadProvider, String threadNamePrefix,
			SimpleScheduledExecutor timeoutExecutorService,
			GsonProvider gsonProvider) {
		Optional<Integer> requestTimeoutMs = REQUEST_TIMEOUT.get( propertySource );
		int connectionTimeoutMs = CONNECTION_TIMEOUT.get( propertySource );

		Optional<BeanHolder<? extends RestClient>> providedRestClientHolder = CLIENT_INSTANCE.getAndMap(
				propertySource, beanResolver::resolve );

		BeanHolder<? extends RestClient> restClientHolder;
		Sniffer sniffer;
		if ( providedRestClientHolder.isPresent() ) {
			restClientHolder = providedRestClientHolder.get();
			sniffer = null;
		}
		else {
			ServerUris hosts = ServerUris.fromOptionalStrings( PROTOCOL.get( propertySource ),
					HOSTS.get( propertySource ), URIS.get( propertySource ) );
			restClientHolder = createClient( beanResolver, propertySource, threadProvider, threadNamePrefix, hosts,
					PATH_PREFIX.get( propertySource ) );
			sniffer = createSniffer( propertySource, restClientHolder.get(), hosts );
		}

		return new ElasticsearchClientImpl(
				restClientHolder, sniffer, timeoutExecutorService,
				requestTimeoutMs, connectionTimeoutMs,
				gsonProvider.getGson(), gsonProvider.getLogHelper()
		);
	}

	private BeanHolder<? extends RestClient> createClient(BeanResolver beanResolver, ConfigurationPropertySource propertySource,
			ThreadProvider threadProvider, String threadNamePrefix,
			ServerUris hosts, String pathPrefix) {
		RestClientBuilder builder = RestClient.builder( hosts.asHostsArray() );
		if ( !pathPrefix.isEmpty() ) {
			builder.setPathPrefix( pathPrefix );
		}

		Optional<? extends BeanHolder<? extends ElasticsearchHttpClientConfigurer>> customConfig = CLIENT_CONFIGURER
				.getAndMap( propertySource, beanResolver::resolve );

		RestClient client = null;
		List<BeanReference<ElasticsearchHttpClientConfigurer>> httpClientConfigurerReferences =
				beanResolver.allConfiguredForRole( ElasticsearchHttpClientConfigurer.class );
		try ( BeanHolder<List<ElasticsearchHttpClientConfigurer>> httpClientConfigurersHolder =
				beanResolver.resolve( httpClientConfigurerReferences ) ) {
			client = builder
					.setRequestConfigCallback( b -> customizeRequestConfig( b, propertySource ) )
					.setHttpClientConfigCallback(
							b -> customizeHttpClientConfig(
									b,
									beanResolver, propertySource,
									threadProvider, threadNamePrefix,
									hosts,
									httpClientConfigurersHolder.get(), customConfig
							)
					)
					.build();
			return BeanHolder.ofCloseable( client );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( client );
			throw e;
		}
		finally {
			if ( customConfig.isPresent() ) {
				// Assuming that #customizeHttpClientConfig has been already executed
				// and therefore the bean has been already used.
				customConfig.get().close();
			}
		}
	}

	private Sniffer createSniffer(ConfigurationPropertySource propertySource,
			RestClient client, ServerUris hosts) {
		boolean discoveryEnabled = DISCOVERY_ENABLED.get( propertySource );
		if ( discoveryEnabled ) {
			SnifferBuilder builder = Sniffer.builder( client )
					.setSniffIntervalMillis(
							DISCOVERY_REFRESH_INTERVAL.get( propertySource ) * 1_000 // The configured value is in seconds
					);

			// https discovery support
			if ( hosts.isSslEnabled() ) {
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
			BeanResolver beanResolver, ConfigurationPropertySource propertySource,
			ThreadProvider threadProvider, String threadNamePrefix,
			ServerUris hosts, Iterable<ElasticsearchHttpClientConfigurer> configurers,
			Optional<? extends BeanHolder<? extends ElasticsearchHttpClientConfigurer>> customConfig) {
		builder.setMaxConnTotal( MAX_TOTAL_CONNECTION.get( propertySource ) )
				.setMaxConnPerRoute( MAX_TOTAL_CONNECTION_PER_ROUTE.get( propertySource ) )
				.setThreadFactory( threadProvider.createThreadFactory( threadNamePrefix + " - Transport thread" ) );
		if ( !hosts.isSslEnabled() ) {
			// In this case disable the SSL capability as it might have an impact on
			// bootstrap time, for example consuming entropy for no reason
			builder.setSSLStrategy( NoopIOSessionStrategy.INSTANCE );
		}

		Optional<String> username = USERNAME.get( propertySource );
		if ( username.isPresent() ) {
			Optional<String> password = PASSWORD.get( propertySource );
			if ( password.isPresent() && !hosts.isSslEnabled() ) {
				log.usingPasswordOverHttp();
			}

			BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(
					new AuthScope( AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME ),
					new UsernamePasswordCredentials( username.get(), password.orElse( null ) )
			);

			builder.setDefaultCredentialsProvider( credentialsProvider );
		}

		Optional<Long> maxKeepAlive = MAX_KEEP_ALIVE.get( propertySource );
		if ( maxKeepAlive.isPresent() ) {
			builder.setKeepAliveStrategy( new CustomConnectionKeepAliveStrategy( maxKeepAlive.get() ) );
		}

		ElasticsearchHttpClientConfigurationContextImpl clientConfigurationContext =
				new ElasticsearchHttpClientConfigurationContextImpl( beanResolver, propertySource, builder );

		for ( ElasticsearchHttpClientConfigurer configurer : configurers ) {
			configurer.configure( clientConfigurationContext );
		}
		if ( customConfig.isPresent() ) {
			BeanHolder<? extends ElasticsearchHttpClientConfigurer> customConfigBeanHolder = customConfig.get();
			customConfigBeanHolder.get().configure( clientConfigurationContext );
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
