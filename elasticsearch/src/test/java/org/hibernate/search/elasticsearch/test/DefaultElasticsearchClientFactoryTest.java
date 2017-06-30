/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.search.test.util.impl.ExceptionMatcherBuilder.isException;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.http.HttpHeader;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.client.impl.DefaultElasticsearchClientFactory;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest.Builder;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.util.JsonHelper;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.BytemanHelper;
import org.hibernate.search.testsupport.BytemanHelper.BytemanAccessor;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.concurrency.Poller;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * @author Yoann Rodiere
 */
@RunWith(BMUnitRunner.class)
public class DefaultElasticsearchClientFactoryTest {

	private static final Poller POLLER = Poller.milliseconds( 10_000, 500 );

	private static final JsonParser JSON_PARSER = new JsonParser();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Rule
	public BytemanAccessor byteman = BytemanHelper.createAccessor();

	@Rule
	public WireMockRule wireMockRule1 = new WireMockRule( wireMockConfig().port( 0 ).httpsPort( 0 ) /* Automatic port selection */ );

	@Rule
	public WireMockRule wireMockRule2 = new WireMockRule( wireMockConfig().port( 0 ).httpsPort( 0 ) /* Automatic port selection */ );

	private DefaultElasticsearchClientFactory clientFactory = new DefaultElasticsearchClientFactory();

	@After
	public void stop() {
		clientFactory.stop();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2274")
	public void simple() throws Exception {
		SearchConfigurationForTest configuration = SearchConfigurationForTest.noTestDefaults()
				.addProperty( ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) );

		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "StatusMessage";
		String responseBody = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 )
						.withStatusMessage( statusMessage )
						.withBody( responseBody )) );

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			assertThat( result.getStatusMessage() ).as( "status message" ).isEqualTo( statusMessage );
			JsonHelper.assertJsonEquals( responseBody, result.getBody().toString() );

			wireMockRule1.verify( postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
		}
	}

	@Test
	public void error() throws Exception {
		SearchConfigurationForTest configuration = SearchConfigurationForTest.noTestDefaults()
				.addProperty( ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) );

		String payload = "{ \"foo\": \"bar\" }";
		String responseBody = "{ \"error\": \"ErrorMessageExplainingTheError\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse().withStatus( 500 )
						.withBody( responseBody )
				) );

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 500 );
			JsonHelper.assertJsonEquals( responseBody, result.getBody().toString() );
		}
	}

	@Test
	public void unparseable() throws Exception {
		SearchConfigurationForTest configuration = SearchConfigurationForTest.noTestDefaults()
				.addProperty( ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse()
						.withBody( "'unparseable" )
						.withFixedDelay( 2000 )
				) );

		thrown.expect(
				isException( CompletionException.class )
						.causedBy( SearchException.class )
								.withMessage( "HSEARCH400089" )
						.causedBy( JsonSyntaxException.class )
				.build()
		);

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			doPost( client, "/myIndex/myType", payload );
		}
	}

	@Test
	public void timeout_read() throws Exception {
		SearchConfigurationForTest configuration = SearchConfigurationForTest.noTestDefaults()
				.addProperty( ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) )
				.addProperty( ElasticsearchEnvironment.SERVER_READ_TIMEOUT, "1000" )
				.addProperty( ElasticsearchEnvironment.SERVER_REQUEST_TIMEOUT, "99999" );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse()
						.withFixedDelay( 2000 )
				) );

		thrown.expect(
				isException( CompletionException.class )
						.causedBy( IOException.class )
				.build()
		);

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			doPost( client, "/myIndex/myType", payload );
		}
	}

	@Test
	public void timeout_request() throws Exception {
		SearchConfigurationForTest configuration = SearchConfigurationForTest.noTestDefaults()
				.addProperty( ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) )
				.addProperty( ElasticsearchEnvironment.SERVER_READ_TIMEOUT, "99999" )
				.addProperty( ElasticsearchEnvironment.SERVER_REQUEST_TIMEOUT, "1000" );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse()
						.withFixedDelay( 2000 )
				) );

		thrown.expect(
				isException( CompletionException.class )
						.causedBy( TimeoutException.class )
				.build()
		);

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			doPost( client, "/myIndex/myType", payload );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2235")
	public void multipleHosts() throws Exception {
		SearchConfigurationForTest configuration = SearchConfigurationForTest.noTestDefaults()
				.addProperty( ElasticsearchEnvironment.SERVER_URI,
						httpUrlFor( wireMockRule1 ) + " " + httpUrlFor( wireMockRule2 ) );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
			wireMockRule2.verify( postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2469")
	public void multipleHosts_failover_serverError() throws Exception {
		SearchConfigurationForTest configuration = SearchConfigurationForTest.noTestDefaults()
				.addProperty( ElasticsearchEnvironment.SERVER_URI,
						httpUrlFor( wireMockRule1 ) + " " + httpUrlFor( wireMockRule2 ) );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 503 ) ) );

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( 2, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 1, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );

			wireMockRule1.resetRequests();
			wireMockRule2.resetRequests();

			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2469")
	public void multipleHosts_failover_timeout() throws Exception {
		SearchConfigurationForTest configuration = SearchConfigurationForTest.noTestDefaults()
				.addProperty( ElasticsearchEnvironment.SERVER_URI,
						httpUrlFor( wireMockRule1 ) + " " + httpUrlFor( wireMockRule2 ) )
				.addProperty( ElasticsearchEnvironment.SERVER_READ_TIMEOUT, "1000" /* 1s */ );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withFixedDelay( 10_000 /* 10s => will time out */ ) ) );

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( 2, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
			/*
			 * Wiremock introduces the delay *before* registering the request to the journal,
			 * so we should have no request in the journal if we time out.
			 */
			wireMockRule2.verify( 0, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );

			wireMockRule1.resetRequests();
			wireMockRule2.resetRequests();

			/*
			 * Remove the failure in the previously failing node,
			 * so that we can detect if requests are sent to this node.
			 */
			wireMockRule2.resetMappings();
			wireMockRule2.stubFor( post( urlPathLike( "/myIndex/myType" ) )
					.withRequestBody( equalToJson( payload ) )
					.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2469")
	public void multipleHosts_failover_fault() throws Exception {
		SearchConfigurationForTest configuration = SearchConfigurationForTest.noTestDefaults()
				.addProperty( ElasticsearchEnvironment.SERVER_URI,
						httpUrlFor( wireMockRule1 ) + " " + httpUrlFor( wireMockRule2 ) );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withFault( Fault.MALFORMED_RESPONSE_CHUNK ) ) );

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( 2, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 1, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );

			wireMockRule1.resetRequests();
			wireMockRule2.resetRequests();

			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2449")
	public void discovery() throws Exception {
		SearchConfigurationForTest configuration = SearchConfigurationForTest.noTestDefaults()
				.addProperty( ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) )
				.addProperty( ElasticsearchEnvironment.DISCOVERY_ENABLED, "true" )
				.addProperty( ElasticsearchEnvironment.DISCOVERY_REFRESH_INTERVAL, "1" );

		String nodesInfoResult = dummyNodeInfoResponse( wireMockRule1.port(), wireMockRule2.port() );

		wireMockRule1.stubFor( get( WireMock.urlMatching( "/_nodes.*" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withBody( nodesInfoResult ) ) );
		wireMockRule2.stubFor( get( WireMock.urlMatching( "/_nodes.*" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withBody( nodesInfoResult ) ) );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			/*
			 * Send requests repeatedly until both hosts have been targeted.
			 * This should happen pretty early (as soon as we sent two requests, actually),
			 * but there is always the risk that the sniffer would send a request
			 * between our own requests, effectively making our own requests target the same host
			 * (since the hosts are each targeted in turn).
			 */
			POLLER.pollAssertion( () -> {
				doPost( client, "/myIndex/myType", payload );
				assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );

				wireMockRule1.verify( postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
				wireMockRule2.verify( postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
			} );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2736")
	@BMRule(
		name = "trackHttpsHostsDiscovery",
		targetClass = "org.elasticsearch.client.RestClient",
		targetMethod = "setHosts(HttpHost[])",
		helper = "org.hibernate.search.testsupport.BytemanHelper",
		binding = "host0 : HttpHost = $1.length >= 1 ? $1[0] : null, host1 : HttpHost = $1.length >= 2 ? $1[1] : null;",
		condition = "host0 != null && host0.getSchemeName().equals( \"https\" )"
				+ " || host1 != null && host1.getSchemeName().equals( \"https\" )",
		action = "pushEvent( \"https\" )"
	)
	public void discoveryScheme() throws Exception {
		SearchConfigurationForTest configuration = SearchConfigurationForTest.noTestDefaults()
				// Need to use HTTP here, so that the sniffer can at least retrieve the host list
				.addProperty( ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) )
				.addProperty( ElasticsearchEnvironment.DISCOVERY_ENABLED, "true" )
				.addProperty( ElasticsearchEnvironment.DISCOVERY_REFRESH_INTERVAL, "1" )
				.addProperty( ElasticsearchEnvironment.DISCOVERY_SCHEME, "https" );

		String nodesInfoResult = dummyNodeInfoResponse(
				wireMockRule1.httpsPort(),
				wireMockRule2.httpsPort()
				);

		wireMockRule1.stubFor( get( WireMock.urlMatching( "/_nodes.*" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withBody( nodesInfoResult ) ) );

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			/*
			 * We can't use a valid SSL/TLS certificate, so we just check, using Byteman,
			 * that the sniffer found some HTTPS hosts at some point.
			 */
			POLLER.pollAssertion( () -> {
				assertThat( byteman.isEventStackEmpty() ? null : byteman.consumeNextRecordedEvent() )
						.as( "An event confirming that HTTPS was used" ).isEqualTo( "https" );
			} );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2453")
	public void authentication() throws Exception {
		String username = "ironman";
		String password = "j@rV1s";
		SearchConfigurationForTest configuration = SearchConfigurationForTest.noTestDefaults()
				.addProperty( ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) )
				.addProperty( ElasticsearchEnvironment.SERVER_USERNAME, username )
				.addProperty( ElasticsearchEnvironment.SERVER_PASSWORD, password );

		String payload = "{ \"foo\": \"bar\" }";

		/*
		 * Jest (actually, the Apache HTTP client) always tries an unauthenticated request first,
		 * and only provides an authentication token if it gets a 401 status.
		 * Thus we must stub the response for unauthenticated requests too.
		 */
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType/_search" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse().withStatus( 401 )
						.withHeader( HttpHeader.WWW_AUTHENTICATE.asString(), "Basic" ) )
				);

		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType/_search" ) )
				.withBasicAuth( username, password )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			ElasticsearchResponse result = doPost( client, "/myIndex/myType/_search", payload );
			assertThat( result.getStatusCode() ).as( "status code" ).isEqualTo( 200 );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2453")
	public void authentication_error() throws Exception {
		SearchConfigurationForTest configuration = SearchConfigurationForTest.noTestDefaults()
				.addProperty( ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) );

		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "StatusMessageUnauthorized";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType/_search" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse().withStatus( 401 /* Unauthorized */ )
						.withStatusMessage( statusMessage )
				) );

		try ( ElasticsearchClient client = createClient( configuration ) ) {
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
		String httpUri = "http://foo.com/";
		SearchConfigurationForTest configuration = SearchConfigurationForTest.noTestDefaults()
				.addProperty( ElasticsearchEnvironment.SERVER_URI, httpUri )
				.addProperty( ElasticsearchEnvironment.SERVER_USERNAME, username )
				.addProperty( ElasticsearchEnvironment.SERVER_PASSWORD, password );

		logged.expectMessage( "HSEARCH400073", httpUri );

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			// Nothing to do here
		}
	}

	private ElasticsearchClient createClient(SearchConfiguration configuration ) {
		SearchConfigurationForTest searchConfiguration = SearchConfigurationForTest.noTestDefaults();
		clientFactory.start( searchConfiguration.getProperties(), new BuildContextForTest( searchConfiguration ) );
		return clientFactory.create( configuration.getProperties() );
	}

	private ElasticsearchResponse doPost(ElasticsearchClient client, String path, String payload) {
		return client.submit( buildRequest( ElasticsearchRequest.post(), path, payload ) ).join();
	}

	private ElasticsearchRequest buildRequest(Builder builder, String path, String payload) {
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

	private static String httpUrlFor(WireMockRule rule) {
		return "http://localhost:" + rule.port();
	}

	private static UrlPathPattern urlPathLike(String path) {
		return urlPathMatching( path + "/?" );
	}

	private static ResponseDefinitionBuilder elasticsearchResponse() {
		return ResponseDefinitionBuilder.okForEmptyJson();
	}

	private String dummyNodeInfoResponse(int... ports) {
		JsonBuilder.Object nodesBuilder = JsonBuilder.object();
		int index = 1;
		for ( int port : ports ) {
			nodesBuilder.add( "hJLXmY_NTrCytiIMbX4_" + index + "g", dummyNodeInfo( port ) );
			++index;
		}

		return JsonBuilder.object()
				.addProperty( "cluster_name", "foo-cluster.local" )
				.add( "nodes", nodesBuilder.build() )
				.build()
				.toString();
	}

	private JsonObject dummyNodeInfo(int port) {
		return JsonBuilder.object()
				.addProperty( "name", "nodeForPort" + port )
				.add( "http", JsonBuilder.object()
						.addProperty( "publish_address", "localhost:" + port )
				)
				.add( "plugins", JsonBuilder.array().build() )
				.build();
	}

}
