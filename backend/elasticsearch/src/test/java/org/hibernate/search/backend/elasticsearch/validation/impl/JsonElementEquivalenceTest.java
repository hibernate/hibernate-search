package org.hibernate.search.backend.elasticsearch.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.util.json.internal.spi.JsonParsing;

import org.junit.jupiter.api.Test;

class JsonElementEquivalenceTest {
	private final JsonParsing parsing = new JsonParsing();
	private final JsonElementEquivalence equivalence = new JsonElementEquivalence();

	@Test
	void settingsCompareNumericValuesWithStringsWithoutLosingPrecision() {
		assertThat( equivalence.isEquivalent( parsing.parse( "1.2300e4" ), parsing.parse( "\"12300\"" ) ) ).isTrue();
		assertThat( equivalence.isEquivalent( parsing.parse( "9007199254740993" ),
				parsing.parse( "\"9007199254740993\"" ) ) ).isTrue();
		assertThat( equivalence.isEquivalent( parsing.parse( "9007199254740993" ),
				parsing.parse( "9007199254740992" ) ) ).isFalse();
		assertThat( equivalence.isEquivalent( parsing.parse( "true" ), parsing.parse( "\"true\"" ) ) ).isTrue();
		assertThat( equivalence.isEquivalent( parsing.parse( "\"01\"" ), parsing.parse( "\"1\"" ) ) ).isFalse();
		assertThat( equivalence.isEquivalent( parsing.parse( "1" ), parsing.parse( "\"other\"" ) ) ).isFalse();
	}

	@Test
	void nestedObjectsAndArrayOrderRetainTheirComparisonPolicy() {
		assertThat( equivalence.isEquivalent( parsing.parse( "{\"value\":[1,true]}" ),
				parsing.parse( "{\"value\":[\"1\",\"true\"]}" ) ) ).isTrue();
		assertThat( equivalence.isEquivalent( parsing.parse( "[1,true]" ), parsing.parse( "[true,1]" ) ) ).isFalse();
		assertThat( equivalence.isEquivalent( parsing.parse( "{\"value\":null}" ), parsing.parse( "{}" ) ) ).isFalse();
		var unordered = new JsonElementUnorderedArrayEquivalence( equivalence );
		assertThat( unordered.isEquivalent( parsing.parse( "[1,\"a\"]" ),
				parsing.parse( "[\"a\",\"1\"]" ) ) ).isTrue();
	}
}
