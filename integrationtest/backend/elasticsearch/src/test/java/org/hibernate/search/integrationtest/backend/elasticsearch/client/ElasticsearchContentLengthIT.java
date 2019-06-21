/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.client;

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
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientFactoryImpl;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.impl.DefaultGsonProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.categories.RequiresNoRequestPostProcessing;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.categories.RequiresRequestPostProcessing;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;

/**
 * Test that we manage to compute the content-length of Elasticsearch
 * requests appropriately (for small requests, or when computing digests, e.g. on AWS),
 * and use the "chunked" transfer-encoding otherwise.
 */
@TestForIssue(jiraKey = "HSEARCH-2849")
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.client.ElasticsearchContentLengthIT")
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
	public WireMockRule wireMockRule =
			new WireMockRule( wireMockConfig().port( 0 ).httpsPort( 0 ) /* Automatic port selection */ );

	@Rule
	public TestConfigurationProvider testConfigurationProvider = new TestConfigurationProvider();

	@Test
	public void tinyPayload() throws Exception {
		wireMockRule.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchClientImplementor client = createClient() ) {
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
		wireMockRule.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		int bodyPartCount = BUFFER_LIMIT / BODY_PART_BYTE_SIZE - 1;

		try ( ElasticsearchClientImplementor client = createClient() ) {
			doPost( client, "/myIndex/myType", produceBody( bodyPartCount ) );
			wireMockRule.verify(
					postRequestedFor( urlPathLike( "/myIndex/myType" ) )
							.withoutHeader( "Transfer-Encoding" )
							.withHeader( "Content-length", equalTo( String.valueOf( bodyPartCount * BODY_PART_BYTE_SIZE ) ) )
					);
		}
	}

	/**
	 * When the request is big and is not post-processed (i.e. no AWS integration),
	 * we can "stream" it to the remote cluster, and avoid storing it entirely in memory.
	 */
	@Test
	@Category(RequiresNoRequestPostProcessing.class)
	public void payloadJustAboveBufferSize_noRequestPostProcessing() throws Exception {
		wireMockRule.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		int bodyPartCount = BUFFER_LIMIT / BODY_PART_BYTE_SIZE
				+ 1 // The last body part will require the use of a second buffer page
				+ 1; // ... and there will still be body parts to write, so we'll give up on computing content-length

		try ( ElasticsearchClientImplementor client = createClient() ) {
			doPost( client, "/myIndex/myType", produceBody( bodyPartCount ) );
			wireMockRule.verify(
					postRequestedFor( urlPathLike( "/myIndex/myType" ) )
							.withHeader( "Transfer-Encoding", equalTo( "chunked" ) )
							.withoutHeader( "Content-length" )
			);
		}
	}

	/**
	 * When the request is post-processed (e.g. with the AWS integration),
	 * we have to store it entirely in memory, so chunked transfer does not make sense.
	 */
	@Test
	@Category(RequiresRequestPostProcessing.class)
	public void payloadJustAboveBufferSize_requestPostProcessing() throws Exception {
		wireMockRule.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		int bodyPartCount = BUFFER_LIMIT / BODY_PART_BYTE_SIZE + 1;

		try ( ElasticsearchClientImplementor client = createClient() ) {
			doPost( client, "/myIndex/myType", produceBody( bodyPartCount ) );
			wireMockRule.verify(
					postRequestedFor( urlPathLike( "/myIndex/myType" ) )
							.withoutHeader( "Transfer-Encoding" )
							.withHeader( "Content-length", equalTo( String.valueOf( bodyPartCount * BODY_PART_BYTE_SIZE ) ) )
					);
		}
	}

	private ElasticsearchClientImplementor createClient() {
		ConfigurationPropertySource defaultBackendProperties =
				TckConfiguration.get().getBackendProperties( testConfigurationProvider, null );

		// Redirect requests to Wiremock
		Map<String, Object> configurationOverride = new HashMap<>();
		configurationOverride.put( ElasticsearchBackendSettings.HOSTS, httpUrlFor( wireMockRule ) );
		ConfigurationPropertySource backendProperties =
				defaultBackendProperties.withOverride( ConfigurationPropertySource.fromMap( configurationOverride ) );

		BeanResolver beanResolver = testConfigurationProvider.createBeanResolverForTest();
		try ( BeanHolder<ElasticsearchClientFactory> factoryHolder =
				beanResolver.resolve( ElasticsearchClientFactoryImpl.REFERENCE ) ) {
			return factoryHolder.get().create(
					backendProperties, DefaultGsonProvider.create( GsonBuilder::new, true )
			);
		}
	}

	private ElasticsearchResponse doPost(ElasticsearchClient client, String path, Collection<JsonObject> bodyParts) {
		return client.submit( buildRequest( ElasticsearchRequest.post(), path, bodyParts ) ).join();
	}

	private ElasticsearchRequest buildRequest(ElasticsearchRequest.Builder builder, String path, Collection<JsonObject> bodyParts) {
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
		return urlPathMatching( path );
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
