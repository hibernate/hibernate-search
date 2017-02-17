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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.eclipse.jetty.http.HttpHeader;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchServiceImpl;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchServiceImplTest {

	private static final String CLIENT_PROPERTY_PREFIX = "hibernate.search.default.";

	private static final int PORT_1 = 9201;
	private static final String URI_1 = "http://localhost:" + PORT_1;

	private static final int PORT_2 = 9202;
	private static final String URI_2 = "http://localhost:" + PORT_2;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Rule
	public WireMockRule wireMockRule1 = new WireMockRule( PORT_1 );

	@Rule
	public WireMockRule wireMockRule2 = new WireMockRule( PORT_2 );

	private ElasticsearchServiceImpl esService = new ElasticsearchServiceImpl();

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2274")
	public void simple() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, URI_1 );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathEqualTo( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try {
			esService.start( configuration.getProperties(), new BuildContextForTest( configuration ) );

			Response result = doPost( "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );
		}
		finally {
			esService.stop();
		}
	}

	@Test
	public void error() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, URI_1 );

		String payload = "{ \"foo\": \"bar\" }";
		String errorMessage = "ErrorMessageExplainingTheError";
		wireMockRule1.stubFor( post( urlPathEqualTo( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse().withStatus( 500 )
						.withBody( "{ \"error\": \"" + errorMessage + "\" }" )
				) );

		try {
			esService.start( configuration.getProperties(), new BuildContextForTest( configuration ) );

			ResponseException exception = null;
			try {
				doPost( "/myIndex/myType", payload );
			}
			catch (ResponseException e) {
				exception = e;
			}
			assertThat( exception ).as( "ResponseException" ).isNotNull();
			Response result = exception.getResponse();
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 500 );
			assertThat( IOUtils.toString( result.getEntity().getContent() ) ).as( "response body" ).contains( errorMessage );
		}
		finally {
			esService.stop();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2235")
	public void multipleHosts() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, URI_1 + " " + URI_2 );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathEqualTo( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathEqualTo( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try {
			esService.start( configuration.getProperties(), new BuildContextForTest( configuration ) );

			Response result = doPost( "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );
			wireMockRule2.verify( postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );
		}
		finally {
			esService.stop();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2449")
	public void discovery() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, URI_1 )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.DISCOVERY_ENABLED, "true" )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.DISCOVERY_REFRESH_INTERVAL, "1" );

		String nodesInfoResult = dummyNodeInfoResponse( PORT_1, PORT_2 );

		wireMockRule1.stubFor( get( WireMock.urlMatching( "/_nodes.*" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withBody( nodesInfoResult ) ) );
		wireMockRule2.stubFor( get( WireMock.urlMatching( "/_nodes.*" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ).withBody( nodesInfoResult ) ) );

		String payload = "{ \"foo\": \"bar\" }";
		wireMockRule1.stubFor( post( urlPathEqualTo( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		wireMockRule2.stubFor( post( urlPathEqualTo( "/myIndex/myType" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try {
			esService.start( configuration.getProperties(), new BuildContextForTest( configuration ) );

			Response result = doPost( "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			Thread.sleep( 2000 ); // Wait for the refresh to occur

			result = doPost( "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );
			result = doPost( "/myIndex/myType", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );
			wireMockRule2.verify( postRequestedFor( urlPathEqualTo( "/myIndex/myType" ) ) );
		}
		finally {
			esService.stop();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2453")
	public void authentication() throws Exception {
		String username = "ironman";
		String password = "j@rV1s";
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, URI_1 )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_USERNAME, username )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_PASSWORD, password );

		String payload = "{ \"foo\": \"bar\" }";

		/*
		 * Jest (actually, the Apache HTTP client) always tries an unauthenticated request first,
		 * and only provides an authentication token if it gets a 401 status.
		 * Thus we must stub the response for unauthenticated requests too.
		 */
		wireMockRule1.stubFor( post( urlPathEqualTo( "/myIndex/myType/_search" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse().withStatus( 401 )
						.withHeader( HttpHeader.WWW_AUTHENTICATE.asString(), "Basic" ) )
				);

		wireMockRule1.stubFor( post( urlPathEqualTo( "/myIndex/myType/_search" ) )
				.withBasicAuth( username, password )
				.withRequestBody( equalToJson( payload ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try {
			esService.start( configuration.getProperties(), new BuildContextForTest( configuration ) );

			Response result = doPost( "/myIndex/myType/_search", payload );
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 200 );

			wireMockRule1.verify( postRequestedFor( urlPathEqualTo( "/myIndex/myType/_search" ) ) );
		}
		finally {
			esService.stop();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2453")
	public void authentication_error() throws Exception {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, URI_1 );

		String payload = "{ \"foo\": \"bar\" }";
		String statusMessage = "StatusMessageUnauthorized";
		wireMockRule1.stubFor( post( urlPathEqualTo( "/myIndex/myType/_search" ) )
				.withRequestBody( equalToJson( payload ) )
				.willReturn(
						elasticsearchResponse().withStatus( 401 /* Unauthorized */ )
						.withStatusMessage( statusMessage )
				) );

		try {
			esService.start( configuration.getProperties(), new BuildContextForTest( configuration ) );

			ResponseException exception = null;
			try {
				doPost( "/myIndex/myType/_search", payload );
			}
			catch (ResponseException e) {
				exception = e;
			}
			assertThat( exception ).as( "ResponseException" ).isNotNull();
			Response result = exception.getResponse();
			assertThat( result.getStatusLine().getStatusCode() ).as( "status code" ).isEqualTo( 401 );
			assertThat( result.getStatusLine().getReasonPhrase() ).as( "reason phrase" ).contains( statusMessage );
		}
		finally {
			esService.stop();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2453")
	public void authentication_http_password() {
		String username = "ironman";
		String password = "j@rV1s";
		String httpUri = "http://foo.com/";
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI, httpUri )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_USERNAME, username )
				.addProperty( CLIENT_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_PASSWORD, password );

		logged.expectMessage( "HSEARCH400073", httpUri );

		try {
			esService.start( configuration.getProperties(), new BuildContextForTest( configuration ) );
		}
		finally {
			esService.stop();
		}
	}

	private Response doPost(String path, String payload) throws IOException, ResponseException {
		return esService.getClient().performRequest(
				"POST", path, Collections.emptyMap(),
				payload == null ? null : new StringEntity( payload, ContentType.APPLICATION_JSON )
				);
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
