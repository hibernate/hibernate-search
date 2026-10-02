package org.hibernate.search.backend.elasticsearch.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NodeProviderTest {

	@ParameterizedTest
	@ValueSource(strings = { "https", "HTTPS", "HtTpS" })
	void httpsUriEnablesSsl(String scheme) {
		NodeProvider provider = fromUris( List.of( scheme + "://localhost:9200" ) );

		assertThat( provider.isSslEnabled() ).isTrue();
		assertThat( provider.nextNode().createRequestURI( "/", null ).getScheme() ).isEqualTo( "https" );
	}

	@Test
	void httpsUrisWithDifferentSchemeCase() {
		NodeProvider provider = fromUris( List.of( "https://localhost:9200", "HTTPS://localhost:9201" ) );

		assertThat( provider.isSslEnabled() ).isTrue();
	}

	@Test
	void mixedHttpAndHttpsUrisAreRejected() {
		assertThatThrownBy( () -> fromUris( List.of( "HTTP://localhost:9200", "HTTPS://localhost:9201" ) ) )
				.isInstanceOf( SearchException.class );
	}

	@ParameterizedTest
	@ValueSource(strings = { "a/b", "a%b", "café", "a?#&=b" })
	void encodedDocumentId(String id) {
		ElasticsearchRequest request = ElasticsearchRequest.get()
				.pathComponent( URLEncodedString.fromString( "index" ) )
				.pathComponent( URLEncodedString.fromString( "_doc" ) )
				.pathComponent( URLEncodedString.fromString( id ) ).build();

		URI uri = fromUris( List.of( "http://localhost:9200" ) ).nextNode()
				.createRequestURI( request.path(), request.parameters() );

		assertThat( uri.getRawPath() ).isEqualTo( "/index/_doc/" + URLEncodedString.fromString( id ).encoded );
		assertThat( uri.getRawQuery() ).isNull();
		assertThat( uri.getRawFragment() ).isNull();
	}

	@Test
	void queryParametersEncodedOnce() {
		ElasticsearchRequest request = ElasticsearchRequest.get().wholeEncodedPath( "/_search" )
				.param( "routing", "a/b%?#&=+ café" )
				.multiValuedParam( "fields", List.of( "first", "second" ) )
				.param( "key&=", "value" ).build();

		URI uri = fromUris( List.of( "http://localhost:9200" ) ).nextNode()
				.createRequestURI( request.path(), request.parameters() );

		assertThat( uri.getRawQuery() )
				.isEqualTo( "routing=a%2Fb%25%3F%23%26%3D%2B+caf%C3%A9&fields=first%2Csecond&key%26%3D=value" );
		assertThat( uri.getRawFragment() ).isNull();
	}

	/**
	 * The path prefix is treated as already URL-encoded, consistently with how the REST clients
	 * handle theirs: escape sequences are passed through as-is and never encoded a second time.
	 */
	@Test
	void encodedPathWithPrefixAndIpv6() {
		NodeProvider provider = NodeProvider.fromOptionalStrings( Optional.empty(), Optional.empty(),
				Optional.of( List.of( "http://[::1]:9200" ) ), "proxy%20caf%C3%A9/" );

		URI uri = provider.nextNode().createRequestURI( "/first%2Findex,second/_search", Map.of( "routing", "a/b" ) );

		assertThat( uri.toASCIIString() )
				.isEqualTo( "http://[::1]:9200/proxy%20caf%C3%A9/first%2Findex,second/_search?routing=a%2Fb" );
	}

	@Test
	void unencodedPathPrefixIsRejected() {
		assertThatThrownBy( () -> NodeProvider.fromOptionalStrings( Optional.empty(), Optional.empty(),
				Optional.of( List.of( "http://localhost:9200" ) ), "proxy café/" ) )
				.isInstanceOf( SearchException.class );
	}

	private static NodeProvider fromUris(List<String> uris) {
		return NodeProvider.fromOptionalStrings( Optional.empty(), Optional.empty(), Optional.of( uris ), "" );
	}
}
