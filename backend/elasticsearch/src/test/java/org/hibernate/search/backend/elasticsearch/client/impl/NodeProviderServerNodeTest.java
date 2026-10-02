package org.hibernate.search.backend.elasticsearch.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.client.impl.NodeProvider.ServerNode;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class NodeProviderServerNodeTest {

	private static ServerNode node() {
		return new ServerNode( "http", "localhost", "", 9200 );
	}

	private static Map<String, String> params(String... keyValuePairs) {
		Map<String, String> params = new LinkedHashMap<>();
		for ( int i = 0; i < keyValuePairs.length; i += 2 ) {
			params.put( keyValuePairs[i], keyValuePairs[i + 1] );
		}
		return params;
	}

	@Nested
	class PathEncodingTest {

		/**
		 * The main regression: request paths reach {@link ServerNode#createRequestURI} already URL-encoded,
		 * so they must not be escaped a second time. Index names derived from nested classes contain a '$',
		 * which {@link URLEncodedString} encodes as '%24'; re-escaping turned that into '%2524'
		 * and made Elasticsearch create/address an index literally named '...%24...'.
		 */
		@Test
		void alreadyEncodedPathIsNotEscapedAgain() {
			String encoded = URLEncodedString.fromString( "entityastreesmokeit$indexedentity-write" ).encoded;
			assertThat( encoded ).isEqualTo( "entityastreesmokeit%24indexedentity-write" );

			URI uri = node().createRequestURI( "/_cluster/health/" + encoded, Map.of() );

			assertThat( uri.getRawPath() )
					.isEqualTo( "/_cluster/health/entityastreesmokeit%24indexedentity-write" );
			assertThat( uri.getPath() )
					.isEqualTo( "/_cluster/health/entityastreesmokeit$indexedentity-write" );
			assertThat( uri ).hasToString(
					"http://localhost:9200/_cluster/health/entityastreesmokeit%24indexedentity-write" );
		}

		/**
		 * An encoded '/' must stay encoded: decoding it would turn a single path component
		 * into two, changing which resource is addressed.
		 */
		@Test
		void encodedSlashStaysEncoded() {
			String encoded = URLEncodedString.fromString( "with/slash" ).encoded;
			assertThat( encoded ).isEqualTo( "with%2Fslash" );

			URI uri = node().createRequestURI( "/my-index/_doc/" + encoded, Map.of() );

			assertThat( uri.getRawPath() ).isEqualTo( "/my-index/_doc/with%2Fslash" );
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"with space",
				"with+plus",
				"with%percent",
				"with#hash",
				"with?question",
				"with&ampersand",
				"with=equals",
				"naïve",
				"日本語"
		})
		void anyCharacterSurvivesTheRoundTrip(String raw) {
			String encoded = URLEncodedString.fromString( raw ).encoded;

			URI uri = node().createRequestURI( "/" + encoded + "/_doc/1", Map.of() );

			// The raw path must carry exactly what URLEncodedString produced: no more, no less.
			assertThat( uri.getRawPath() ).isEqualTo( "/" + encoded + "/_doc/1" );
		}

		@Test
		void emptyPath() {
			assertThat( node().createRequestURI( "", Map.of() ) )
					.hasToString( "http://localhost:9200" );
		}

		@Test
		void pathMustStartWithSlash() {
			assertThatThrownBy( () -> node().createRequestURI( "_doc", Map.of() ) )
					.isInstanceOf( IllegalArgumentException.class )
					.hasMessageContaining( "Path must start with '/'" );
		}
	}

	@Nested
	class QueryStringTest {

		/**
		 * Unlike paths, query string parameters arrive raw, so they must be encoded exactly once.
		 */
		@Test
		void parametersAreEncodedExactlyOnce() {
			URI uri = node().createRequestURI( "/_search/scroll", params( "scroll_id", "aB+c/d=" ) );

			assertThat( uri.getRawQuery() ).isEqualTo( "scroll_id=aB%2Bc%2Fd%3D" );
			assertThat( uri.getQuery() ).isEqualTo( "scroll_id=aB+c/d=" );
		}

		@Test
		void multipleParametersKeepInsertionOrder() {
			URI uri = node().createRequestURI( "/_cluster/health/my-index",
					params( "wait_for_status", "yellow", "timeout", "10000ms" ) );

			assertThat( uri.getRawQuery() ).isEqualTo( "wait_for_status=yellow&timeout=10000ms" );
		}

		@Test
		void parameterKeysAreEncodedToo() {
			URI uri = node().createRequestURI( "/_doc", params( "a key", "a value" ) );

			// URLEncoder uses application/x-www-form-urlencoded, hence '+' rather than '%20'.
			assertThat( uri.getRawQuery() ).isEqualTo( "a+key=a+value" );
		}

		@Test
		void percentInParameterValueIsEncoded() {
			URI uri = node().createRequestURI( "/_doc", params( "q", "100%" ) );

			assertThat( uri.getRawQuery() ).isEqualTo( "q=100%25" );
			assertThat( uri.getQuery() ).isEqualTo( "q=100%" );
		}

		@Test
		void noQueryStringWhenNoParameters() {
			assertThat( node().createRequestURI( "/_doc", Map.of() ) )
					.hasToString( "http://localhost:9200/_doc" );
		}

		@Test
		void noQueryStringWhenParametersAreNull() {
			assertThat( node().createRequestURI( "/_doc", null ) )
					.hasToString( "http://localhost:9200/_doc" );
		}
	}

	@Nested
	class BasePathTest {

		@ParameterizedTest
		@CsvSource({
				"'',                 http://localhost:9200/_doc",
				"/,                  http://localhost:9200/_doc",
				"prefix,             http://localhost:9200/prefix/_doc",
				"/prefix,            http://localhost:9200/prefix/_doc",
				"/prefix/,           http://localhost:9200/prefix/_doc",
				"/deep/prefix,       http://localhost:9200/deep/prefix/_doc"
		})
		void normalization(String basePath, String expected) {
			ServerNode node = new ServerNode( "http", "localhost", basePath, 9200 );

			assertThat( node.createRequestURI( "/_doc", Map.of() ) ).hasToString( expected );
		}

		/**
		 * The base path is treated as already URL-encoded, consistently with the request paths it gets
		 * prepended to and with how the REST clients handle their path prefix
		 * (which they hand over to a URI parser rather than to an encoder).
		 */
		@Test
		void basePathIsTreatedAsAlreadyEncoded() {
			ServerNode node = new ServerNode( "http", "localhost", "/a%24b", 9200 );

			URI uri = node.createRequestURI( "/_doc", Map.of() );

			assertThat( uri.getRawPath() ).isEqualTo( "/a%24b/_doc" );
			assertThat( uri.getPath() ).isEqualTo( "/a$b/_doc" );
		}

		/**
		 * A malformed path prefix must be reported on startup rather than on the first request.
		 */
		@Test
		void malformedBasePathIsRejectedOnConstruction() {
			assertThatThrownBy( () -> new ServerNode( "http", "localhost", "/a b", 9200 ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "HSEARCH400694" )
					.hasMessageContaining( "http://localhost:9200/a b" );
		}
	}

	@Nested
	class AuthorityTest {

		@ParameterizedTest
		@CsvSource({
				"http,  localhost,   9200, http://localhost:9200/_doc",
				"https, localhost,   9200, https://localhost:9200/_doc",
				"http,  es.example,    -1, http://es.example/_doc",
				"http,  '[::1]',     9200, 'http://[::1]:9200/_doc'",
				"http,  '::1',       9200, 'http://[::1]:9200/_doc'",
				"http,  127.0.0.1,   9200, http://127.0.0.1:9200/_doc"
		})
		void schemeHostAndPort(String protocol, String host, int port, String expected) {
			ServerNode node = new ServerNode( protocol, host, "", port );

			assertThat( node.createRequestURI( "/_doc", Map.of() ) ).hasToString( expected );
		}

		@Test
		void ipv6HostIsUsableAsAnActualUri() {
			ServerNode node = new ServerNode( "https", "::1", "/prefix", 9200 );

			URI uri = node.createRequestURI( "/idx%24a", Map.of() );

			assertThat( uri.getHost() ).isEqualTo( "[::1]" );
			assertThat( uri.getPort() ).isEqualTo( 9200 );
			assertThat( uri.getRawPath() ).isEqualTo( "/prefix/idx%24a" );
		}
	}

	@Nested
	class EqualsHashCodeTest {

		@Test
		void equalWhenSameCoordinates() {
			assertThat( new ServerNode( "http", "localhost", "/prefix", 9200 ) )
					.isEqualTo( new ServerNode( "http", "localhost", "/prefix", 9200 ) )
					.hasSameHashCodeAs( new ServerNode( "http", "localhost", "/prefix", 9200 ) );
		}

		@Test
		void notEqualWhenDifferentPort() {
			assertThat( new ServerNode( "http", "localhost", "", 9200 ) )
					.isNotEqualTo( new ServerNode( "http", "localhost", "", 9201 ) );
		}

		@Test
		void equalWhenBasePathNormalizesToTheSameValue() {
			assertThat( new ServerNode( "http", "localhost", "prefix/", 9200 ) )
					.isEqualTo( new ServerNode( "http", "localhost", "/prefix", 9200 ) );
		}

		/**
		 * Equality is based on the resulting base URL, so two spellings of the same IPv6 host
		 * designate the same node.
		 */
		@Test
		void equalWhenIpv6HostIsSpelledDifferently() {
			assertThat( new ServerNode( "http", "::1", "", 9200 ) )
					.isEqualTo( new ServerNode( "http", "[::1]", "", 9200 ) );
		}

		/**
		 * An implicit default port is not the same as an explicit one, as far as URLs are concerned.
		 */
		@Test
		void notEqualWhenOnlyOnePortIsExplicit() {
			assertThat( new ServerNode( "http", "localhost", "", -1 ) )
					.isNotEqualTo( new ServerNode( "http", "localhost", "", 80 ) );
		}

		@Test
		void toStringIsTheBaseUrl() {
			assertThat( new ServerNode( "https", "localhost", "prefix/", 9200 ) )
					.hasToString( "https://localhost:9200/prefix" );
		}
	}

	@Nested
	class RoundRobinTest {

		@Test
		void nextNodeCyclesThroughNodes() {
			ServerNode first = new ServerNode( "http", "host1", "", 9200 );
			ServerNode second = new ServerNode( "http", "host2", "", 9200 );
			NodeProvider provider = new NodeProvider( List.of( first, second ), false );

			assertThat( provider.nextNode() ).isEqualTo( first );
			assertThat( provider.nextNode() ).isEqualTo( second );
			assertThat( provider.nextNode() ).isEqualTo( first );
		}
	}
}
