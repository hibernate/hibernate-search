/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Test;

public class ParseUtilsTest {

	@Test
	public void parseGeoPoint() {
		GeoPoint geoPoint = ParseUtils.parseGeoPoint( "12.123, -24.234" );
		assertThat( geoPoint ).isEqualTo( GeoPoint.of( 12.123, -24.234 ) );

		geoPoint = ParseUtils.parseGeoPoint( "12.123,-24.234" );
		assertThat( geoPoint ).isEqualTo( GeoPoint.of( 12.123, -24.234 ) );

		geoPoint = ParseUtils.parseGeoPoint( "12.123,   -24.234" );
		assertThat( geoPoint ).isEqualTo( GeoPoint.of( 12.123, -24.234 ) );

		SubTest.expectException( () -> ParseUtils.parseGeoPoint( "12.123#-24.234" ) )
				.assertThrown()
				.hasMessage( "HSEARCH000564: Unable to parse the provided geo-point value: '12.123#-24.234'. The expected format is latitude, longitude." );

		SubTest.expectException( () -> ParseUtils.parseGeoPoint( "12.123" ) )
				.assertThrown()
				.hasMessage( "HSEARCH000564: Unable to parse the provided geo-point value: '12.123'. The expected format is latitude, longitude." );
	}
}
