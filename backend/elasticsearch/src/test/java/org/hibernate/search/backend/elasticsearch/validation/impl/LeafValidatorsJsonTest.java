package org.hibernate.search.backend.elasticsearch.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.util.json.internal.spi.JsonParsing;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.LongNode;

class LeafValidatorsJsonTest {
	@Test
	void numericNullValuesIgnoreNodeWidthButRetainExactValueAndStringTypes() {
		var parsing = new JsonParsing();
		var collector = new ValidationErrorCollector();
		var validator = LeafValidators.jsonElement( DataTypes.LONG );
		validator.validate( collector, ValidationContextType.MAPPING_ATTRIBUTE, "null_value",
				LongNode.valueOf( 3 ), parsing.parse( "3" ) );
		assertThat( collector.hasError() ).isFalse();
		validator.validate( collector, ValidationContextType.MAPPING_ATTRIBUTE, "null_value",
				parsing.parse( "9007199254740993" ), parsing.parse( "9007199254740992" ) );
		assertThat( collector.hasError() ).isTrue();
		var strings = new ValidationErrorCollector();
		validator.validate( strings, ValidationContextType.MAPPING_ATTRIBUTE, "null_value",
				parsing.parse( "3" ), parsing.parse( "\"3\"" ) );
		assertThat( strings.hasError() ).isTrue();
	}
}
