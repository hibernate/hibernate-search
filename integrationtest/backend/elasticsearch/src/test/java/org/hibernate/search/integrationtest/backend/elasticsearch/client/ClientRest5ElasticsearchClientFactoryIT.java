package org.hibernate.search.integrationtest.backend.elasticsearch.client;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonClient;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.client.rest5.ElasticsearchHttpClientConfigurationContext;
import org.hibernate.search.backend.elasticsearch.client.rest5.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.backend.elasticsearch.client.rest5.cfg.ClientRest5ElasticsearchBackendClientSettings;
import org.hibernate.search.backend.elasticsearch.client.rest5.cfg.spi.ClientRest5ElasticsearchBackendClientSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.rest5.impl.ClientRest5ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.common.execution.spi.DelegatingSimpleScheduledExecutor;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.thread.impl.EmbeddedThreadProvider;
import org.hibernate.search.engine.environment.thread.impl.ThreadPoolProviderImpl;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendFeatures;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendHelper;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;
import org.hibernate.search.util.impl.test.extension.RetryExtension;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.spi.JsonParseException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;

import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.client5.http.nio.AsyncConnectionEndpoint;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.PoolEntry;
import org.apache.hc.core5.reactor.ConnectionInitiator;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.Level;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.DefaultElasticsearchClientFactoryTest")
class ClientRest5ElasticsearchClientFactoryIT {

	// Some tests in here are flaky, for some reason once in a while wiremock takes a very long time to answer
	// even though no delay was configured.
	// The exact reason is unknown though, so just try multiple times...

	@RegisterExtension
	private final ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@RegisterExtension
	private final WireMockExtension wireMockRule1 = WireMockExtension.newInstance()
			.options( wireMockConfig().dynamicPort().dynamicHttpsPort() )
			.build();

	@RegisterExtension
	private final WireMockExtension wireMockRule2 = WireMockExtension.newInstance()
			.options( wireMockConfig().dynamicPort().dynamicHttpsPort() )
			.build();

	@RegisterExtension
	private final TestConfigurationProvider testConfigurationProvider = new TestConfigurationProvider();

	private final ThreadPoolProviderImpl threadPoolProvider = new ThreadPoolProviderImpl(
			BeanHolder.of(
					new EmbeddedThreadProvider( ClientRest5ElasticsearchClientFactoryIT.class.getName() + ": " ) ) );

	private final ScheduledExecutorService timeoutExecutorService =
			threadPoolProvider.newScheduledExecutor( 1, "Timeout - " );

	@AfterEach
	void cleanup() {
		timeoutExecutorService.shutdownNow();
		threadPoolProvider.close();

		// Avoid side-effects from one test to another
		// Ideally WiremockRule should do that by itself, but it doesn't...
		wireMockRule1.resetAll();
		wireMockRule2.resetAll();
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-2274")
	void simple_http() {
		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "OK";
		String responseBody = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.andMatching( httpProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 )
						.withStatusMessage( statusMessage )
						.withBody( responseBody ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient() ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			assertThat( result.statusMessage() ).as( "status message" ).isEqualTo( statusMessage );
			assertJsonEquals( responseBody, result.bodyJson().toString() );

			wireMockRule1.verify(
					postRequestedFor( urlPathMatching( "/myIndex/myType" ) )
							.andMatching( httpProtocol() )
			);
		}
	}

	@RetryExtension.TestWithRetry
	void simple_httpClientConfigurer() throws Exception {
		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "OK";
		String responseBody = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.andMatching( httpProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 )
						.withStatusMessage( statusMessage )
						.withBody( responseBody ) ) );

		HttpResponseInterceptor responseInterceptor = spy( HttpResponseInterceptor.class );

		try ( ElasticsearchJsonClientImplementor client = createClient( properties -> properties.accept(
				ClientRest5ElasticsearchBackendClientSettings.CLIENT_CONFIGURER,
				(ElasticsearchHttpClientConfigurer) context -> context
						.clientBuilder()
						.addResponseInterceptorFirst( responseInterceptor )
		) ) ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			assertThat( result.statusMessage() ).as( "status message" ).isEqualTo( statusMessage );
			assertJsonEquals( responseBody, result.bodyJson().toString() );

			wireMockRule1.verify(
					postRequestedFor( urlPathMatching( "/myIndex/myType" ) )
							.andMatching( httpProtocol() )
			);
		}

		verify( responseInterceptor, times( 1 ) ).process( any(), any(), any() );
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-4099")
	void uris_http() {
		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "OK";
		String responseBody = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.andMatching( httpProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 )
						.withStatusMessage( statusMessage )
						.withBody( responseBody ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.URIS, httpUrisFor( wireMockRule1 ) );
				}
		) ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			assertThat( result.statusMessage() ).as( "status message" ).isEqualTo( statusMessage );
			assertJsonEquals( responseBody, result.bodyJson().toString() );

			wireMockRule1.verify(
					postRequestedFor( urlPathMatching( "/myIndex/myType" ) )
							.andMatching( httpProtocol() )
			);
		}
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-4051")
	void pathPrefix_http() {
		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "OK";
		String responseBody = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/bla/bla/bla/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.andMatching( httpProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 )
						.withStatusMessage( statusMessage )
						.withBody( responseBody ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.PATH_PREFIX, "bla/bla/bla" );
				}
		) ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			assertThat( result.statusMessage() ).as( "status message" ).isEqualTo( statusMessage );
			assertJsonEquals( responseBody, result.bodyJson().toString() );

			wireMockRule1.verify(
					postRequestedFor( urlPathMatching( "/bla/bla/bla/myIndex/myType" ) )
							.andMatching( httpProtocol() )
			);
		}
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-4099")
	void pathPrefix_uris() {
		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "OK";
		String responseBody = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/bla/bla/bla/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.andMatching( httpProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 )
						.withStatusMessage( statusMessage )
						.withBody( responseBody ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.PATH_PREFIX, "bla/bla/bla" );
					properties.accept( ElasticsearchBackendSettings.URIS, httpUrisFor( wireMockRule1 ) );
				}
		) ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			assertThat( result.statusMessage() ).as( "status message" ).isEqualTo( statusMessage );
			assertJsonEquals( responseBody, result.bodyJson().toString() );

			wireMockRule1.verify(
					postRequestedFor( urlPathMatching( "/bla/bla/bla/myIndex/myType" ) )
							.andMatching( httpProtocol() )
			);
		}
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-2274")
	void simple_https() {
		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "OK";
		String responseBody = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.andMatching( httpsProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 )
						.withStatusMessage( statusMessage )
						.withBody( responseBody ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.HOSTS, httpsHostAndPortFor( wireMockRule1 ) );
					properties.accept( ElasticsearchBackendSettings.PROTOCOL, "https" );
				}
		) ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			assertThat( result.statusMessage() ).as( "status message" ).isEqualTo( statusMessage );
			assertJsonEquals( responseBody, result.bodyJson().toString() );

			wireMockRule1.verify(
					postRequestedFor( urlPathMatching( "/myIndex/myType" ) )
							.andMatching( httpsProtocol() )
			);
		}
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-2274")
	void uris_https() {
		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "OK";
		String responseBody = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.andMatching( httpsProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 )
						.withStatusMessage( statusMessage )
						.withBody( responseBody ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.URIS, httpsUrisFor( wireMockRule1 ) );
				}
		) ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			assertThat( result.statusMessage() ).as( "status message" ).isEqualTo( statusMessage );
			assertJsonEquals( responseBody, result.bodyJson().toString() );

			wireMockRule1.verify(
					postRequestedFor( urlPathMatching( "/myIndex/myType" ) )
							.andMatching( httpsProtocol() )
			);
		}
	}

	@RetryExtension.TestWithRetry
	void error() {
		String payload = "{ \"foo\": \"bar\" }";
		String responseBody = "{ \"error\": \"ErrorMessageExplainingTheError\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse().withStatus( 500 )
								.withBody( responseBody )
				) );

		try ( ElasticsearchJsonClientImplementor client = createClient() ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 500 );
			assertJsonEquals( responseBody, result.bodyJson().toString() );
		}
	}

	@RetryExtension.TestWithRetry
	void unparseable() {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse()
								.withBody( "'unparseable" )
				) );

		assertThatThrownBy( () -> {
			try ( ElasticsearchJsonClientImplementor client = createClient() ) {
				doPost( client, "/myIndex/myType", payload );
			}
		} )
				.isInstanceOf( AssertionFailure.class )
				.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( CompletionException.class )
				.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "HSEARCH400089" )
				.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( JsonParseException.class );
	}

	@RetryExtension.TestWithRetry
	void timeout_read() {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse()
								.withFixedDelay( 2000 )
				) );

		assertThatThrownBy( () -> {
			try ( ElasticsearchJsonClientImplementor client = createClient(
					properties -> {
						properties.accept( ClientRest5ElasticsearchBackendClientSettings.READ_TIMEOUT, "1000" );
						properties.accept( ClientRest5ElasticsearchBackendClientSettings.REQUEST_TIMEOUT, "99999" );
					}
			) ) {
				doPost( client, "/myIndex/myType", payload );
			}
		} )
				.isInstanceOf( AssertionFailure.class )
				.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( CompletionException.class )
				.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( IOException.class );
	}

	@RetryExtension.TestWithRetry
	void timeout_request() {
		String payload = "{ \"foo\": \"bar\" }";

		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse()
								.withFixedDelay( 2000 )
				) );

		assertThatThrownBy( () -> {
			try ( ElasticsearchJsonClientImplementor client = createClient(
					properties -> {
						properties.accept( ClientRest5ElasticsearchBackendClientSettings.READ_TIMEOUT, "99999" );
						properties.accept( ClientRest5ElasticsearchBackendClientSettings.REQUEST_TIMEOUT, "1000" );
					}
			) ) {
				doPost( client, "/myIndex/myType", payload );
			}
		} )
				.isInstanceOf( AssertionFailure.class )
				.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( CompletionException.class )
				.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Request execution exceeded the timeout of 1s, 0ms and 0ns",
						"Request was POST /myIndex/myType with parameters {}" );
	}

	/**
	 * Verify that by default, even when the client is clogged (many pending requests),
	 * we don't trigger timeouts just because requests spend a long time waiting;
	 * timeouts are only related to how long the *server* takes to answer.
	 */
	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-2836")
	void cloggedClient_noTimeout_read() {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/long" ) )
				.willReturn( elasticsearchResponse()
						.withFixedDelay( 300 ) ) );
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 )
						.withFixedDelay( 100 ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.HOSTS, httpHostAndPortFor( wireMockRule1 ) );
					properties.accept( ClientRest5ElasticsearchBackendClientSettings.MAX_CONNECTIONS_PER_ROUTE, "1" );
					properties.accept( ClientRest5ElasticsearchBackendClientSettings.READ_TIMEOUT, "1000" );
				}
		) ) {
			// Clog up the client: put many requests in the queue, to be executed asynchronously,
			// so that we're sure the next request will have to wait in the queue
			// for more that the configured timeout before it ends up being executed.
			for ( int i = 0; i < 10; ++i ) {
				client.submit( buildRequest( ElasticsearchJsonRequest.post(), "/long", payload ) );
			}
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( 1, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
		}
	}

	/**
	 * Verify that when a request timeout is set, and when the client is clogged (many pending requests),
	 * we do trigger timeouts just because requests spend a long time waiting.
	 */
	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-2836")
	void cloggedClient_timeout_request() {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/long" ) )
				.willReturn( elasticsearchResponse()
						.withFixedDelay( 300 ) ) );
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 )
						.withFixedDelay( 100 ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.HOSTS, httpHostAndPortFor( wireMockRule1 ) );
					properties.accept( ClientRest5ElasticsearchBackendClientSettings.MAX_CONNECTIONS_PER_ROUTE, "1" );
					properties.accept( ClientRest5ElasticsearchBackendClientSettings.REQUEST_TIMEOUT, "1000" );
				}
		) ) {
			// Clog up the client: put many requests in the queue, to be executed asynchronously,
			// so that we're sure the next request will have to wait in the queue
			// for more that the configured timeout before it ends up being executed.
			for ( int i = 0; i < 10; ++i ) {
				client.submit( buildRequest( ElasticsearchJsonRequest.post(), "/long", payload ) );
			}

			assertThatThrownBy( () -> {
				doPost( client, "/myIndex/myType", payload );
			} )
					.isInstanceOf( AssertionFailure.class )
					.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
					.isInstanceOf( CompletionException.class )
					.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Request execution exceeded the timeout of 1s, 0ms and 0ns",
							"Request was POST /myIndex/myType with parameters {}" );
		}
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-2235")
	void multipleHosts() {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.HOSTS,
							httpHostAndPortFor( wireMockRule1, wireMockRule2 ) );
				}
		) ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
			wireMockRule2.verify( postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
		}
	}

	@RetryExtension.TestWithRetry
	void multipleURIs() {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.URIS,
							httpsUrisFor( wireMockRule1, wireMockRule2 ) );
				}
		) ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
			wireMockRule2.verify( postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
		}
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-2469")
	void multipleHosts_failover_serverError() {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 503 ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.HOSTS,
							httpHostAndPortFor( wireMockRule1, wireMockRule2 ) );
				}
		) ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( 2, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 2, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );

			wireMockRule1.resetRequests();
			wireMockRule2.resetRequests();

			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
		}
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-2469")
	void multipleHosts_failover_timeout() {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withFixedDelay( 5_000 ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.HOSTS,
							httpHostAndPortFor( wireMockRule1, wireMockRule2 ) );
					// Use a timeout much higher than 1s, because wiremock can be really slow...
					properties.accept( ClientRest5ElasticsearchBackendClientSettings.READ_TIMEOUT, "1000" );
				}
		) ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( 2, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 1, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );

			wireMockRule1.resetRequests();
			wireMockRule2.resetRequests();

			/*
			 * Remove the failure in the previously failing node,
			 * so that we can detect if requests are sent to this node.
			 */
			wireMockRule2.resetMappings();
			wireMockRule2.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
					.withRequestBody( equalToJson( payload ) )
					.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
		}
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-2469")
	void multipleHosts_failover_fault() {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withFault( Fault.MALFORMED_RESPONSE_CHUNK ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.HOSTS,
							httpHostAndPortFor( wireMockRule1, wireMockRule2 ) );
				}
		) ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( 2, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 1, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );

			wireMockRule1.resetRequests();
			wireMockRule2.resetRequests();

			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
		}
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-2449")
	void discovery_http() {
		assumeTrue( ElasticsearchTckBackendFeatures.supportsVersionCheck(),
				"Elasticsearch REST Client based on Apache HTTP client 5 requires version when sniffer is parsing responses." );
		String nodesInfoResult = dummyNodeInfoResponse( wireMockRule1.getPort(), wireMockRule2.getPort() );

		wireMockRule1.stubFor( get( urlPathMatching( "/_nodes.*" ) )
				.andMatching( httpProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withBody( nodesInfoResult ) ) );
		wireMockRule2.stubFor( get( urlPathMatching( "/_nodes.*" ) )
				.andMatching( httpProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withBody( nodesInfoResult ) ) );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.andMatching( httpProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.andMatching( httpProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ClientRest5ElasticsearchBackendClientSettings.DISCOVERY_ENABLED, "true" );
					properties.accept( ClientRest5ElasticsearchBackendClientSettings.DISCOVERY_REFRESH_INTERVAL, "1" );
				}
		) ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );

			/*
			 * Send requests repeatedly until both hosts have been targeted.
			 * This should happen pretty early (as soon as we sent two requests, actually),
			 * but there is always the risk that the sniffer would send a request
			 * between our own requests, effectively making our own requests target the same host
			 * (since the hosts are each targeted in turn).
			 */
			await().untilAsserted( () -> {
				ElasticsearchJsonResponse newResult = doPost( client, "/myIndex/myType", payload );
				assertThat( newResult.statusCode() ).as( "status code" ).isEqualTo( 200 );

				wireMockRule2.verify(
						postRequestedFor( urlPathMatching( "/myIndex/myType" ) )
								.andMatching( httpProtocol() )
				);
				wireMockRule2.verify(
						postRequestedFor( urlPathMatching( "/myIndex/myType" ) )
								.andMatching( httpProtocol() )
				);
			} );
		}
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-2736")
	void discovery_https() {
		assumeTrue( ElasticsearchTckBackendFeatures.supportsVersionCheck(),
				"Elasticsearch REST Client based on Apache HTTP client 5 requires version when sniffer is parsing responses." );
		String nodesInfoResult = dummyNodeInfoResponse( wireMockRule1.getHttpsPort(), wireMockRule2.getHttpsPort() );

		wireMockRule1.stubFor( get( urlPathMatching( "/_nodes.*" ) )
				.andMatching( httpsProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withBody( nodesInfoResult ) ) );
		wireMockRule2.stubFor( get( urlPathMatching( "/_nodes.*" ) )
				.andMatching( httpsProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withBody( nodesInfoResult ) ) );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.andMatching( httpsProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.andMatching( httpsProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.HOSTS, httpsHostAndPortFor( wireMockRule1 ) );
					properties.accept( ElasticsearchBackendSettings.PROTOCOL, "https" );
					properties.accept( ClientRest5ElasticsearchBackendClientSettings.DISCOVERY_ENABLED, "true" );
					properties.accept( ClientRest5ElasticsearchBackendClientSettings.DISCOVERY_REFRESH_INTERVAL, "1" );
				}
		) ) {
			/*
			 * Send requests repeatedly until both hosts have been targeted.
			 * This should happen pretty early (as soon as we sent two requests, actually),
			 * but there is always the risk that the sniffer would send a request
			 * between our own requests, effectively making our own requests target the same host
			 * (since the hosts are each targeted in turn).
			 */
			await().untilAsserted( () -> {
				ElasticsearchJsonResponse newResult = doPost( client, "/myIndex/myType", payload );
				assertThat( newResult.statusCode() ).as( "status code" ).isEqualTo( 200 );

				wireMockRule2.verify(
						postRequestedFor( urlPathMatching( "/myIndex/myType" ) )
								.andMatching( httpsProtocol() )
				);
				wireMockRule2.verify(
						postRequestedFor( urlPathMatching( "/myIndex/myType" ) )
								.andMatching( httpsProtocol() )
				);
			} );
		}
	}

	private static RequestMatcherExtension httpProtocol() {
		return protocol( "http" );
	}

	private static RequestMatcherExtension httpsProtocol() {
		return protocol( "https" );
	}

	private static RequestMatcherExtension protocol(String protocol) {
		return new RequestMatcherExtension() {
			@Override
			public MatchResult match(Request request, Parameters parameters) {
				return MatchResult.of( protocol.equals( request.getScheme() ) );
			}

			@Override
			public String getName() {
				return "expected protocol: " + protocol;
			}
		};
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-2453")
	void authentication() {
		assumeFalse(
				ElasticsearchTestHostConnectionConfiguration.get().isAws(),
				"This test only is only relevant if Elasticsearch request are *NOT* automatically" +
						" augmented with an \"Authentication:\" header." +
						" \"Authentication:\" headers are added by the AWS integration in particular."
		);
		String username = "ironman";
		String password = "j@rV1s";

		String payload = "{ \"foo\": \"bar\" }";

		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType/_search" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 401 )
						.withHeader( "www-authenticate", "Basic realm=\"IT Realm\"" ) ) );

		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType/_search" ) )
				.withBasicAuth( username, password )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.USERNAME, username );
					properties.accept( ElasticsearchBackendSettings.PASSWORD, password );
				}
		) ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType/_search", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
		}
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-2453")
	void authentication_error() {
		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "Unauthorized";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType/_search" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse().withStatus( 401 )
								.withStatusMessage( statusMessage )
				) );

		try ( ElasticsearchJsonClientImplementor client = createClient() ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType/_search", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 401 );
			assertThat( result.statusMessage() ).as( "status message" ).isEqualTo( statusMessage );
		}
	}

	@RetryExtension.TestWithRetry
	@TestForIssue(jiraKey = "HSEARCH-2453")
	void authentication_http_password() {
		String username = "ironman";
		String password = "j@rV1s";

		logged.expectEvent( Level.WARN, "The password will be sent in clear text over the network" );

		try ( ElasticsearchJsonClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.USERNAME, username );
					properties.accept( ElasticsearchBackendSettings.PASSWORD, password );
				}
		) ) {
			// Nothing to do here
		}
	}

	@RetryExtension.TestWithRetry
	void uriAndProtocol() {
		Consumer<BiConsumer<String, Object>> additionalProperties = properties -> {
			properties.accept( ElasticsearchBackendSettings.URIS, "http://is-not-called:12345" );
			properties.accept( ElasticsearchBackendSettings.PROTOCOL, "http" );
		};

		assertThatThrownBy( () -> createClient( additionalProperties ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid target hosts configuration",
						"both the 'uris' property and the 'protocol' property are set",
						"Uris: '[http://is-not-called:12345]'", "Protocol: 'http'",
						"Either set the protocol and hosts simultaneously using the 'uris' property",
						"or set them separately using the 'protocol' property and the 'hosts' property"
				);
	}

	@RetryExtension.TestWithRetry
	void uriAndHosts() {
		Consumer<BiConsumer<String, Object>> additionalProperties = properties -> {
			properties.accept( ElasticsearchBackendSettings.URIS, "http://is-not-called:12345" );
			properties.accept( ElasticsearchBackendSettings.HOSTS, "not-called-either:234" );
		};
		// host and port are dynamic
		// host and port are dynamic
		assertThatThrownBy( () -> createClient( additionalProperties ) )
				.isInstanceOf( SearchException.class )
				.// host and port are dynamic
				hasMessageContainingAll( // host and port are dynamic
						"Invalid target hosts configuration", // host and port are dynamic
						"both the 'uris' property and the 'hosts' property are set",
						"Uris: '[http://is-not-called:12345]'", "Hosts: '[",
						"Either set the protocol and hosts simultaneously using the 'uris' property",
						"or set them separately using the 'protocol' property and the 'hosts' property"
				);
	}

	@RetryExtension.TestWithRetry
	void differentProtocolsOnUris() {
		Consumer<BiConsumer<String, Object>> additionalProperties = properties -> {
			properties.accept( ElasticsearchBackendSettings.URIS,
					"http://is-not-called:12345, https://neather-is:12345" );
		};

		assertThatThrownBy( () -> createClient( additionalProperties ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid target hosts configuration: the 'uris' use different protocols (http, https)",
						"All URIs must use the same protocol",
						"Uris: '[http://is-not-called:12345, https://neather-is:12345]'"
				);
	}

	@RetryExtension.TestWithRetry
	void emptyListOfUris() {
		Consumer<BiConsumer<String, Object>> additionalProperties = properties -> {
			properties.accept( ElasticsearchBackendSettings.URIS, Collections.emptyList() );
		};

		assertThatThrownBy( () -> createClient( additionalProperties ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Invalid target hosts configuration: the list of URIs must not be empty"
				);
	}

	@RetryExtension.TestWithRetry
	void emptyListOfHosts() {
		Consumer<BiConsumer<String, Object>> additionalProperties = properties -> {
			properties.accept( ElasticsearchBackendSettings.HOSTS, Collections.emptyList() );
		};

		assertThatThrownBy( () -> createClient( additionalProperties ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Invalid target hosts configuration: the list of hosts must not be empty"
				);
	}

	@RetryExtension.TestWithRetry
	void clientInstance() throws IOException, URISyntaxException {
		try ( Rest5Client myRestClient = Rest5Client.builder( HttpHost.create( httpUrisFor( wireMockRule1 ) ) ).build() ) {
			String payload = "{ \"foo\": \"bar\" }";
			String statusMessage = "OK";
			String responseBody = "{ \"foo\": \"bar\" }";
			wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
					.withRequestBody( equalToJson( payload ) )
					.andMatching( httpProtocol() )
					.willReturn( elasticsearchResponse().withStatus( 200 )
							.withStatusMessage( statusMessage )
							.withBody( responseBody ) ) );

			try ( ElasticsearchJsonClientImplementor client = createClient( properties -> {
				properties.accept( ClientRest5ElasticsearchBackendClientSpiSettings.CLIENT_INSTANCE,
						BeanReference.ofInstance( myRestClient ) );
			} ) ) {
				ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
				assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
				assertThat( result.statusMessage() ).as( "status message" ).isEqualTo( statusMessage );
				assertJsonEquals( responseBody, result.bodyJson().toString() );

				wireMockRule1.verify(
						postRequestedFor( urlPathMatching( "/myIndex/myType" ) )
								.andMatching( httpProtocol() )
				);
			}

			// Hibernate Search must not close provided client instances
			assertThat( myRestClient ).returns( true, Rest5Client::isRunning );
		}
	}

	@RetryExtension.TestWithRetry
	void maxKeepAliveNegativeValue() {
		assertThatThrownBy( () -> createClient(
				properties -> {
					properties.accept( ClientRest5ElasticsearchBackendClientSettings.MAX_KEEP_ALIVE, -1 );
				}
		) ).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid value for configuration property",
						"must be positive or zero"
				);
	}

	@RetryExtension.TestWithRetry
	void maxKeepAliveConnectionIsNotClosed() throws InterruptedException {
		maxKeepAliveConnection( 100000, 1 );
	}

	@RetryExtension.TestWithRetry
	void maxKeepAliveConnectionIsClosed() throws InterruptedException {
		maxKeepAliveConnection( 10, 2 );
	}

	void maxKeepAliveConnection(long time, int connections) throws InterruptedException {
		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "OK";
		String responseBody = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.andMatching( httpProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 )
						.withStatusMessage( statusMessage )
						.withBody( responseBody ) ) );

		Set<String> usedConnections = Collections.synchronizedSet( new HashSet<>() );
		try ( ElasticsearchJsonClientImplementor client = createClient( properties -> {
			properties.accept( ClientRest5ElasticsearchBackendClientSettings.MAX_KEEP_ALIVE, time );
			properties.accept(
					ClientRest5ElasticsearchBackendClientSettings.CLIENT_CONFIGURER,
					(ElasticsearchHttpClientConfigurer) context -> {
						context.clientBuilder()
								.setConnectionManager( createPoolManager( usedConnections, connections, connections ) );
					}
			);
		} ) ) {
			ElasticsearchJsonResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			assertThat( result.statusMessage() ).as( "status message" ).isEqualTo( statusMessage );
			assertJsonEquals( responseBody, result.bodyJson().toString() );

			wireMockRule1.verify(
					postRequestedFor( urlPathMatching( "/myIndex/myType" ) )
							.andMatching( httpProtocol() )
			);

			TimeUnit.SECONDS.sleep( 2 );

			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.statusCode() ).as( "status code" ).isEqualTo( 200 );
			assertThat( result.statusMessage() ).as( "status message" ).isEqualTo( statusMessage );
			assertJsonEquals( responseBody, result.bodyJson().toString() );

			wireMockRule1.verify(
					postRequestedFor( urlPathMatching( "/myIndex/myType" ) )
							.andMatching( httpProtocol() )
			);

			assertThat( usedConnections ).hasSize( connections );
		}
	}

	private AsyncClientConnectionManager createPoolManager(Set<String> connections, int maxConnTotal, int maxConnPerRoute) {
		PoolingAsyncClientConnectionManager delegate = // Since httpclient5 5.6.4, the built-in hostname verification is actually applied to the engine
				PoolingAsyncClientConnectionManagerBuilder.create()
						.setMaxConnTotal( maxConnTotal )
						.setMaxConnPerRoute( maxConnPerRoute )
						.// Since httpclient5 5.6.4, the built-in hostname verification is actually applied to the engine
						setTlsStrategy( // (previously it was set after the parameters were copied to the engine and had no effect).
								ClientTlsStrategyBuilder.create()
										.setSslContext( buildAllowAnythingSSLContext() ).// Use CLIENT policy so that only the noop verifier below runs, keeping WireMock's self-signed cert accepted.
										setHostVerificationPolicy( HostnameVerificationPolicy.CLIENT )
										.setHostnameVerifier( org.apache.http.conn.ssl.NoopHostnameVerifier.INSTANCE )
										.buildAsync()
						)
						.build();

		return new AsyncClientConnectionManager() {
			private Field poolEntryRef;

			@Override
			public void close() throws IOException {
				delegate.close();
			}

			@Override
			public void close(CloseMode closeMode) {
				delegate.close( closeMode );
			}

			@Override
			public Future<AsyncConnectionEndpoint> lease(String id, HttpRoute route, Object state, Timeout requestTimeout,
					FutureCallback<AsyncConnectionEndpoint> callback) {
				return delegate.lease( id, route, state, requestTimeout, callback );
			}

			@SuppressWarnings("unchecked")
			@Override
			public void release(AsyncConnectionEndpoint endpoint, Object state, TimeValue keepAlive) {
				try {
					if ( poolEntryRef == null ) {
						poolEntryRef = endpoint.getClass().getDeclaredField( "poolEntryRef" );
						poolEntryRef.setAccessible( true );
					}
					AtomicReference<PoolEntry<?, ?>> ref = (AtomicReference<PoolEntry<?, ?>>) poolEntryRef.get( endpoint );
					connections.add( ref.get().getConnection().toString() );

				}
				catch (NoSuchFieldException | IllegalAccessException e) {
					fail( e.getMessage(), e );
				}
				delegate.release( endpoint, state, keepAlive );
			}

			@Override
			public Future<AsyncConnectionEndpoint> connect(AsyncConnectionEndpoint endpoint,
					ConnectionInitiator connectionInitiator, Timeout connectTimeout, Object attachment, HttpContext context,
					FutureCallback<AsyncConnectionEndpoint> callback) {
				return delegate.connect( endpoint, connectionInitiator, connectTimeout, attachment, context, callback );
			}

			@Override
			public void upgrade(AsyncConnectionEndpoint endpoint, Object attachment, HttpContext context) {
				delegate.upgrade( endpoint, attachment, context );
			}
		};
	}

	// For EJC and lambda arg
	@SuppressWarnings("unused")
	private ElasticsearchJsonClientImplementor createClient() {
		return createClient( ignored -> {} );
	}

	private ElasticsearchJsonClientImplementor createClient(Consumer<BiConsumer<String, Object>> additionalProperties) {
		Map<String, ?> defaultBackendProperties =
				new ElasticsearchTckBackendHelper().createDefaultBackendSetupStrategy()
						.createBackendConfigurationProperties( testConfigurationProvider );

		Map<String, Object> clientProperties = new HashMap<>( defaultBackendProperties );

		// We won't target the provided ES instance, but Wiremock
		clientProperties.remove( ElasticsearchBackendSettings.HOSTS );
		clientProperties.remove( ElasticsearchBackendSettings.PROTOCOL );
		clientProperties.remove( ElasticsearchBackendSettings.URIS );

		// Per-test overrides
		additionalProperties.accept( clientProperties::put );

		// By default, target the Wiremock server 1 using HTTP
		if ( !clientProperties.containsKey( ElasticsearchBackendSettings.HOSTS )
				&& !clientProperties.containsKey( ElasticsearchBackendSettings.URIS ) ) {
			clientProperties.put( ElasticsearchBackendSettings.URIS, httpUrisFor( wireMockRule1 ) );
		}

		ConfigurationPropertySource clientPropertySource = AllAwareConfigurationPropertySource.fromMap( clientProperties );

		Map<String, Object> beanResolverConfiguration = new HashMap<>();
		// Accept Wiremock's self-signed SSL certificates
		beanResolverConfiguration.put(
				EngineSpiSettings.BEAN_CONFIGURERS,
				Collections.singletonList( elasticsearchSslBeanConfigurer( clientPropertySource ) )
		);

		BeanResolver beanResolver = testConfigurationProvider.createBeanResolverForTest(
				AllAwareConfigurationPropertySource.fromMap( beanResolverConfiguration )

		);
		return new org.hibernate.search.backend.elasticsearch.client.common.impl.JsonTransportClient(
				new ClientRest5ElasticsearchClientFactory().create( beanResolver, clientPropertySource,
						threadPoolProvider.threadProvider(), "Client",
						new DelegatingSimpleScheduledExecutor( timeoutExecutorService, true ) ),
				JsonProviderHelper.create( true )
		);
	}

	private ElasticsearchJsonResponse doPost(ElasticsearchJsonClient client, String path, String payload) {
		try {
			return client.submit( buildRequest( ElasticsearchJsonRequest.post(), path, payload ) ).join();
		}
		catch (RuntimeException e) {
			throw new AssertionFailure( "Unexpected exception during POST: " + e.getMessage(), e );
		}
	}

	private ElasticsearchJsonRequest buildRequest(ElasticsearchJsonRequest.Builder builder, String path, String payload) {
		for ( String pathComponent : path.split( "/" ) ) {
			if ( !pathComponent.isEmpty() ) {
				URLEncodedString fromString = URLEncodedString.fromString( pathComponent );
				builder = builder.pathComponent( fromString );
			}
		}
		if ( payload != null ) {
			builder = builder.bodyJson( ( (ObjectNode) new JsonParsing().parse( payload ) ) );
		}
		return builder.build();
	}

	private static String httpHostAndPortFor(WireMockExtension... extensions) {
		return Arrays.stream( extensions )
				.map( extension -> "localhost:" + extension.getPort() )
				.collect( Collectors.joining( "," ) );
	}

	private static String httpsHostAndPortFor(WireMockExtension... extensions) {
		return Arrays.stream( extensions )
				.map( extension -> "localhost:" + extension.getHttpsPort() )
				.collect( Collectors.joining( "," ) );
	}

	private static String httpUrisFor(WireMockExtension... extensions) {
		return Arrays.stream( extensions )
				.map( extension -> "http://localhost:" + extension.getPort() )
				.collect( Collectors.joining( "," ) );
	}

	private static String httpsUrisFor(WireMockExtension... extensions) {
		return Arrays.stream( extensions )
				.map( extension -> "https://localhost:" + extension.getHttpsPort() )
				.collect( Collectors.joining( "," ) );
	}

	private static ResponseDefinitionBuilder elasticsearchResponse() {
		return ResponseDefinitionBuilder.okForEmptyJson();
	}

	private String dummyNodeInfoResponse(int... ports) {
		ObjectNode body = new ObjectNode( JsonNodeFactory.instance );
		body.put( "cluster_name", "foo-cluster.local" );
		ObjectNode nodes = new ObjectNode( JsonNodeFactory.instance );
		body.set( "nodes", nodes );
		int index = 1;
		for ( int port : ports ) {
			nodes.set( "hJLXmY_NTrCytiIMbX4_" + index + "g", dummyNodeInfo( port ) );
			++index;
		}

		return body.toString();
	}

	private ObjectNode dummyNodeInfo(int port) {
		ObjectNode node = new ObjectNode( JsonNodeFactory.instance );
		node.put( "name", "nodeForPort" + port );
		node.put( "version", ElasticsearchTestDialect.getActualVersion().versionString() );
		ObjectNode http = new ObjectNode( JsonNodeFactory.instance );
		node.set( "http", http );
		http.put( "publish_address", "127.0.0.1:" + port );
		ArrayNode boundAddresses = new ArrayNode( JsonNodeFactory.instance );
		http.set( "bound_address", boundAddresses );
		boundAddresses.add( "[::]:" + port );
		boundAddresses.add( "127.0.0.1:" + port );
		ArrayNode roles = new ArrayNode( JsonNodeFactory.instance );
		node.set( "roles", roles );
		roles.add( "ingest" );
		roles.add( "master" );
		roles.add( "data" );
		roles.add( "ml" );

		node.set( "plugins", new ObjectNode( JsonNodeFactory.instance ) );

		return node;
	}

	private static final ConfigurationProperty<Integer> MAX_TOTAL_CONNECTION =
			ConfigurationProperty.forKey( ClientRest5ElasticsearchBackendClientSettings.MAX_CONNECTIONS )
					.asIntegerStrictlyPositive()
					.withDefault( ClientRest5ElasticsearchBackendClientSettings.Defaults.MAX_CONNECTIONS )
					.build();

	private static final ConfigurationProperty<Integer> MAX_TOTAL_CONNECTION_PER_ROUTE =
			ConfigurationProperty.forKey( ClientRest5ElasticsearchBackendClientSettings.MAX_CONNECTIONS_PER_ROUTE )
					.asIntegerStrictlyPositive()
					.withDefault( ClientRest5ElasticsearchBackendClientSettings.Defaults.MAX_CONNECTIONS_PER_ROUTE )
					.build();

	private static final ConfigurationProperty<Integer> READ_TIMEOUT =
			ConfigurationProperty.forKey( ClientRest5ElasticsearchBackendClientSettings.READ_TIMEOUT )
					.asIntegerPositiveOrZeroOrNegative()
					.withDefault( ClientRest5ElasticsearchBackendClientSettings.Defaults.READ_TIMEOUT )
					.build();

	private static final ConfigurationProperty<Integer> CONNECTION_TIMEOUT =
			ConfigurationProperty.forKey( ClientRest5ElasticsearchBackendClientSettings.CONNECTION_TIMEOUT )
					.asIntegerPositiveOrZeroOrNegative()
					.withDefault( ClientRest5ElasticsearchBackendClientSettings.Defaults.CONNECTION_TIMEOUT )
					.build();

	private static BeanConfigurer elasticsearchSslBeanConfigurer(ConfigurationPropertySource propertySource) {
		return context -> {
			context.define(
					ElasticsearchHttpClientConfigurer.class,
					BeanReference.ofInstance( new ElasticsearchHttpClientConfigurer() {
						@Override
						public void configure(ElasticsearchHttpClientConfigurationContext context) {
							context.clientBuilder()
									.setConnectionManager( // Since httpclient5 5.6.4, the built-in hostname verification is actually applied to the engine
											PoolingAsyncClientConnectionManagerBuilder.create()
													.setMaxConnTotal( MAX_TOTAL_CONNECTION.get( propertySource ) )
													.setMaxConnPerRoute( MAX_TOTAL_CONNECTION_PER_ROUTE.get( propertySource ) )
													.setDefaultConnectionConfig(
															ConnectionConfig.copy( ConnectionConfig.DEFAULT )
																	.setConnectTimeout(
																			CONNECTION_TIMEOUT.get( propertySource ),
																			TimeUnit.MILLISECONDS )
																	.setSocketTimeout( READ_TIMEOUT.get( propertySource ),
																			TimeUnit.MILLISECONDS )
																	.build()
													)
													.// Since httpclient5 5.6.4, the built-in hostname verification is actually applied to the engine
													setTlsStrategy( // (previously it was set after the parameters were copied to the engine and had no effect).
															ClientTlsStrategyBuilder.create()
																	.setSslContext( buildAllowAnythingSSLContext() ).// Use CLIENT policy so that only the noop verifier below runs, keeping WireMock's self-signed cert accepted.
																	setHostVerificationPolicy(
																			HostnameVerificationPolicy.CLIENT )
																	.setHostnameVerifier( NoopHostnameVerifier.INSTANCE )
																	.buildAsync()
													)
													.build()
									);
						}
					} )
			);
		};
	}

	private static SSLContext buildAllowAnythingSSLContext() {
		try {
			return SSLContexts.custom().loadTrustMaterial( null, new TrustAllStrategy() ).build();
		}
		catch (Exception e) {
			throw new AssertionFailure( "Unexpected exception", e );
		}
	}
}
