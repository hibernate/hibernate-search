package org.hibernate.search.backend.elasticsearch.search.query.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResultTotal;

import org.junit.jupiter.api.Test;

class ElasticsearchRawResultCharacterizationTest {
	@Test
	@SuppressWarnings("deprecation")
	void trustedResponseBorrowsOriginalWhileDeprecatedGsonResponseIsIndependent() {
		var response = JacksonJsonHandlers.object().fromJson( "{\"hits\":{\"total\":{\"value\":1,\"relation\":\"eq\"},"
				+ "\"hits\":[{\"_id\":\"1\",\"_source\":{\"number\":9007199254740993}}]},\"custom\":null}" );
		ElasticsearchSearchResultImpl<String> result = new ElasticsearchSearchResultImpl<>( response,
				SimpleSearchResultTotal.exact( 1 ), List.of( "1" ), Map.of(), 2, false, null );
		assertThat( result.responseBody( JacksonJsonHandlers.object() ) ).isSameAs( response );
		var legacy = result.responseBody();
		legacy.addProperty( "changed", true );
		assertThat( response.has( "changed" ) ).isFalse();
		assertThat( legacy.get( "custom" ).isJsonNull() ).isTrue();
		assertThat( legacy.getAsJsonObject( "hits" ).getAsJsonArray( "hits" ).get( 0 ).getAsJsonObject()
				.getAsJsonObject( "_source" ).get( "number" ).getAsLong() ).isEqualTo( 9007199254740993L );
	}
}
