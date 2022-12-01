/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
}
