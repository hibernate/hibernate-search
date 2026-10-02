package org.hibernate.search.backend.elasticsearch.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

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

	private static NodeProvider fromUris(List<String> uris) {
		return NodeProvider.fromOptionalStrings( Optional.empty(), Optional.empty(), Optional.of( uris ), "" );
	}
}
