/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Year;

import org.hibernate.search.engine.spatial.GeoPoint;

import org.junit.jupiter.api.Test;

class ParseUtilsTest {

	@Test
	void parseYear() {
		assertThat( ParseUtils.parseYear( "2001" ) ).isEqualTo( Year.of( 2001 ) );
		assertThat( ParseUtils.parseYear( "1999" ) ).isEqualTo( Year.of( 1999 ) );
		assertThat( ParseUtils.parseYear( "1769" ) ).isEqualTo( Year.of( 1769 ) );
		assertThat( ParseUtils.parseYear( "-0001" ) ).isEqualTo( Year.of( -1 ) );
		assertThat( ParseUtils.parseYear( "-2001" ) ).isEqualTo( Year.of( -2001 ) );
		assertThat( ParseUtils.parseYear( "+454654554" ) ).isEqualTo( Year.of( 454654554 ) );
		assertThat( ParseUtils.parseYear( "-454654554" ) ).isEqualTo( Year.of( -454654554 ) );

		// Lenient parsing
		assertThat( ParseUtils.parseYear( "+2001" ) ).isEqualTo( Year.of( 2001 ) );
		assertThat( ParseUtils.parseYear( "454654554" ) ).isEqualTo( Year.of( 454654554 ) );
		assertThat( ParseUtils.parseYear( "-1" ) ).isEqualTo( Year.of( -1 ) );
	}

	@Test
	void parseGeoPoint() {
		GeoPoint geoPoint = ParseUtils.parseGeoPoint( "12.123, -24.234" );
		assertThat( geoPoint ).isEqualTo( GeoPoint.of( 12.123, -24.234 ) );

		geoPoint = ParseUtils.parseGeoPoint( "12.123,-24.234" );
		assertThat( geoPoint ).isEqualTo( GeoPoint.of( 12.123, -24.234 ) );

		geoPoint = ParseUtils.parseGeoPoint( "12.123,   -24.234" );
		assertThat( geoPoint ).isEqualTo( GeoPoint.of( 12.123, -24.234 ) );

		assertThatThrownBy( () -> ParseUtils.parseGeoPoint( "12.123#-24.234" ) )
				.hasMessageContainingAll( "Invalid geo-point value: '12.123#-24.234'.",
						"The expected format is '<latitude as double>, <longitude as double>'." );

		assertThatThrownBy( () -> ParseUtils.parseGeoPoint( "12.123" ) )
				.hasMessageContainingAll( "Invalid geo-point value: '12.123'.",
						"The expected format is '<latitude as double>, <longitude as double>'." );
	}

	@Test
	void parseByteVector() {
		assertThat( ParseUtils.parseBytePrimitiveArray( "[ 1, 2, 3, 4, 5 ]" ) )
				.containsExactly( 1, 2, 3, 4, 5 );
		assertThat( ParseUtils.parseBytePrimitiveArray( "[1,2,3,4,5,6]" ) )
				.containsExactly( 1, 2, 3, 4, 5, 6 );
		assertThat( ParseUtils.parseBytePrimitiveArray( "[ 1 2 3 4 5 6 7 ]" ) )
				.containsExactly( 1, 2, 3, 4, 5, 6, 7 );

		assertThat( ParseUtils.parseBytePrimitiveArray( "  \t [ 1,      2,\t\t 3, 4, 5 ]   " ) )
				.containsExactly( 1, 2, 3, 4, 5 );

		assertThat( ParseUtils.parseBytePrimitiveArray( "1, 2, 3, 4, 5" ) )
				.containsExactly( 1, 2, 3, 4, 5 );
		assertThat( ParseUtils.parseBytePrimitiveArray( "1; 2; 3; 4; 5" ) )
				.containsExactly( 1, 2, 3, 4, 5 );
		assertThat( ParseUtils.parseBytePrimitiveArray( "1 2 3 4 5" ) )
				.containsExactly( 1, 2, 3, 4, 5 );

		assertThatThrownBy( () -> ParseUtils.parseBytePrimitiveArray( "[ 1a 2a 3a 4a 5 ]" ) )
				.hasMessageContainingAll( "Invalid string for type 'java.lang.Byte': '1a'. For input string: \"1a\"" );
		assertThatThrownBy( () -> ParseUtils.parseBytePrimitiveArray( "[1,2[32],2]" ) )
				.hasMessageContainingAll( "Invalid string for type 'java.lang.Byte': '2[32]'. For input string: \"2[32]\"" );
		assertThatThrownBy( () -> ParseUtils.parseBytePrimitiveArray( "[ 1, 2, 3, 4, 5" ) )
				.hasMessageContainingAll( "Invalid string for type 'java.lang.Byte': '['. For input string: \"[\"" );
		assertThatThrownBy( () -> ParseUtils.parseBytePrimitiveArray( "1, 2, 3, 4, 5]" ) )
				.hasMessageContainingAll( "Invalid string for type 'java.lang.Byte': '5]'. For input string: \"5]\"" );
	}

	@Test
	void parseFloatVector() {
		assertThat( ParseUtils.parseFloatPrimitiveArray( "[ 1.0, 2.0, 3.0, 4.0, 5.0 ]" ) )
				.containsExactly( 1.0f, 2.0f, 3.0f, 4.0f, 5.0f );
		assertThat( ParseUtils.parseFloatPrimitiveArray( "[1.0,2.0,3.0,4.0,5.0,6.0]" ) )
				.containsExactly( 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f );
		assertThat( ParseUtils.parseFloatPrimitiveArray( "[ 1.0 2.0 3.0 4.0 5.0 6.0 7.0 ]" ) )
				.containsExactly( 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f );

		assertThat( ParseUtils.parseFloatPrimitiveArray( "  \t [ 1.0,      2.0,\t\t 3.0, 4.0, 5.0 ]   " ) )
				.containsExactly( 1.0f, 2.0f, 3.0f, 4.0f, 5.0f );

		assertThat( ParseUtils.parseFloatPrimitiveArray( "1.0, 2.0, 3.0, 4.0, 5.0" ) )
				.containsExactly( 1.0f, 2.0f, 3.0f, 4.0f, 5.0f );
		assertThat( ParseUtils.parseFloatPrimitiveArray( "1.0; 2.0; 3.0; 4.0; 5.0" ) )
				.containsExactly( 1.0f, 2.0f, 3.0f, 4.0f, 5.0f );
		assertThat( ParseUtils.parseFloatPrimitiveArray( "1.0 2.0 3.0 4.0 5.0" ) )
				.containsExactly( 1.0f, 2.0f, 3.0f, 4.0f, 5.0f );

		assertThatThrownBy( () -> ParseUtils.parseFloatPrimitiveArray( "[ 1.0a 2.0a 3.0a 4.0a 5.0 ]" ) )
				.hasMessageContainingAll( "Invalid string for type 'java.lang.Float': '1.0a'. For input string: \"1.0a\"" );
		assertThatThrownBy( () -> ParseUtils.parseFloatPrimitiveArray( "[1.0,2.0[32.0],2.0]" ) )
				.hasMessageContainingAll(
						"Invalid string for type 'java.lang.Float': '2.0[32.0]'. For input string: \"2.0[32.0]\"" );
		assertThatThrownBy( () -> ParseUtils.parseFloatPrimitiveArray( "[ 1.0, 2.0, 3.0, 4.0, 5.0" ) )
				.hasMessageContainingAll( "Invalid string for type 'java.lang.Float': '['. For input string: \"[\"" );
		assertThatThrownBy( () -> ParseUtils.parseFloatPrimitiveArray( "1.0, 2.0, 3.0, 4.0, 5.0]" ) )
				.hasMessageContainingAll( "Invalid string for type 'java.lang.Float': '5.0]'. For input string: \"5.0]\"" );
	}
}
