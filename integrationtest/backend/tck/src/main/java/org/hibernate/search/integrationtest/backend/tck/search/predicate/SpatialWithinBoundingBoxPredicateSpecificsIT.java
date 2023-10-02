/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.junit.jupiter.api.Test;

class SpatialWithinBoundingBoxPredicateSpecificsIT extends AbstractSpatialWithinPredicateIT {

	private static final GeoBoundingBox BOUNDING_BOX_1 = GeoBoundingBox.of(
			GeoPoint.of( 45.785889, 4.819749 ),
			GeoPoint.of( 45.746915, 4.844146 )
	);

	private static final GeoBoundingBox BOUNDING_BOX_2 = GeoBoundingBox.of(
			GeoPoint.of( 45.762111, 4.83 ),
			GeoPoint.of( 45.742692, 4.857632 )
	);

	private static final String ADDITIONAL_POINT_1_ID = "additional_1";
	private static final GeoPoint ADDITIONAL_POINT_1_GEO_POINT = GeoPoint.of( 24.5, 25.5 );

	private static final String ADDITIONAL_POINT_2_ID = "additional_2";
	private static final GeoPoint ADDITIONAL_POINT_2_GEO_POINT = GeoPoint.of( 24.5, 23.5 );

	@Test
	void matchMultipleDocuments() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.spatial().within().field( "geoPoint" ).boundingBox( BOUNDING_BOX_2 ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), IMOUTO_ID, CHEZ_MARGOTTE_ID );

		assertThatQuery( mainIndex.query()
				.where( f -> f.spatial().within().field( "geoPoint" ).boundingBox( BOUNDING_BOX_1 ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), OURSON_QUI_BOIT_ID, IMOUTO_ID );

		assertThatQuery( mainIndex.query()
				.where( f -> f.spatial().within().field( "geoPoint" )
						.boundingBox( BOUNDING_BOX_2.topLeft().latitude(), BOUNDING_BOX_2.topLeft().longitude(),
								BOUNDING_BOX_2.bottomRight().latitude(), BOUNDING_BOX_2.bottomRight().longitude() ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), IMOUTO_ID, CHEZ_MARGOTTE_ID );

		assertThatQuery( mainIndex.query()
				.where( f -> f.spatial().within().field( "geoPoint" )
						.boundingBox( BOUNDING_BOX_1.topLeft().latitude(), BOUNDING_BOX_1.topLeft().longitude(),
								BOUNDING_BOX_1.bottomRight().latitude(), BOUNDING_BOX_1.bottomRight().longitude() ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), OURSON_QUI_BOIT_ID, IMOUTO_ID );
	}

	@Test
	void consistency() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.spatial().within().field( "geoPoint" )
						.boundingBox( GeoBoundingBox.of( GeoPoint.of( 25, 23 ), GeoPoint.of( 24, 26 ) ) ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), ADDITIONAL_POINT_1_ID, ADDITIONAL_POINT_2_ID );
	}

	@Override
	protected void initData() {
		super.initData();

		mainIndex.bulkIndexer()
				.add( ADDITIONAL_POINT_1_ID, document -> {
					document.addValue( mainIndex.binding().geoPoint, ADDITIONAL_POINT_1_GEO_POINT );
				} )
				.add( ADDITIONAL_POINT_2_ID, document -> {
					document.addValue( mainIndex.binding().geoPoint, ADDITIONAL_POINT_2_GEO_POINT );
				} )
				.join();
	}
}
