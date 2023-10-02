/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;

import org.junit.jupiter.api.Test;

class SpatialWithinPolygonPredicateSpecificsIT extends AbstractSpatialWithinPredicateIT {

	private static final GeoPolygon POLYGON_1 = GeoPolygon.of(
			GeoPoint.of( 45.785889, 4.819749 ),
			GeoPoint.of( 45.753050, 4.811030 ),
			GeoPoint.of( 45.746915, 4.844146 ),
			GeoPoint.of( 45.785889, 4.848877 ),
			GeoPoint.of( 45.785889, 4.819749 )
	);

	private static final GeoPolygon POLYGON_2 = GeoPolygon.of(
			GeoPoint.of( 45.762111, 4.841442 ),
			GeoPoint.of( 45.751826, 4.837118 ),
			GeoPoint.of( 45.742692, 4.857632 ),
			GeoPoint.of( 45.758982, 4.866473 ),
			GeoPoint.of( 45.762111, 4.841442 )
	);

	private static final GeoPolygon CHEZ_MARGOTTE_POLYGON = GeoPolygon.of(
			GeoPoint.of( 45.7530375, 4.8510298 ),
			GeoPoint.of( 45.7530373, 4.8510298 ),
			GeoPoint.of( 45.7530373, 4.8510300 ),
			GeoPoint.of( 45.7530375, 4.8510300 ),
			GeoPoint.of( 45.7530375, 4.8510298 )
	);

	@Test
	void matchMultipleDocuments() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.spatial().within().field( "geoPoint" ).polygon( POLYGON_2 ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), IMOUTO_ID, CHEZ_MARGOTTE_ID );

		assertThatQuery( mainIndex.query()
				.where( f -> f.spatial().within().field( "geoPoint" ).polygon( POLYGON_1 ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), OURSON_QUI_BOIT_ID, IMOUTO_ID );
	}
}
