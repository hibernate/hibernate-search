package org.hibernate.search.backend.elasticsearch.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLContext;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.jdk.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.engine.cfg.impl.MapConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.impl.EmbeddedThreadProvider;
import org.hibernate.search.util.impl.test.http.StreamingHttpServer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClientJdkElasticsearchClientFactoryTest {
	@ParameterizedTest
	@ValueSource(longs = { 0, 1, 2 })
	void clientFactoryAppliesConfiguredDecodedResponseLimit(long limit) throws Exception {
		var compressed = new ByteArrayOutputStream();
		try ( var gzip = new GZIPOutputStream( compressed ) ) {
			gzip.write( "{}".getBytes( StandardCharsets.UTF_8 ) );
		}
		try ( var server = new StreamingHttpServer( exchange -> {
			exchange.headers( 200, "application/json", "Content-Encoding: gzip\r\n" );
			exchange.chunk( compressed.toByteArray() );
			exchange.end();
		} ) ) {
			BeanResolver resolver = mock( BeanResolver.class );
			when( resolver.resolve( List.of() ) ).thenReturn( BeanHolder.of( List.of() ) );
			var client = new org.hibernate.search.backend.elasticsearch.client.common.impl.JsonTransportClient(
					new ClientJdkElasticsearchClientFactory().create( resolver,
							new MapConfigurationPropertySource( Map.of( ElasticsearchBackendSettings.URIS, server.uri(),
									ElasticsearchBackendSettings.RESPONSE_BODY_SIZE_LIMIT, limit ) ),
							new EmbeddedThreadProvider( "test" ), "test", null ),
					JsonProviderHelper.create( false ) );
			try {
				var result = client.submit( ElasticsearchJsonRequest.get().build() );
				if ( limit == 1 ) {
					assertThatThrownBy( () -> result.get( 10, TimeUnit.SECONDS ) ).hasStackTraceContaining( "byte limit" );
				}
				else {
					assertThat( result.get( 10, TimeUnit.SECONDS ).bodyJson().isEmpty() ).isTrue();
				}
			}
			finally {
				client.close();
			}
		}
	}

	@Test
	void headResponseWithGzipMetadataHasNoBodyToDecode() throws Exception {
		try ( var server = new StreamingHttpServer( exchange -> {
			exchange.headers( 200, "application/json", "Content-Encoding: gzip\r\n" );
			exchange.closeConnection();
		} ) ) {
			BeanResolver resolver = mock( BeanResolver.class );
			when( resolver.resolve( List.of() ) ).thenReturn( BeanHolder.of( List.of() ) );
			var client = new org.hibernate.search.backend.elasticsearch.client.common.impl.JsonTransportClient(
					new ClientJdkElasticsearchClientFactory().create( resolver,
							new MapConfigurationPropertySource( Map.of( ElasticsearchBackendSettings.URIS, server.uri() ) ),
							new EmbeddedThreadProvider( "test" ), "test", null ),
					JsonProviderHelper.create( false ) );
			try {
				var response = client.submit( ElasticsearchJsonRequest.builder( "HEAD" ).build() )
						.get( 10, TimeUnit.SECONDS );
				assertThat( response.statusCode() ).isEqualTo( 200 );
				assertThat( response.bodyJson() ).isNull();
			}
			finally {
				client.close();
			}
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "http", "https", "HTTPS", "HtTpS" })
	void defaultCertificateValidationForEveryScheme(String scheme) throws Exception {
		BeanResolver beanResolver = mock( BeanResolver.class );
		BeanReference<ElasticsearchHttpClientConfigurer> configurerReference =
				BeanReference.of( ElasticsearchHttpClientConfigurer.class, "test" );
		List<BeanReference<ElasticsearchHttpClientConfigurer>> references = List.of( configurerReference );
		when( beanResolver.allConfiguredForRole( ElasticsearchHttpClientConfigurer.class ) ).thenReturn( references );
		SSLContext[] configuredContext = new SSLContext[1];
		ElasticsearchHttpClientConfigurer configurer = context -> {
			HttpClient client = context.clientBuilder().build();
			try {
				configuredContext[0] = client.sslContext();
			}
			finally {
				// HttpClient implements AutoCloseable only on JDK 21 and newer.
				if ( ( (Object) client ) instanceof AutoCloseable closeable ) {
					try {
						closeable.close();
					}
					catch (Exception e) {
						throw new IllegalStateException( e );
					}
				}
			}
		};
		when( beanResolver.resolve( references ) ).thenReturn( BeanHolder.of( List.of( configurer ) ) );
		when( beanResolver.resolve( List.of() ) ).thenReturn( BeanHolder.of( List.of() ) );

		var client = new ClientJdkElasticsearchClientFactory().create(
				beanResolver,
				new MapConfigurationPropertySource( Map.of( ElasticsearchBackendSettings.URIS, scheme + "://localhost:9200" ) ),
				new EmbeddedThreadProvider( "test" ), "test", null );
		try {
			// The JDK default context validates the server certificate against the default trust store.
			assertThat( configuredContext[0] ).isSameAs( SSLContext.getDefault() );
		}
		finally {
			client.close();
		}
	}
}
