package org.hibernate.search.backend.elasticsearch.logging.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;

import org.junit.jupiter.api.Test;

class ElasticsearchJsonObjectFormatterTest {
	@Test
	void formatsNativeJsonAndNeutralValuesIncludingNull() {
		var node = JacksonJsonHandlers.object().fromJson( "{\"value\":42}" );
		assertThat( new ElasticsearchJsonObjectFormatter( node ).toString() ).contains( "\"value\"", "42" );
		assertThat( new ElasticsearchJsonObjectFormatter( "neutral value" ).toString() ).isEqualTo( "neutral value" );
		assertThat( new ElasticsearchJsonObjectFormatter( 42 ).toString() ).isEqualTo( "42" );
		assertThat( new ElasticsearchJsonObjectFormatter( null ).toString() ).isEqualTo( "null" );
	}
}
