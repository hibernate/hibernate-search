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

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.client.impl.DefaultElasticsearchClientFactory;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest.Builder;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.BytemanHelper;
import org.hibernate.search.testsupport.BytemanHelper.BytemanAccessor;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.concurrency.Poller;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Ignore;
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

/**
 * @author Yoann Rodiere
 */
@RunWith(BMUnitRunner.class)
public class DefaultElasticsearchClientFactoryTest {

	private static final Poller POLLER = Poller.milliseconds( 10_000, 500 );

	private static final JsonParser JSON_PARSER = new JsonParser();

	private static final String CLIENT_SCOPE_NAME = "default";
	private static final String CLIENT_PROPERTY_PREFIX = "hibernate.search.default.";

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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2274")
	public void simple() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchClient client = clientFactory.create( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			Response result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
		}
	}

	@Test
	public void error() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) );

		String payload = "{ \"foo\": \"bar\" }";
		String errorMessage = "ErrorMessageExplainingTheError";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse().withStatus( 500 )
						.withBody( "{ \"error\": \"" + errorMessage + "\" }" )
				) );

		try ( ElasticsearchClient client = clientFactory.create( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			Response result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 500 );
			assertThat( IOUtils.toString( result.getEntity().getContent() ) ).as( "response body" ).contains( errorMessage );
		}
	}

	@Test
	public void timeout_read() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_READ_TIMEOUT, "1000" )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_REQUEST_TIMEOUT, "99999" );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse()
						.withFixedDelay( 2000 )
				) );

		thrown.expect( IOException.class );

		try ( ElasticsearchClient client = clientFactory.create( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			doPost( client, "/myIndex/myType", payload );
		}
	}

	@Test
	public void timeout_request() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_READ_TIMEOUT, "99999" )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_REQUEST_TIMEOUT, "1000" );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse()
						.withFixedDelay( 2000 )
				) );

		thrown.expect( IOException.class );

		try ( ElasticsearchClient client = clientFactory.create( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			doPost( client, "/myIndex/myType", payload );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2235")
	public void multipleHosts() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI,
						httpUrlFor( wireMockRule1 ) + " " + httpUrlFor( wireMockRule2 ) );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchClient client = clientFactory.create( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			Response result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
			wireMockRule2.verify( postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2469")
	public void multipleHosts_failover_serverError() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI,
						httpUrlFor( wireMockRule1 ) + " " + httpUrlFor( wireMockRule2 ) );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 503 ) ) );

		try ( ElasticsearchClient client = clientFactory.create( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			Response result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( 2, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 1, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );

			wireMockRule1.resetRequests();
			wireMockRule2.resetRequests();

			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2469")
	public void multipleHosts_failover_timeout() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI,
						httpUrlFor( wireMockRule1 ) + " " + httpUrlFor( wireMockRule2 ) )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_READ_TIMEOUT, "1000" /* 1s */ );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withFixedDelay( 10_000 /* 10s => will time out */ ) ) );

		try ( ElasticsearchClient client = clientFactory.create( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			Response result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

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
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2469")
	public void multipleHosts_failover_fault() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI,
						httpUrlFor( wireMockRule1 ) + " " + httpUrlFor( wireMockRule2 ) )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_READ_TIMEOUT, "1000" /* 1s */ );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withFault( Fault.MALFORMED_RESPONSE_CHUNK ) ) );

		try ( ElasticsearchClient client = clientFactory.create( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			Response result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( 2, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 1, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );

			wireMockRule1.resetRequests();
			wireMockRule2.resetRequests();

			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathLike( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2469")
	public void multipleHosts_failover_serverError() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, URI_1 + " " + URI_2 );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathEqualTo( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathEqualTo( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 503 ) ) );

		try ( RestClient client = clientFactory.createClient( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			Response result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( 2, postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 1, postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );

			wireMockRule1.resetRequests();
			wireMockRule2.resetRequests();

			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2469")
	public void multipleHosts_failover_timeout() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, URI_1 + " " + URI_2 )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_READ_TIMEOUT, "1000" /* 1s */ );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathEqualTo( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathEqualTo( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withFixedDelay( 10_000 /* 10s => will time out */ ) ) );

		try ( RestClient client = clientFactory.createClient( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			Response result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( 2, postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );
			/*
			 * Wiremock introduces the delay *before* registering the request to the journal,
			 * so we should have no request in the journal if we time out.
			 */
			wireMockRule2.verify( 0, postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );

			wireMockRule1.resetRequests();
			wireMockRule2.resetRequests();

			/*
			 * Remove the failure in the previously failing node,
			 * so that we can detect if requests are sent to this node.
			 */
			wireMockRule2.resetMappings();
			wireMockRule2.stubFor( post( urlPathEqualTo( "/myIndex/myType" ) )
					.withRequestBody( equalToJson( payload ) )
					.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2469")
	public void multipleHosts_failover_fault() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, URI_1 + " " + URI_2 )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_READ_TIMEOUT, "1000" /* 1s */ );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathEqualTo( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathEqualTo( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withFault( Fault.MALFORMED_RESPONSE_CHUNK ) ) );

		try ( RestClient client = clientFactory.createClient( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			Response result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( 2, postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 1, postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );

			wireMockRule1.resetRequests();
			wireMockRule2.resetRequests();

			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			// Must not use the failing node anymore
			wireMockRule1.verify( 2, postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );
			wireMockRule2.verify( 0, postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2449")
	public void discovery() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.DISCOVERY_ENABLED, "true" )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.DISCOVERY_REFRESH_INTERVAL, "1" );

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

		try ( ElasticsearchClient client = clientFactory.create( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			Response result = doPost( client, "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			/*
			 * Send requests repeatedly until both hosts have been targeted.
			 * This should happen pretty early (as soon as we sent two requests, actually),
			 * but there is always the risk that the sniffer would send a request
			 * between our own requests, effectively making our own requests target the same host
			 * (since the hosts are each targeted in turn).
			 */
			POLLER.pollAssertion( () -> {
				doPost( client, "/myIndex/myType", payload );
				assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

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
	@Ignore // HSEARCH-2481 Byteman-based tests executed in the Elasticsearch module won't work
	public void discoveryScheme() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				// Need to use HTTP here, so that the sniffer can at least retrieve the host list
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.DISCOVERY_ENABLED, "true" )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.DISCOVERY_REFRESH_INTERVAL, "1" )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.DISCOVERY_SCHEME, "https" );

		String nodesInfoResult = dummyNodeInfoResponse(
				wireMockRule1.httpsPort(),
				wireMockRule2.httpsPort()
				);

		wireMockRule1.stubFor( get( WireMock.urlMatching( "/_nodes.*" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withBody( nodesInfoResult ) ) );

		try ( ElasticsearchClient client = clientFactory.create( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
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
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_USERNAME, username )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_PASSWORD, password );

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

		try ( ElasticsearchClient client = clientFactory.create( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			Response result = doPost( client, "/myIndex/myType/_search", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( postRequestedFor( urlPathLike( "/myIndex/myType/_search" ) ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2453")
	public void authentication_error() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule1 ) );

		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "StatusMessageUnauthorized";
		wireMockRule1.stubFor( post( urlPathLike( "/myIndex/myType/_search" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse().withStatus( 401 /* Unauthorized */ )
						.withStatusMessage( statusMessage )
				) );

		try ( ElasticsearchClient client = clientFactory.create( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			Response result = doPost( client, "/myIndex/myType/_search", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 401 );
			assertThat( result.getStatusLine().getReasonPhrase() ).as( "reason phrase" ).contains( statusMessage );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2453")
	public void authentication_http_password() throws Exception {
		String username = "ironman";
		String password = "j@rV1s";
		String httpUri = "http://foo.com/";
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, httpUri )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_USERNAME, username )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_PASSWORD, password );

		logged.expectMessage( "HSEARCH400073", httpUri );

		try ( ElasticsearchClient client = clientFactory.create( CLIENT_SCOPE_NAME, configuration.getProperties() ) ) {
			// Nothing to do here
		}
	}

	private Response doPost(ElasticsearchClient client, String path, String payload) throws IOException, ResponseException {
		return client.execute( buildRequest( ElasticsearchRequest.post(), path, payload ) );
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
