package org.hibernate.search.documentation.backend.elasticsearch.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.util.json.JsonHandler;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class JsonMigrationExamplesTest {


	@Test
	void treeNavigation() {
		// tag::tree-navigation[]
		ObjectNode root = JacksonJsonHandlers.object().fromJson( "{\"parent\":[{\"value\":[42]}]}" );
		ObjectNode parent = (ObjectNode) root.path( "parent" ).path( 0 );
		long value = parent.path( "value" ).path( 0 ).longValue();
		parent.put( "value", 7L );
		boolean absent = root.path( "missing" ).isMissingNode();
		// end::tree-navigation[]
		assertThat( value ).isEqualTo( 42L );
		assertThat( absent ).isTrue();
		assertThat( root.toString() ).isEqualTo( "{\"parent\":[{\"value\":7}]}" );
	}

	@Test
	void exactNumbersAndGeneralValues() {
		// tag::numeric-values[]
		JsonHandler<JsonNode> values = JacksonJsonHandlers.value();
		JsonNode tree = values.fromJson( "[9007199254740993,0.123456789012345678901234567890,-0.0,null]" );
		BigInteger integer = tree.get( 0 ).bigIntegerValue();
		BigDecimal decimal = tree.get( 1 ).decimalValue();
		// end::numeric-values[]
		assertThat( integer ).isEqualTo( new BigInteger( "9007199254740993" ) );
		assertThat( decimal ).isEqualTo( new BigDecimal( "0.123456789012345678901234567890" ) );
		assertThat( Double.doubleToRawLongBits( tree.get( 2 ).doubleValue() ) ).isEqualTo( Long.MIN_VALUE );
		assertThat( tree.get( 3 ).isNull() ).isTrue();
		assertThat( values.fromJson( "null" ).isNull() ).isTrue();
		assertThat( values.fromJson( "true" ).booleanValue() ).isTrue();
		assertThatThrownBy( () -> values.fromJson( "1e2147483648" ) ).isInstanceOf( RuntimeException.class );
	}

}
