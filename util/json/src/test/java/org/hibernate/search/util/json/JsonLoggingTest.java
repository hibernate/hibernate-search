package org.hibernate.search.util.json;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.spi.JsonEmptyInputException;
import org.hibernate.search.util.json.spi.JsonParseException;

import org.junit.jupiter.api.Test;

class JsonLoggingTest {
	@Test
	void malformedInputKeepsParserCauseAndAddsSearchMessageId() {
		assertThatThrownBy( () -> new JsonParsing().parse( "{\"value\":}" ) )
				.isInstanceOf( JsonParseException.class )
				.hasMessageStartingWith( "HSEARCH910036: Invalid JSON input:" )
				.hasCauseInstanceOf( RuntimeException.class );
	}

	@Test
	void emptyInputRetainsItsDistinctExceptionType() {
		assertThatThrownBy( () -> new JsonParsing().parse( " " ) )
				.isInstanceOf( JsonEmptyInputException.class )
				.hasMessage( "HSEARCH910027: Expected one JSON root; input was empty" )
				.hasNoCause();
	}
}
