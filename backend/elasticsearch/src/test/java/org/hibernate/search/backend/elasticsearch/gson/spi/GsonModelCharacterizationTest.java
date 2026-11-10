package org.hibernate.search.backend.elasticsearch.gson.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

class GsonModelCharacterizationTest {

	@Test
	void insertionOrderDuplicatesNullAndCopyOwnership() {
		JsonObject root = JsonParser.parseString( "{\"z\":1,\"a\":null,\"z\":2,\"nested\":{\"array\":[1]}}" ).getAsJsonObject();
		assertThat( root.keySet() ).containsExactly( "z", "a", "nested" );
		assertThat( root.get( "missing" ) ).isNull();
		assertThat( root.get( "a" ) ).isSameAs( JsonNull.INSTANCE );
		assertThat( root.get( "z" ).getAsInt() ).isEqualTo( 2 );
		JsonObject copy = root.deepCopy();
		copy.getAsJsonObject( "nested" ).getAsJsonArray( "array" ).add( 2 );
		assertThat( root.getAsJsonObject( "nested" ).getAsJsonArray( "array" ) ).hasSize( 1 );
		assertThat( copy.get( "z" ) ).isSameAs( root.get( "z" ) );
	}

	@Test
	void numberLexemesNarrowingEqualityAndHashing() {
		JsonPrimitive exponent = JsonParser.parseString( "1.2300e+04" ).getAsJsonPrimitive();
		assertThat( exponent.toString() ).isEqualTo( "1.2300e+04" );
		assertThat( exponent.getAsBigDecimal() ).isEqualByComparingTo( new BigDecimal( "12300" ) );
		assertThat( JsonParser.parseString( "9007199254740993" ).getAsLong() ).isEqualTo( 9007199254740993L );
		assertThat( JsonParser.parseString( "1.9" ).getAsLong() ).isEqualTo( 1L );
		assertThat( JsonParser.parseString( "4294967297" ).getAsInt() ).isEqualTo( 1 );
		JsonPrimitive integer = new JsonPrimitive( 1L );
		JsonPrimitive decimal = new JsonPrimitive( 1.0 );
		assertThat( integer ).isEqualTo( decimal );
		// Gson 2.15 normalizes hashes of equivalent numeric representations.
		assertThat( integer.hashCode() ).isEqualTo( decimal.hashCode() );
		assertThat( JsonParser.parseString( "9007199254740993" ) )
				.isEqualTo( JsonParser.parseString( "9007199254740992" ) );
		assertThat( JsonParser.parseString( "{\"a\":1,\"b\":2}" ) )
				.isEqualTo( JsonParser.parseString( "{\"b\":2,\"a\":1}" ) );
	}

	@Test
	void requestEscapingAndTreeStringEscaping() {
		JsonObject object = new JsonObject();
		object.addProperty( "text", "<>&='\n\"\\é漢😀" );
		String requestJson = new GsonBuilder().setPrettyPrinting().create().toJson( object );
		assertThat( requestJson ).contains( "\\u003c\\u003e\\u0026\\u003d\\u0027", "\\n", "\\\"", "\\\\", "é漢😀" );
		assertThat( object.toString() ).contains( "<>&='" );
		assertThat( JsonParser.parseString( requestJson ) ).isEqualTo( object );
	}
}
