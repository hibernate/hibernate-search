/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.common.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptor;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorProvider;
import org.hibernate.search.backend.elasticsearch.client.jdk.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.backend.elasticsearch.client.jdk.cfg.ClientJdkElasticsearchBackendClientSettings;
import org.hibernate.search.backend.elasticsearch.client.jdk.cfg.spi.ClientJdkElasticsearchBackendClientSpiSettings;
import org.hibernate.search.backend.elasticsearch.logging.spi.ConfigurationLog;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.util.common.impl.SuppressingCloser;


public class ClientJdkElasticsearchClientFactory implements ElasticsearchClientFactory {

	private static final OptionalConfigurationProperty<BeanReference<? extends HttpClient>> CLIENT_INSTANCE =
			ConfigurationProperty.forKey( ClientJdkElasticsearchBackendClientSpiSettings.CLIENT_INSTANCE )
					.asBeanReference( HttpClient.class )
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
			ConfigurationProperty.forKey( ClientJdkElasticsearchBackendClientSettings.REQUEST_TIMEOUT )
					.asIntegerStrictlyPositive()
					.build();

	private static final ConfigurationProperty<Integer> CONNECTION_TIMEOUT =
			ConfigurationProperty.forKey( ClientJdkElasticsearchBackendClientSettings.CONNECTION_TIMEOUT )
					.asIntegerPositiveOrZeroOrNegative()
					.withDefault( ClientJdkElasticsearchBackendClientSettings.Defaults.CONNECTION_TIMEOUT )
					.build();

	private static final OptionalConfigurationProperty<
			BeanReference<? extends ElasticsearchHttpClientConfigurer>> CLIENT_CONFIGURER =
					ConfigurationProperty.forKey( ClientJdkElasticsearchBackendClientSettings.CLIENT_CONFIGURER )
							.asBeanReference( ElasticsearchHttpClientConfigurer.class )
							.build();

	@Override
	public ElasticsearchClientImplementor create(BeanResolver beanResolver, ConfigurationPropertySource propertySource,
			ThreadProvider threadProvider, String threadNamePrefix,
			SimpleScheduledExecutor timeoutExecutorService,
			GsonProvider gsonProvider) {
		Optional<Integer> requestTimeoutMs = REQUEST_TIMEOUT.get( propertySource );

		NodeProvider nodeProvider = NodeProvider.fromOptionalStrings( PROTOCOL.get( propertySource ),
				HOSTS.get( propertySource ), URIS.get( propertySource ), PATH_PREFIX.get( propertySource ) );
		BeanHolder<? extends RestJdkClient> restClientHolder =
				createClient( beanResolver, propertySource, threadProvider, threadNamePrefix, nodeProvider );

		return new ClientJdkElasticsearchClient(
				restClientHolder, timeoutExecutorService, requestTimeoutMs,
				gsonProvider.getGson(), gsonProvider.getLogHelper(),
				createRequestInterceptors( beanResolver, propertySource )
		);
	}

	private static List<HttpRequestInterceptor> createRequestInterceptors(BeanResolver beanResolver,
			ConfigurationPropertySource propertySource) {
		List<HttpRequestInterceptor> interceptors = new ArrayList<>();
		List<BeanReference<ElasticsearchRequestInterceptorProvider>> requestInterceptorProviderReferences =
				beanResolver.allConfiguredForRole( ElasticsearchRequestInterceptorProvider.class );
		try ( BeanHolder<List<ElasticsearchRequestInterceptorProvider>> requestInterceptorProvidersHodler =
				beanResolver.resolve( requestInterceptorProviderReferences ) ) {
			ClientJdkElasticsearchHttpClientConfigurationContext clientConfigurationContext =
					new ClientJdkElasticsearchHttpClientConfigurationContext( beanResolver, propertySource, null );
			for ( ElasticsearchRequestInterceptorProvider interceptorProvider : requestInterceptorProvidersHodler.get() ) {
				Optional<ElasticsearchRequestInterceptor> requestInterceptor =
						interceptorProvider.provide( clientConfigurationContext );
				if ( requestInterceptor.isPresent() ) {
					interceptors.add( new ClientJdkHttpRequestInterceptor( requestInterceptor.get() ) );
				}
			}
		}
		return interceptors;
	}

	private BeanHolder<? extends RestJdkClient> createClient(BeanResolver beanResolver,
			ConfigurationPropertySource propertySource,
			ThreadProvider threadProvider, String threadNamePrefix, NodeProvider nodeProvider) {
		Optional<BeanHolder<? extends HttpClient>> providedHttpClientHolder = CLIENT_INSTANCE.getAndMap(
				propertySource, beanResolver::resolve );
		if ( providedHttpClientHolder.isPresent() ) {
			return BeanHolder.ofCloseable( new RestJdkClient( nodeProvider, providedHttpClientHolder.get().get() ) );
		}

		HttpClient.Builder builder = HttpClient.newBuilder()
				// NOTE: ES does not work ok with HTTP 2 if we don't send the content length and that can happen so let's stick to 1.1 for now ?
				//   (we end up with Caused by: java.io.IOException: Received RST_STREAM: Stream cancelled)
				.version( HttpClient.Version.HTTP_1_1 );

		Optional<? extends BeanHolder<? extends ElasticsearchHttpClientConfigurer>> customConfig = CLIENT_CONFIGURER
				.getAndMap( propertySource, beanResolver::resolve );

		RestJdkClient client = null;
		List<BeanReference<ElasticsearchHttpClientConfigurer>> httpClientConfigurerReferences =
				beanResolver.allConfiguredForRole( ElasticsearchHttpClientConfigurer.class );
		try ( BeanHolder<List<ElasticsearchHttpClientConfigurer>> httpClientConfigurersHolder =
				beanResolver.resolve( httpClientConfigurerReferences ) ) {
			customizeHttpClientConfig(
					builder,
					beanResolver, propertySource,
					threadProvider, threadNamePrefix,
					nodeProvider, httpClientConfigurersHolder.get(),
					customConfig
			);
			client = new RestJdkClient( nodeProvider, builder.build() );
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

	private HttpClient.Builder customizeHttpClientConfig(HttpClient.Builder builder,
			BeanResolver beanResolver, ConfigurationPropertySource propertySource,
			ThreadProvider threadProvider, String threadNamePrefix,
			NodeProvider nodeProvider, Iterable<ElasticsearchHttpClientConfigurer> configurers,
			Optional<? extends BeanHolder<? extends ElasticsearchHttpClientConfigurer>> customConfig) {
		builder.executor( Executors.newCachedThreadPool(
				threadProvider.createThreadFactory( threadNamePrefix + " - Transport thread" ) ) );

		if ( !nodeProvider.isSslEnabled() ) {
			SSLContext sslContext = null;
			try {
				sslContext = SSLContext.getInstance( "TLS" );
				sslContext.init( null, TRUST_ALL_CERTS, new SecureRandom() );
				builder.sslContext( sslContext );
			}
			catch (NoSuchAlgorithmException | KeyManagementException e) {
				throw new RuntimeException( e );
			}
		}

		builder.connectTimeout( Duration.ofMillis( CONNECTION_TIMEOUT.get( propertySource ) ) );


		Optional<String> username = USERNAME.get( propertySource );
		if ( username.isPresent() ) {
			Optional<char[]> password = PASSWORD.get( propertySource ).map( String::toCharArray );
			if ( password.isPresent() && !nodeProvider.isSslEnabled() ) {
				ConfigurationLog.INSTANCE.usingPasswordOverHttp();
			}
			builder.authenticator( new Authenticator() {
				private final PasswordAuthentication passwordAuthentication =
						new PasswordAuthentication( username.get(), password.orElseGet( () -> new char[0] ) );

				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return passwordAuthentication;
				}
			} );
		}

		ClientJdkElasticsearchHttpClientConfigurationContext clientConfigurationContext =
				new ClientJdkElasticsearchHttpClientConfigurationContext( beanResolver, propertySource, builder );

		for ( ElasticsearchHttpClientConfigurer configurer : configurers ) {
			configurer.configure( clientConfigurationContext );
		}
		if ( customConfig.isPresent() ) {
			BeanHolder<? extends ElasticsearchHttpClientConfigurer> customConfigBeanHolder = customConfig.get();
			customConfigBeanHolder.get().configure( clientConfigurationContext );
		}

		return builder;
	}

	private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[] {
			new X509TrustManager() {

				private static final X509Certificate[] X_509_CERTIFICATES = new X509Certificate[0];

				public X509Certificate[] getAcceptedIssuers() {
					return X_509_CERTIFICATES;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
					// Do nothing: trust client certificates
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
					// Do nothing: trust server certificates
				}
			}
	};
}
