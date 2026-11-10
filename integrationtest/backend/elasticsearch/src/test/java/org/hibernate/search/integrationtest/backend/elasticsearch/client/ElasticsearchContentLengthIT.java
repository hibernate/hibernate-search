package org.hibernate.search.integrationtest.backend.elasticsearch.client;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient.getDiscoveredClientFactory;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonClient;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.common.execution.spi.DelegatingSimpleScheduledExecutor;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.impl.EmbeddedThreadProvider;
import org.hibernate.search.engine.environment.thread.impl.ThreadPoolProviderImpl;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.internal.spi.JsonParsing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import tools.jackson.databind.node.ObjectNode;

/**
 * Test that we manage to compute the content-length of Elasticsearch
 * requests appropriately after a bounded small-body probe,
 * and use the "chunked" transfer-encoding otherwise.
 */
@TestForIssue(jiraKey = "HSEARCH-2849")
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.client.ElasticsearchContentLengthIT")
class ElasticsearchContentLengthIT {

	private static final ObjectNode BODY_PART = ( (ObjectNode) new JsonParsing().parse( "{ \"foo\": \"bar\" }" ) );

	private static final int BODY_PART_BYTE_SIZE;
	static {
		JsonCodecService json =
				JsonProviderHelper.create( false ).getCodec();
		try ( ByteArrayOutputStream out = new ByteArrayOutputStream();
				Writer writer = new OutputStreamWriter( out, StandardCharsets.UTF_8 ) ) {
			json.write( BODY_PART, writer );
			// Account for EOL at the end of each body part
			writer.write( '\n' );
			writer.flush();
			BODY_PART_BYTE_SIZE = out.size();
		}
		catch (IOException e) {
			throw new IllegalStateException( "Error while initializing a constant", e );
		}
	}

	private static final int BUFFER_LIMIT = JsonBodies.DEFAULT_BUFFER_SIZE;

	@RegisterExtension
	private final WireMockExtension wireMockRule = WireMockExtension.newInstance()
			.options( wireMockConfig().dynamicPort().dynamicHttpsPort() )
			.build();

	@RegisterExtension
	public TestConfigurationProvider testConfigurationProvider = new TestConfigurationProvider();

	private final ThreadPoolProviderImpl threadPoolProvider = new ThreadPoolProviderImpl(
			BeanHolder.of( new EmbeddedThreadProvider( ElasticsearchContentLengthIT.class.getName() + ": " ) )
	);

	private ScheduledExecutorService timeoutExecutorService =
			threadPoolProvider.newScheduledExecutor( 1, "Timeout - " );

	@AfterEach
	void cleanup() {
		timeoutExecutorService.shutdownNow();
		threadPoolProvider.close();

		// Avoid side-effects from one test to another
		// Ideally WiremockRule should do that by itself, but it doesn't...
		wireMockRule.resetAll();
	}

	@Test
	void tinyPayload() throws Exception {
		wireMockRule.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		try ( ElasticsearchJsonClientImplementor client = createClient() ) {
			doPost( client, "/myIndex/myType", produceBody( 1 ) );
			wireMockRule.verify(
					postRequestedFor( urlPathLike( "/myIndex/myType" ) )
							.withoutHeader( "Transfer-Encoding" )
							.withHeader( "Content-length", equalTo( String.valueOf( BODY_PART_BYTE_SIZE ) ) )
			);
		}
	}

	@Test
	void payloadJustBelowBufferSize() throws Exception {
		wireMockRule.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		int bodyPartCount = BUFFER_LIMIT / BODY_PART_BYTE_SIZE - 1;

		try ( ElasticsearchJsonClientImplementor client = createClient() ) {
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
	void payloadJustAboveBufferSize_noRequestPostProcessing() throws Exception {
		assumeFalse(
				ElasticsearchTestHostConnectionConfiguration.get().isAws(),
				"This test only is only relevant if Elasticsearch request are *NOT* post-processed." +
						" Elasticsearch requests are post-processed by the AWS integration in particular."
		);
		wireMockRule.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );
		// The last body part will require the use of a second buffer page
		int // The last body part will require the use of a second buffer page
		bodyPartCount = // ... and there will still be body parts to write, so we'll give up on computing content-length
				BUFFER_LIMIT / BODY_PART_BYTE_SIZE
						+ 1 + 1;
		try ( ElasticsearchJsonClientImplementor client = createClient() ) {
			doPost( client, "/myIndex/myType", produceBody( bodyPartCount ) );
			wireMockRule.verify(
					postRequestedFor( urlPathLike( "/myIndex/myType" ) )
							.withHeader( "Transfer-Encoding", equalTo( "chunked" ) )
							.withoutHeader( "Content-length" )
			);
		}
	}

	/**
	 * Signing uses an independent bounded cursor stream. Large requests keep their
	 * unknown length after signing and are sent with chunked transfer encoding.
	 */
	@Test
	void payloadJustAboveBufferSize_requestPostProcessing() throws Exception {
		assumeTrue(
				ElasticsearchTestHostConnectionConfiguration.get().isAws(),
				"This test only is only relevant if Elasticsearch request are post-processed." +
						" Elasticsearch requests are post-processed by the AWS integration in particular."
		);
		wireMockRule.stubFor( post( urlPathLike( "/myIndex/myType" ) )
				.willReturn( elasticsearchResponse().withStatus( 200 ) ) );

		int bodyPartCount = BUFFER_LIMIT / BODY_PART_BYTE_SIZE + 1;

		try ( ElasticsearchJsonClientImplementor client = createClient() ) {
			doPost( client, "/myIndex/myType", produceBody( bodyPartCount ) );
			wireMockRule.verify(
					postRequestedFor( urlPathLike( "/myIndex/myType" ) )
							.withHeader( "Transfer-Encoding", equalTo( "chunked" ) ).withoutHeader( "Content-length" )
			);
		}
	}

	private ElasticsearchJsonClientImplementor createClient() {
		Map<String, ?> defaultBackendProperties =
				new ElasticsearchTckBackendHelper().createDefaultBackendSetupStrategy()
						.createBackendConfigurationProperties( testConfigurationProvider );

		Map<String, Object> clientProperties = new HashMap<>( defaultBackendProperties );

		// We won't target the provided ES instance, but Wiremock
		clientProperties.remove( ElasticsearchBackendSettings.HOSTS );
		clientProperties.remove( ElasticsearchBackendSettings.PROTOCOL );
		clientProperties.remove( ElasticsearchBackendSettings.URIS );

		// Target the Wiremock server using HTTP
		clientProperties.put( ElasticsearchBackendSettings.URIS, httpUriFor( wireMockRule ) );

		ConfigurationPropertySource clientPropertySource = AllAwareConfigurationPropertySource.fromMap( clientProperties );

		BeanResolver beanResolver = testConfigurationProvider.createBeanResolverForTest();
		return new org.hibernate.search.backend.elasticsearch.client.common.impl.JsonTransportClient(
				getDiscoveredClientFactory( beanResolver ).get()
						.create( beanResolver, clientPropertySource,
								threadPoolProvider.threadProvider(), "Client",
								new DelegatingSimpleScheduledExecutor( timeoutExecutorService, true ) ),
				JsonProviderHelper.create( true ) );
	}

	private ElasticsearchJsonResponse doPost(ElasticsearchJsonClient client, String path, Collection<ObjectNode> bodyParts) {
		return client.submit( buildRequest( ElasticsearchJsonRequest.post(), path, bodyParts ) ).join();
	}

	private ElasticsearchJsonRequest buildRequest(ElasticsearchJsonRequest.Builder builder, String path,
			Collection<ObjectNode> bodyParts) {
		for ( String pathComponent : path.split( "/" ) ) {
			if ( !pathComponent.isEmpty() ) {
				URLEncodedString fromString = URLEncodedString.fromString( pathComponent );
				builder = builder.pathComponent( fromString );
			}
		}
		for ( ObjectNode bodyPart : bodyParts ) {
			builder = builder.bodyJson( bodyPart );
		}
		return builder.build();
	}

	private static String httpUriFor(WireMockExtension extension) {
		return "http://localhost:" + extension.getPort();
	}

	private static UrlPathPattern urlPathLike(String path) {
		return urlPathMatching( path );
	}

	private static ResponseDefinitionBuilder elasticsearchResponse() {
		return ResponseDefinitionBuilder.okForEmptyJson();
	}

	private static Collection<ObjectNode> produceBody(int bodyPartCount) {
		Collection<ObjectNode> result = new ArrayList<>( bodyPartCount );
		for ( int i = 0; i < bodyPartCount; i++ ) {
			result.add( BODY_PART );
		}
		return result;
	}

}
