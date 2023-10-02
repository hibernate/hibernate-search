/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.junit.jupiter.api.Test;

class SpatialWithinCirclePredicateSpecificsIT extends AbstractSpatialWithinPredicateIT {

	private static final GeoPoint METRO_HOTEL_DE_VILLE = GeoPoint.of( 45.7673396, 4.833743 );
	private static final GeoPoint METRO_GARIBALDI = GeoPoint.of( 45.7515926, 4.8514779 );

	@Test
	void matchMultipleDocuments() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.spatial().within()
						.field( "geoPoint" )
						.circle( METRO_GARIBALDI, 1_500 ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), CHEZ_MARGOTTE_ID, IMOUTO_ID );

		assertThatQuery( mainIndex.query()
				.where( f -> f.spatial().within()
						.field( "geoPoint" )
						.circle( METRO_HOTEL_DE_VILLE, 500 ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), OURSON_QUI_BOIT_ID );

		assertThatQuery( mainIndex.query()
				.where( f -> f.spatial().within()
						.field( "geoPoint" )
						.circle( METRO_GARIBALDI.latitude(), METRO_GARIBALDI.longitude(), 1_500 ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), CHEZ_MARGOTTE_ID, IMOUTO_ID );

		assertThatQuery( mainIndex.query()
				.where( f -> f.spatial().within()
						.field( "geoPoint" )
						.circle( METRO_GARIBALDI.latitude(), METRO_GARIBALDI.longitude(), 1.5, DistanceUnit.KILOMETERS ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), CHEZ_MARGOTTE_ID, IMOUTO_ID );

		assertThatQuery( mainIndex.query()
				.where( f -> f.spatial().within()
						.field( "geoPoint" )
						.circle( METRO_GARIBALDI, 1.5, DistanceUnit.KILOMETERS ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), CHEZ_MARGOTTE_ID, IMOUTO_ID );
	}

}
