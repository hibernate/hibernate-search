package org.hibernate.search.backend.elasticsearch.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.common.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.jdk.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.engine.cfg.impl.MapConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.impl.EmbeddedThreadProvider;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.gson.GsonBuilder;

class ClientJdkElasticsearchClientFactoryTest {

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

		ElasticsearchClientImplementor client = new ClientJdkElasticsearchClientFactory().create(
				beanResolver,
				new MapConfigurationPropertySource( Map.of( ElasticsearchBackendSettings.URIS, scheme + "://localhost:9200" ) ),
				new EmbeddedThreadProvider( "test" ), "test", null,
				GsonProvider.create( GsonBuilder::new, false ) );
		try {
			// The JDK default context validates the server certificate against the default trust store.
			assertThat( configuredContext[0] ).isSameAs( SSLContext.getDefault() );
		}
		finally {
			client.close();
		}
	}
}
