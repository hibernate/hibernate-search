/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-2859")
class MultivaluedSpatialIT {

	// latitude increases from south to north
	// longitude increases from west to east
	private static final GeoPoint NORTH_WEST = GeoPoint.of( 7.0, 3.0 );
	private static final GeoPoint NORTH_EAST = GeoPoint.of( 7.0, 7.0 );
	private static final GeoPoint SOUTH_WEST = GeoPoint.of( 3.0, 3.0 );
	private static final GeoPoint SOUTH_EAST = GeoPoint.of( 3.0, 7.0 );

	private static final GeoBoundingBox AROUND_NORTH_WEST_BOX =
			GeoBoundingBox.of( GeoPoint.of( 8.0, 2.0 ), GeoPoint.of( 6.0, 4.0 ) );

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndex( index ).setup();
		initData();
	}

	@Test
	void boundingBox() {
		StubMappingScope scope = index.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.spatial().within().field( "geoPoint" ).boundingBox( AROUND_NORTH_WEST_BOX ) )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsAnyOrder( index.typeName(), "1" );
	}

	private void initData() {
		index.bulkIndexer()
				.add( "1", f -> {
					f.addValue( index.binding().geoPoint, NORTH_WEST );
					f.addValue( index.binding().geoPoint, SOUTH_EAST );
				} )
				.add( "2", f -> {
					f.addValue( index.binding().geoPoint, NORTH_EAST );
					f.addValue( index.binding().geoPoint, SOUTH_WEST );
				} )
				.join();
	}

	protected static class IndexBinding {
		final IndexFieldReference<GeoPoint> geoPoint;

		IndexBinding(IndexSchemaElement root) {
			geoPoint = root.field( "geoPoint", f -> f.asGeoPoint() )
					.multiValued().toReference();
		}
	}
}
