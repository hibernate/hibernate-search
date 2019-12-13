/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.client;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.util.impl.test.ExceptionMatcherBuilder.isException;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientFactoryImpl;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.thread.impl.DefaultThreadProvider;
import org.hibernate.search.engine.environment.thread.impl.ThreadPoolProviderImpl;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.categories.RequiresNoAutomaticAuthenticationHeader;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendHelper;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContexts;

@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.DefaultElasticsearchClientFactoryTest")
public class ElasticsearchClientFactoryImplIT {

	private static final JsonParser JSON_PARSER = new JsonParser();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Rule
	public WireMockRule wireMockRule1 = new WireMockRule( wireMockConfig().port( 0 )
			.httpsPort( 0 ) /* Automatic port selection */ );

	@Rule
	public WireMockRule wireMockRule2 = new WireMockRule( wireMockConfig().port( 0 ).httpsPort( 0 ) /* Automatic port selection */ );

	@Rule
	public TestConfigurationProvider testConfigurationProvider = new TestConfigurationProvider();

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2274")
	public void simple_http() throws Exception {
		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "StatusMessage";
		String responseBody = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.andMatching( httpProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 )
						.withStatusMessage( statusMessage )
						.withBody( responseBody ) ) );

		try ( ElasticsearchClientImplementor client = createClient() ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			assertThat( result.getStatusMessage() ).as( "status message" ).isEqualTo( statusMessage );
			assertJsonEquals( responseBody, result.getBody().toString() );

			wireMockRule1.verify(
					postRequestedFor( urlPathMatching( "/myIndex/myType" ) )
							.andMatching( httpProtocol() )
			);
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2274")
	public void simple_https() throws Exception {
		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "StatusMessage";
		String responseBody = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.andMatching( httpsProtocol() )
				.willReturn( elasticsearchResponse().withStatus( 200 )
						.withStatusMessage( statusMessage )
						.withBody( responseBody ) ) );

		try ( ElasticsearchClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.HOSTS, httpsHostAndPortFor( wireMockRule1 ) );
					properties.accept( ElasticsearchBackendSettings.PROTOCOL, "https" );
				}
		) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			assertThat( result.getStatusMessage() ).as( "status message" ).isEqualTo( statusMessage );
			assertJsonEquals( responseBody, result.getBody().toString() );

			wireMockRule1.verify(
					postRequestedFor( urlPathMatching( "/myIndex/myType" ) )
							.andMatching( httpsProtocol() )
			);
		}
	}

	@Test
	public void error() throws Exception {
		String payload = "{ \"foo\": \"bar\" }";
		String responseBody = "{ \"error\": \"ErrorMessageExplainingTheError\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse().withStatus( 500 )
						.withBody( responseBody )
				) );

		try ( ElasticsearchClientImplementor client = createClient() ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 500 );
			assertJsonEquals( responseBody, result.getBody().toString() );
		}
	}

	@Test
	public void unparseable() throws Exception {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse()
						.withBody( "'unparseable" )
						.withFixedDelay( 2000 )
				) );

		thrown.expect(
				isException( AssertionFailure.class )
						.causedBy( CompletionException.class )
						.causedBy( SearchException.class )
								.withMessage( "HSEARCH400089" )
						.causedBy( JsonSyntaxException.class )
				.build()
		);

		try ( ElasticsearchClientImplementor client = createClient() ) {
			doPost( client, "/myIndex/myType", payload );
		}
	}

	@Test
	public void timeout_read() throws Exception {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse()
						.withFixedDelay( 2000 )
				) );

		thrown.expect(
				isException( AssertionFailure.class )
						.causedBy( CompletionException.class )
						.causedBy( IOException.class )
				.build()
		);

		try ( ElasticsearchClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.READ_TIMEOUT, "1000" );
					properties.accept( ElasticsearchBackendSettings.REQUEST_TIMEOUT, "99999" );
				}
		) ) {
			doPost( client, "/myIndex/myType", payload );
		}
	}

	@Test
	public void timeout_request() throws Exception {
		String payload = "{ \"foo\": \"bar\" }";

		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse()
						.withFixedDelay( 2000 )
				) );

		thrown.expect(
				isException( AssertionFailure.class )
						.causedBy( CompletionException.class )
						.causedBy( SearchException.class )
						.withMessage( "Query took longer than expected" )
				.build()
		);

		try ( ElasticsearchClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.READ_TIMEOUT, "99999" );
					properties.accept( ElasticsearchBackendSettings.REQUEST_TIMEOUT, "1000" );
				}
		) ) {
			doPost( client, "/myIndex/myType", payload );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2235")
	public void multipleHosts() throws Exception {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.HOSTS, httpHostAndPortFor( wireMockRule1, wireMockRule2 ) );
				}
		) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
			wireMockRule2.verify( postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2469")
	public void multipleHosts_failover_serverError() throws Exception {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 503 ) ) );

		try ( ElasticsearchClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.HOSTS, httpHostAndPortFor( wireMockRule1, wireMockRule2 ) );
				}
		) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( 2, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 1, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );

			wireMockRule1.resetRequests();
			wireMockRule2.resetRequests();

			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2469")
	public void multipleHosts_failover_timeout() throws Exception {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withFixedDelay( 10_000 /* 10s => will time out */ ) ) );

		try ( ElasticsearchClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.HOSTS, httpHostAndPortFor( wireMockRule1, wireMockRule2 ) );
					// Use a timeout much higher than 1s, because wiremock can be really slow...
					properties.accept( ElasticsearchBackendSettings.READ_TIMEOUT, "5000" /* 5s */ );
				}
		) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

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
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2469")
	public void multipleHosts_failover_fault() throws Exception {
		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathMatching( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withFault( Fault.MALFORMED_RESPONSE_CHUNK ) ) );

		try ( ElasticsearchClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.HOSTS, httpHostAndPortFor( wireMockRule1, wireMockRule2 ) );
				}
		) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( 2, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 1, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );

			wireMockRule1.resetRequests();
			wireMockRule2.resetRequests();

			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathMatching( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2449")
	public void discovery_http() throws Exception {
		String nodesInfoResult = dummyNodeInfoResponse( wireMockRule1.port(), wireMockRule2.port() );

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

		try ( ElasticsearchClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.DISCOVERY_ENABLED, "true" );
					properties.accept( ElasticsearchBackendSettings.DISCOVERY_REFRESH_INTERVAL, "1" );
				}
		) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			/*
			 * Send requests repeatedly until both hosts have been targeted.
			 * This should happen pretty early (as soon as we sent two requests, actually),
			 * but there is always the risk that the sniffer would send a request
			 * between our own requests, effectively making our own requests target the same host
			 * (since the hosts are each targeted in turn).
			 */
			await().untilAsserted( () -> {
				ElasticsearchResponse newResult = doPost( client, "/myIndex/myType", payload );
				assertThat( newResult.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2736")
	public void discovery_https() throws Exception {
		String nodesInfoResult = dummyNodeInfoResponse( wireMockRule1.httpsPort(), wireMockRule2.httpsPort() );

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

		try ( ElasticsearchClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.HOSTS, httpsHostAndPortFor( wireMockRule1 ) );
					properties.accept( ElasticsearchBackendSettings.PROTOCOL, "https" );
					properties.accept( ElasticsearchBackendSettings.DISCOVERY_ENABLED, "true" );
					properties.accept( ElasticsearchBackendSettings.DISCOVERY_REFRESH_INTERVAL, "1" );
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
				ElasticsearchResponse newResult = doPost( client, "/myIndex/myType", payload );
				assertThat( newResult.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2453")
	@Category(RequiresNoAutomaticAuthenticationHeader.class)
	public void authentication() throws Exception {
		String username = "ironman";
		String password = "j@rV1s";

		String payload = "{ \"foo\": \"bar\" }";

		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType/_search" ) )
				.withBasicAuth( username, password )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.USERNAME, username );
					properties.accept( ElasticsearchBackendSettings.PASSWORD, password );
				}
		) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType/_search", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2453")
	public void authentication_error() throws Exception {
		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "StatusMessageUnauthorized";
		wireMockRule1.stubFor( post( urlPathMatching( "/myIndex/myType/_search" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse().withStatus( 401 /* Unauthorized */ )
						.withStatusMessage( statusMessage )
				) );

		try ( ElasticsearchClientImplementor client = createClient() ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType/_search", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 401 );
			assertThat( result.getStatusMessage() ).as( "status message" ).isEqualTo( statusMessage );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2453")
	public void authentication_http_password() throws Exception {
		String username = "ironman";
		String password = "j@rV1s";

		logged.expectMessage( "The password will be sent in clear text over the network" );

		try ( ElasticsearchClientImplementor client = createClient(
				properties -> {
					properties.accept( ElasticsearchBackendSettings.USERNAME, username );
					properties.accept( ElasticsearchBackendSettings.PASSWORD, password );
				}
		) ) {
			// Nothing to do here
		}
	}

	private ElasticsearchClientImplementor createClient() {
		return createClient( ignored -> { } );
	}

	private ElasticsearchClientImplementor createClient(Consumer<BiConsumer<String, Object>> additionalProperties) {
		ConfigurationPropertySource defaultBackendProperties =
				new ElasticsearchTckBackendHelper().createDefaultBackendSetupStrategy()
						.createBackendConfigurationPropertySource( testConfigurationProvider );
		ThreadPoolProvider threadPoolProvider = new ThreadPoolProviderImpl(
				new DefaultThreadProvider( ElasticsearchClientFactoryImplIT.class.getName() + ": " )
		);

		Map<String, Object> configurationOverride = new HashMap<>();
		// Redirect requests to Wiremock (rule 1 only by default)
		configurationOverride.put( ElasticsearchBackendSettings.HOSTS, httpHostAndPortFor( wireMockRule1 ) );
		configurationOverride.put( ElasticsearchBackendSettings.PROTOCOL, "http" );
		// Per-test overrides
		additionalProperties.accept( configurationOverride::put );
		ConfigurationPropertySource backendProperties =
				defaultBackendProperties.withOverride( ConfigurationPropertySource.fromMap( configurationOverride ) );

		Map<String, Object> beanResolverConfiguration = new HashMap<>();
		// Accept Wiremock's self-signed SSL certificates
		beanResolverConfiguration.put(
				EngineSpiSettings.Radicals.BEAN_CONFIGURERS,
				Collections.singletonList( elasticsearchSslBeanConfigurer() )
		);

		BeanResolver beanResolver = testConfigurationProvider.createBeanResolverForTest(
				ConfigurationPropertySource.fromMap( beanResolverConfiguration )

		);
		try ( BeanHolder<ElasticsearchClientFactory> factoryHolder =
				beanResolver.resolve( ElasticsearchClientFactoryImpl.REFERENCE ) ) {
			return factoryHolder.get().create(
					backendProperties, threadPoolProvider,
					GsonProvider.create( GsonBuilder::new, true )
			);
		}
	}

	private ElasticsearchResponse doPost(ElasticsearchClient client, String path, String payload) {
		try {
			return client.submit( buildRequest( ElasticsearchRequest.post(), path, payload ) ).join();
		}
		catch (RuntimeException e) {
			throw new AssertionFailure( "Unexpected exception during POST: " + e.getMessage(), e );
		}
	}

	private ElasticsearchRequest buildRequest(ElasticsearchRequest.Builder builder, String path, String payload) {
		for ( String pathComponent : path.split( "/" ) ) {
			if ( !pathComponent.isEmpty() ) {
				URLEncodedString fromString = URLEncodedString.fromString( pathComponent );
				builder = builder.pathComponent( fromString );
			}
		}
		if ( payload != null ) {
			builder = builder.body( JSON_PARSER.parse( payload ).getAsJsonObject() );
		}
		return builder.build();
	}

	private static String httpHostAndPortFor(WireMockRule ... rules) {
		return Arrays.stream( rules )
				.map( rule -> "localhost:" + rule.port() )
				.collect( Collectors.joining( "," ) );
	}

	private static String httpsHostAndPortFor(WireMockRule ... rules) {
		return Arrays.stream( rules )
				.map( rule -> "localhost:" + rule.httpsPort() )
				.collect( Collectors.joining( "," ) );
	}

	private static List<String> httpsHostsAndPortsFor(List<WireMockRule> rules) {
		return rules.stream()
				.map( ElasticsearchClientFactoryImplIT::httpsHostAndPortFor )
				.collect( Collectors.toList() );
	}

	private static ResponseDefinitionBuilder elasticsearchResponse() {
		return ResponseDefinitionBuilder.okForEmptyJson();
	}

	private String dummyNodeInfoResponse(int... ports) {
		JsonObject body = new JsonObject();
		body.addProperty( "cluster_name", "foo-cluster.local" );

		JsonObject nodes = new JsonObject();
		body.add( "nodes", nodes );
		int index = 1;
		for ( int port : ports ) {
			nodes.add( "hJLXmY_NTrCytiIMbX4_" + index + "g", dummyNodeInfo( port ) );
			++index;
		}

		return body.toString();
	}

	private JsonObject dummyNodeInfo(int port) {
		JsonObject node = new JsonObject();
		node.addProperty( "name", "nodeForPort" + port );
		node.addProperty( "version", ElasticsearchTestDialect.getClusterVersion() );

		JsonObject http = new JsonObject();
		node.add( "http", http );
		http.addProperty( "publish_address", "127.0.0.1:" + port );
		JsonArray boundAddresses = new JsonArray();
		http.add( "bound_address", boundAddresses );
		boundAddresses.add( "[::]:" + port );
		boundAddresses.add( "127.0.0.1:" + port );

		JsonArray roles = new JsonArray();
		node.add( "roles", roles );
		roles.add( "ingest" );
		roles.add( "master" );
		roles.add( "data" );
		roles.add( "ml" );

		node.add( "plugins", new JsonObject() );

		return node;
	}

	private static BeanConfigurer elasticsearchSslBeanConfigurer() {
		return context -> {
			context.assignRole(
					ElasticsearchHttpClientConfigurer.class,
					BeanReference.ofInstance( new ElasticsearchHttpClientConfigurer() {
						@Override
						public void configure(HttpAsyncClientBuilder builder,
								ConfigurationPropertySource propertySource) {
							builder.setSSLHostnameVerifier( NoopHostnameVerifier.INSTANCE );
							builder.setSSLContext( buildAllowAnythingSSLContext() );
						}
					} )
			);
		};
	}

	private static SSLContext buildAllowAnythingSSLContext() {
		try {
			return SSLContexts.custom().loadTrustMaterial( null, new TrustSelfSignedStrategy() ).build();
		}
		catch (Exception e) {
			throw new AssertionFailure( "Unexpected exception", e );
		}
	}

}
