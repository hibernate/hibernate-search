/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.spatial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class GeoPolygonTest {

	@Test
	void validPolygon() {
		GeoPolygon polygon = GeoPolygon.of( GeoPoint.of( 26, 23 ), GeoPoint.of( 26, 26 ), GeoPoint.of( 24, 26 ),
				GeoPoint.of( 24, 23 ), GeoPoint.of( 26, 23 ) );
		assertThat( polygon ).isNotNull();

		polygon = GeoPolygon.of( Arrays.asList( GeoPoint.of( 26, 23 ), GeoPoint.of( 26, 26 ), GeoPoint.of( 24, 26 ),
				GeoPoint.of( 24, 23 ), GeoPoint.of( 26, 23 ) ) );
		assertThat( polygon ).isNotNull();
	}

	@Test
	void invalidPolygon() {
		assertThatThrownBy(
				() -> GeoPolygon.of(
						GeoPoint.of( 26, 23 ),
						GeoPoint.of( 26, 26 ),
						GeoPoint.of( 24, 26 ),
						GeoPoint.of( 24, 23 )
				)
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "HSEARCH000516" );

		assertThatThrownBy(
				() -> GeoPolygon.of( Arrays.asList(
						GeoPoint.of( 26, 23 ),
						GeoPoint.of( 26, 26 ),
						GeoPoint.of( 24, 26 ),
						GeoPoint.of( 24, 23 )
				) )
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "HSEARCH000516" );
	}
}
