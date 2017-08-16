/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test.client;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.client.impl.DefaultElasticsearchClientFactory;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest.Builder;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.testutil.junit.SkipOnAWS;
import org.hibernate.search.elasticsearch.testutil.junit.SkipWithoutAWS;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;

/**
 * Test that we manage to compute the content-length of Elasticsearch
 * requests appropriately (for small requests, or when computing digests, e.g. on AWS),
 * and use the "chunked" transfer-encoding otherwise.
 *
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-2849")
public class ElasticsearchContentLengthIT {

	private static final JsonObject BODY_PART = new JsonParser().parse( "{ \"foo\": \"bar\" }" ).getAsJsonObject();

	private static final int BODY_PART_BYTE_SIZE;
	static {
		Gson gson = new Gson();
		try ( ByteArrayOutputStream out = new ByteArrayOutputStream();
				Writer writer = new OutputStreamWriter( out, StandardCharsets.UTF_8 );
				JsonWriter jsonWriter = new JsonWriter( writer ) ) {
			gson.toJson( BODY_PART, jsonWriter );
			writer.write( '\n' ); // Account for EOL at the end of each body part
			jsonWriter.flush();
			BODY_PART_BYTE_SIZE = out.size();
		}
		catch (IOException e) {
			throw new IllegalStateException( "Error while initializing a constant", e );
		}
	}

	private static final int BUFFER_LIMIT = 1024;

	@Rule
	public WireMockRule wireMockRule = new WireMockRule( wireMockConfig().port( 0 ).httpsPort( 0 ) /* Automatic port selection */ );

	private DefaultElasticsearchClientFactory clientFactory = new DefaultElasticsearchClientFactory();

	@After
	public void stop() {
		clientFactory.stop();
	}

	@Test
	public void tinyPayload() throws Exception {
		SearchConfigurationForTest configuration = createConfiguration();

		wireMockRule.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			doPost( client, "/myIndex/myType", produceBody( 1 ) );
			wireMockRule.verify(
					postRequestedFor( urlPathLike( "/myIndex/myType" ) )
							.withoutHeader( "Transfer-Encoding" )
							.withHeader( "Content-length", equalTo( String.valueOf( BODY_PART_BYTE_SIZE ) ) )
					);
		}
	}

	@Test
	public void payloadJustBelowBufferSize() throws Exception {
		SearchConfigurationForTest configuration = createConfiguration();

		wireMockRule.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		int bodyPartCount = BUFFER_LIMIT / BODY_PART_BYTE_SIZE - 1;

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			doPost( client, "/myIndex/myType", produceBody( bodyPartCount ) );
			wireMockRule.verify(
					postRequestedFor( urlPathLike( "/myIndex/myType" ) )
							.withoutHeader( "Transfer-Encoding" )
							.withHeader( "Content-length", equalTo( String.valueOf( bodyPartCount * BODY_PART_BYTE_SIZE ) ) )
					);
		}
	}

	@Test
	@Category(SkipOnAWS.class)
	public void payloadJustAboveBufferSize_nonAws() throws Exception {
		SearchConfigurationForTest configuration = createConfiguration();

		wireMockRule.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		int bodyPartCount = BUFFER_LIMIT / BODY_PART_BYTE_SIZE + 1;

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			doPost( client, "/myIndex/myType", produceBody( bodyPartCount ) );
			wireMockRule.verify(
					postRequestedFor( urlPathLike( "/myIndex/myType" ) )
							.withHeader( "Transfer-Encoding", equalTo( "chunked" ) )
							.withoutHeader( "Content-length" )
					);
		}
	}

	@Test
	@Category(SkipWithoutAWS.class)
	public void payloadJustAboveBufferSize_aws() throws Exception {
		SearchConfigurationForTest configuration = createConfiguration();

		wireMockRule.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		int bodyPartCount = BUFFER_LIMIT / BODY_PART_BYTE_SIZE + 1;

		try ( ElasticsearchClient client = createClient( configuration ) ) {
			doPost( client, "/myIndex/myType", produceBody( bodyPartCount ) );
			wireMockRule.verify(
					postRequestedFor( urlPathLike( "/myIndex/myType" ) )
							.withoutHeader( "Transfer-Encoding" )
							.withHeader( "Content-length", equalTo( String.valueOf( bodyPartCount * BODY_PART_BYTE_SIZE ) ) )
					);
		}
	}

	private SearchConfigurationForTest createConfiguration() {
		return new SearchConfigurationForTest()
				.addProperty( "hibernate.search.default." + ElasticsearchEnvironment.SERVER_URI, httpUrlFor( wireMockRule ) );
	}

	private ElasticsearchClient createClient(SearchConfiguration configuration) {
		clientFactory.start( configuration.getProperties(), new BuildContextForTest( configuration ) );
		Properties maskedProperties = new MaskedProperty( configuration.getProperties(), "hibernate.search.default" );
		return clientFactory.create( maskedProperties );
	}

	private ElasticsearchResponse doPost(ElasticsearchClient client, String path, Collection<JsonObject> bodyParts) {
		return client.submit( buildRequest( ElasticsearchRequest.post(), path, bodyParts ) ).join();
	}

	private ElasticsearchRequest buildRequest(Builder builder, String path, Collection<JsonObject> bodyParts) {
		for ( String pathComponent : path.split( "/" ) ) {
			if ( !pathComponent.isEmpty() ) {
				URLEncodedString fromString = URLEncodedString.fromString( pathComponent );
				builder = builder.pathComponent( fromString );
			}
		}
		for ( JsonObject bodyPart : bodyParts ) {
			builder = builder.body( bodyPart );
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

	private static Collection<JsonObject> produceBody(int bodyPartCount) {
		Collection<JsonObject> result = new ArrayList<>( bodyPartCount );
		for ( int i = 0; i < bodyPartCount; i++ ) {
			result.add( BODY_PART );
		}
		return result;
	}

}
