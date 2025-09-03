/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.elasticsearch.lowlevel.impl;

import java.net.SocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.elasticsearch.client.common.cfg.ElasticsearchBackendClientCommonSettings;
import org.hibernate.search.backend.elasticsearch.client.common.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.client.common.logging.spi.ElasticsearchClientConfigurationLog;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.elasticsearch.lowlevel.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.backend.elasticsearch.client.elasticsearch.lowlevel.cfg.ElasticsearchBackendClientSettings;
import org.hibernate.search.backend.elasticsearch.client.elasticsearch.lowlevel.cfg.spi.ElasticsearchBackendClientSpiSettings;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.util.common.impl.SuppressingCloser;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.net.NamedEndpoint;
import org.apache.hc.core5.reactor.ssl.TransportSecurityLayer;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.sniff.NodesSniffer;
import org.opensearch.client.sniff.OpenSearchNodesSniffer;
import org.opensearch.client.sniff.Sniffer;
import org.opensearch.client.sniff.SnifferBuilder;


/**
 * @author Gunnar Morling
 */
public class ElasticsearchClientFactoryImpl implements ElasticsearchClientFactory {

	private static final OptionalConfigurationProperty<BeanReference<? extends RestClient>> CLIENT_INSTANCE =
			ConfigurationProperty.forKey( ElasticsearchBackendClientSpiSettings.CLIENT_INSTANCE )
					.asBeanReference( RestClient.class )
					.build();

	private static final OptionalConfigurationProperty<List<String>> HOSTS =
			ConfigurationProperty.forKey( ElasticsearchBackendClientCommonSettings.HOSTS )
					.asString().multivalued()
					.build();

	private static final OptionalConfigurationProperty<String> PROTOCOL =
			ConfigurationProperty.forKey( ElasticsearchBackendClientCommonSettings.PROTOCOL )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<List<String>> URIS =
			ConfigurationProperty.forKey( ElasticsearchBackendClientCommonSettings.URIS )
					.asString().multivalued()
					.build();

	private static final ConfigurationProperty<String> PATH_PREFIX =
			ConfigurationProperty.forKey( ElasticsearchBackendClientCommonSettings.PATH_PREFIX )
					.asString()
					.withDefault( ElasticsearchBackendClientCommonSettings.Defaults.PATH_PREFIX )
					.build();

	private static final OptionalConfigurationProperty<String> USERNAME =
			ConfigurationProperty.forKey( ElasticsearchBackendClientCommonSettings.USERNAME )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> PASSWORD =
			ConfigurationProperty.forKey( ElasticsearchBackendClientCommonSettings.PASSWORD )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<Integer> REQUEST_TIMEOUT =
			ConfigurationProperty.forKey( ElasticsearchBackendClientCommonSettings.REQUEST_TIMEOUT )
					.asIntegerStrictlyPositive()
					.build();

	private static final ConfigurationProperty<Integer> READ_TIMEOUT =
			ConfigurationProperty.forKey( ElasticsearchBackendClientCommonSettings.READ_TIMEOUT )
					.asIntegerPositiveOrZeroOrNegative()
					.withDefault( ElasticsearchBackendClientCommonSettings.Defaults.READ_TIMEOUT )
					.build();

	private static final ConfigurationProperty<Integer> CONNECTION_TIMEOUT =
			ConfigurationProperty.forKey( ElasticsearchBackendClientCommonSettings.CONNECTION_TIMEOUT )
					.asIntegerPositiveOrZeroOrNegative()
					.withDefault( ElasticsearchBackendClientCommonSettings.Defaults.CONNECTION_TIMEOUT )
					.build();

	private static final ConfigurationProperty<Integer> MAX_TOTAL_CONNECTION =
			ConfigurationProperty.forKey( ElasticsearchBackendClientCommonSettings.MAX_CONNECTIONS )
					.asIntegerStrictlyPositive()
					.withDefault( ElasticsearchBackendClientCommonSettings.Defaults.MAX_CONNECTIONS )
					.build();

	private static final ConfigurationProperty<Integer> MAX_TOTAL_CONNECTION_PER_ROUTE =
			ConfigurationProperty.forKey( ElasticsearchBackendClientCommonSettings.MAX_CONNECTIONS_PER_ROUTE )
					.asIntegerStrictlyPositive()
					.withDefault( ElasticsearchBackendClientCommonSettings.Defaults.MAX_CONNECTIONS_PER_ROUTE )
					.build();

	private static final ConfigurationProperty<Boolean> DISCOVERY_ENABLED =
			ConfigurationProperty.forKey( ElasticsearchBackendClientCommonSettings.DISCOVERY_ENABLED )
					.asBoolean()
					.withDefault( ElasticsearchBackendClientCommonSettings.Defaults.DISCOVERY_ENABLED )
					.build();

	private static final ConfigurationProperty<Integer> DISCOVERY_REFRESH_INTERVAL =
			ConfigurationProperty.forKey( ElasticsearchBackendClientCommonSettings.DISCOVERY_REFRESH_INTERVAL )
					.asIntegerStrictlyPositive()
					.withDefault( ElasticsearchBackendClientCommonSettings.Defaults.DISCOVERY_REFRESH_INTERVAL )
					.build();

	private static final OptionalConfigurationProperty<
			BeanReference<? extends ElasticsearchHttpClientConfigurer>> CLIENT_CONFIGURER =
					ConfigurationProperty.forKey( ElasticsearchBackendClientSettings.CLIENT_CONFIGURER )
							.asBeanReference( ElasticsearchHttpClientConfigurer.class )
							.build();

	private static final OptionalConfigurationProperty<Long> MAX_KEEP_ALIVE =
			ConfigurationProperty.forKey( ElasticsearchBackendClientCommonSettings.MAX_KEEP_ALIVE )
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
			restClientHolder = createClient( beanResolver, propertySource, threadProvider, threadNamePrefix,
					hosts, PATH_PREFIX.get( propertySource ) );
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
									hosts, httpClientConfigurersHolder.get(), customConfig
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
				NodesSniffer hostsSniffer = new OpenSearchNodesSniffer(
						client,
						OpenSearchNodesSniffer.DEFAULT_SNIFF_REQUEST_TIMEOUT, // 1sec
						OpenSearchNodesSniffer.Scheme.HTTPS );
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
		builder.setThreadFactory( threadProvider.createThreadFactory( threadNamePrefix + " - Transport thread" ) );

		PoolingAsyncClientConnectionManagerBuilder connectionManagerBuilder =
				PoolingAsyncClientConnectionManagerBuilder.create()
						.setMaxConnTotal( MAX_TOTAL_CONNECTION.get( propertySource ) )
						.setMaxConnPerRoute( MAX_TOTAL_CONNECTION_PER_ROUTE.get( propertySource ) );

		if ( !hosts.isSslEnabled() ) {
			// In this case disable the SSL capability as it might have an impact on
			// bootstrap time, for example consuming entropy for no reason
			//            connectionManagerBuilder.setTlsStrategy(
			//                    ClientTlsStrategyBuilder.create()
			//                            .setSslContext(
			//                                    SSLContextBuilder.create()
			//                                            .loadTrustMaterial(null, new TrustAllStrategy())
			//                                            .build()
			//                            )
			//                            .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
			//                            .build()
			//            );
			connectionManagerBuilder.setTlsStrategy( NoopTlsStrategy.INSTANCE );
		}

		builder.setConnectionManager(
				connectionManagerBuilder
						.setDefaultConnectionConfig(
								ConnectionConfig.copy( ConnectionConfig.DEFAULT )
										.setConnectTimeout( CONNECTION_TIMEOUT.get( propertySource ), TimeUnit.MILLISECONDS )
										.setSocketTimeout( READ_TIMEOUT.get( propertySource ), TimeUnit.MILLISECONDS )
										.build()
						)
						.build()
		);

		Optional<String> username = USERNAME.get( propertySource );
		if ( username.isPresent() ) {
			Optional<char[]> password = PASSWORD.get( propertySource ).map( String::toCharArray );
			if ( password.isPresent() && !hosts.isSslEnabled() ) {
				ElasticsearchClientConfigurationLog.INSTANCE.usingPasswordOverHttp();
			}

			BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(
					new AuthScope( null, null, -1, null, null ),
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
				.setConnectionRequestTimeout( Timeout.DISABLED ) //Disable lease handling for the connection pool! See also HSEARCH-2681
				.setResponseTimeout( READ_TIMEOUT.get( propertySource ), TimeUnit.MILLISECONDS );
	}

	private static class NoopTlsStrategy implements TlsStrategy {
		private static final NoopTlsStrategy INSTANCE = new NoopTlsStrategy();

		private NoopTlsStrategy() {
		}

		@SuppressWarnings("deprecation")
		@Override
		public boolean upgrade(TransportSecurityLayer sessionLayer, HttpHost host, SocketAddress localAddress,
				SocketAddress remoteAddress, Object attachment, Timeout handshakeTimeout) {
			throw new UnsupportedOperationException( "upgrade is not supported." );
		}

		@Override
		public void upgrade(TransportSecurityLayer sessionLayer, NamedEndpoint endpoint, Object attachment,
				Timeout handshakeTimeout, FutureCallback<TransportSecurityLayer> callback) {
			if ( callback != null ) {
				callback.completed( sessionLayer );
			}
		}
	}
}
