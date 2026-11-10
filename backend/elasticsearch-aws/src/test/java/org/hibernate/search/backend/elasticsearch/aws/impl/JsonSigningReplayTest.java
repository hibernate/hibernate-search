package org.hibernate.search.backend.elasticsearch.aws.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.client.common.json.impl.ElasticsearchJsonBody;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorContext;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.spi.JsonBody;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

class JsonSigningReplayTest {
	@Test
	void signingAndRetryReadTheSameBoundedNativeBody() throws Exception {
		var tree = new JsonParsing().parse( "{\"value\":\"é😀\",\"number\":9007199254740993,\"null\":null}" );
		JsonBody body = ElasticsearchJsonBody.ndjson( List.of( tree ) );
		byte[] expected;
		try ( var stream = body.openStream() ) {
			expected = stream.readAllBytes();
		}
		Context context = new Context( body );
		try ( var provider = HttpEntityContentStreamProvider.create( context ) ) {
			InputStream first = provider.newStream();
			assertThat( first.readAllBytes() ).isEqualTo( expected );
			InputStream second = provider.newStream();
			assertThat( second ).isNotSameAs( first );
			assertThat( second.readAllBytes() ).isEqualTo( expected );
		}
		var signer = new ElasticsearchAwsSigningRequestInterceptor( Region.US_EAST_1, "es",
				StaticCredentialsProvider.create( AwsBasicCredentials.create( "test-access-key", "test-secret-key" ) ) );
		signer.intercept( context );
		assertThat( context.headers ).anySatisfy( (key, values) -> {
			assertThat( key ).isEqualToIgnoringCase( "Authorization" );
			assertThat( values ).anySatisfy( value -> assertThat( value ).contains( "Credential=test-access-key/" ) );
		} );
		try ( InputStream retry = context.content() ) {
			assertThat( retry.readAllBytes() ).isEqualTo( expected );
		}
		assertThat( new String( expected, StandardCharsets.UTF_8 ) ).endsWith( "\n" );
	}

	private static final class Context implements ElasticsearchRequestInterceptorContext {
		private final JsonBody body;
		private Map<String, List<String>> headers;

		private Context(JsonBody body) {
			this.body = body;
		}

		public boolean hasContent() {
			return true;
		}

		public InputStream content() {
			return body.openStream();
		}

		public String scheme() {
			return "https";
		}

		public String host() {
			return "search.example.org";
		}

		public Integer port() {
			return 443;
		}

		public String method() {
			return "POST";
		}

		public String path() {
			return "/_bulk";
		}

		public Map<String, String> queryParameters() {
			return Map.of( "refresh", "true" );
		}

		public void overrideHeaders(Map<String, List<String>> headers) {
			this.headers = headers;
		}
	}
}
