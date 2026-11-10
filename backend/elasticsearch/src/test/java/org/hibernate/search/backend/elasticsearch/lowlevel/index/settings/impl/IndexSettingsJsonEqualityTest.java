package org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.hibernate.search.util.json.internal.spi.JsonParsing;

import org.junit.jupiter.api.Test;

class IndexSettingsJsonEqualityTest {
	@Test
	void diffRetainsMathematicalNumberEqualityAcrossNodeWidthsAndNestedScales() {
		var parsing = new JsonParsing();
		var original =
				new IndexSettings( null, null, Map.of( "setting", parsing.parse( "{\"values\":[2.00,9007199254740993]}" ) ) );
		assertThat( original.diff( Map.of( "setting", parsing.parse( "{\"values\":[2,9007199254740993]}" ) ) )
				.getExtraAttributes() ).isEmpty();
		assertThat( original.diff( Map.of( "setting", parsing.parse( "{\"values\":[\"2\",9007199254740993]}" ) ) ) )
				.isSameAs( original );
		assertThat( original.diff( Map.of( "setting", parsing.parse( "{\"values\":[2,9007199254740992]}" ) ) ) )
				.isSameAs( original );
	}
}
