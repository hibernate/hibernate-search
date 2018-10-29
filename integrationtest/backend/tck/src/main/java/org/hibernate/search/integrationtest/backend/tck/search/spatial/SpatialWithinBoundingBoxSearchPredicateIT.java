/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Test;

public class SpatialWithinBoundingBoxSearchPredicateIT extends AbstractSpatialWithinSearchPredicateIT {

	private static final GeoBoundingBox BOUNDING_BOX_1 = GeoBoundingBox.of(
			GeoPoint.of( 45.785889, 4.819749 ),
			GeoPoint.of( 45.746915, 4.844146 )
	);

	private static final GeoBoundingBox BOUNDING_BOX_2 = GeoBoundingBox.of(
			GeoPoint.of( 45.762111, 4.83 ),
			GeoPoint.of( 45.742692, 4.857632 )
	);


	private static final GeoBoundingBox CHEZ_MARGOTTE_BOUNDING_BOX = GeoBoundingBox.of(
			GeoPoint.of( 45.7530375, 4.8510298 ),
			GeoPoint.of( 45.7530373, 4.8510300 )
	);

	private static final GeoBoundingBox BOUNDING_BOX_1_1 = moveBoundingBox( BOUNDING_BOX_1, -1 );
	private static final GeoBoundingBox BOUNDING_BOX_1_2 = moveBoundingBox( BOUNDING_BOX_1, -2 );
	private static final GeoBoundingBox BOUNDING_BOX_2_1 = moveBoundingBox( BOUNDING_BOX_2, -1 );
	private static final GeoBoundingBox BOUNDING_BOX_2_2 = moveBoundingBox( BOUNDING_BOX_2, -2 );

	private static final String ADDITIONAL_POINT_1_ID = "additional_1";
	private static final GeoPoint ADDITIONAL_POINT_1_GEO_POINT = GeoPoint.of( 24.5, 25.5 );

	private static final String ADDITIONAL_POINT_2_ID = "additional_2";
	private static final GeoPoint ADDITIONAL_POINT_2_GEO_POINT = GeoPoint.of( 24.5, 23.5 );

	@Test
	public void within_boundingBox() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" ).boundingBox( BOUNDING_BOX_2 ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" ).boundingBox( BOUNDING_BOX_1 ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" )
						.boundingBox( BOUNDING_BOX_2.getTopLeft().getLatitude(), BOUNDING_BOX_2.getTopLeft().getLongitude(),
								BOUNDING_BOX_2.getBottomRight().getLatitude(), BOUNDING_BOX_2.getBottomRight().getLongitude() )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" )
						.boundingBox( BOUNDING_BOX_1.getTopLeft().getLatitude(), BOUNDING_BOX_1.getTopLeft().getLongitude(),
								BOUNDING_BOX_1.getBottomRight().getLatitude(), BOUNDING_BOX_1.getBottomRight().getLongitude() )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );
	}

	@Test
	public void boundingBox_consistency() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" )
						.boundingBox( GeoBoundingBox.of( GeoPoint.of( 25, 23 ), GeoPoint.of( 24, 26 ) ) )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, ADDITIONAL_POINT_1_ID, ADDITIONAL_POINT_2_ID );
	}

	@Test
	public void unsupported_field_types() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SubTest.expectException(
				"spatial().within().boundingBox() predicate on field with unsupported type",
				() -> searchTarget.predicate().spatial().within().onField( "string" ).boundingBox( BOUNDING_BOX_1 )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Spatial predicates are not supported by" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( "string" )
				) );
	}

	@Test
	public void boost() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.bool()
						.should( c -> c.spatial().within().onField( "geoPoint" ).boundingBox( CHEZ_MARGOTTE_BOUNDING_BOX ) )
						.should( c -> c.match().onField( "string" ).boostedTo( 42 ).matching( OURSON_QUI_BOIT_STRING ) )
				)
				.sort( c -> c.byScore() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.bool()
						.should( c -> c.spatial().within().onField( "geoPoint" ).boostedTo( 42 ).boundingBox( CHEZ_MARGOTTE_BOUNDING_BOX ) )
						.should( c -> c.match().onField( "string" ).matching( OURSON_QUI_BOIT_STRING ) )
				)
				.sort( c -> c.byScore() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void multi_fields() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		// onField(...).orField(...)

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" ).orField( "geoPoint_1" ).boundingBox( BOUNDING_BOX_1 ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" ).orField( "geoPoint_1" ).boundingBox( BOUNDING_BOX_2_1 ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		// onField().orFields(...)

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.boundingBox( BOUNDING_BOX_2 )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.boundingBox( BOUNDING_BOX_1_1 )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.boundingBox( BOUNDING_BOX_2_2 )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		// onFields(...)

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onFields( "geoPoint", "geoPoint_2" ).boundingBox( BOUNDING_BOX_2 ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onFields( "geoPoint", "geoPoint_2" ).boundingBox( BOUNDING_BOX_1_2 ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );
	}

	@Test
	public void boundingBox_error_null() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SubTest.expectException(
				"spatial().within().boundingBox() predicate with null bounding box",
				() -> searchTarget.predicate().spatial().within().onField( "geoPoint" ).boundingBox( null )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "HSEARCH900000" );
	}

	@Test
	public void unknown_field() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SubTest.expectException(
				"spatial().within().boundingBox() predicate on unknown field",
				() -> searchTarget.predicate().spatial().within().onField( "unknown_field" )
						.boundingBox( BOUNDING_BOX_1 ).end()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	@Override
	protected void initData() {
		super.initData();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( sessionContext );
		workPlan.add( referenceProvider( ADDITIONAL_POINT_1_ID ), document -> {
			indexAccessors.geoPoint.write( document, ADDITIONAL_POINT_1_GEO_POINT );
		} );
		workPlan.add( referenceProvider( ADDITIONAL_POINT_2_ID ), document -> {
			indexAccessors.geoPoint.write( document, ADDITIONAL_POINT_2_GEO_POINT );
		} );

		workPlan.execute().join();

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.matchAll() )
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID, CHEZ_MARGOTTE_ID, EMPTY_ID, ADDITIONAL_POINT_1_ID,
				ADDITIONAL_POINT_2_ID );
	}

	private static GeoBoundingBox moveBoundingBox(GeoBoundingBox originalBoundingBox, double degrees) {
		return GeoBoundingBox.of( originalBoundingBox.getTopLeft().getLatitude() + degrees, originalBoundingBox.getTopLeft().getLongitude() + degrees,
				originalBoundingBox.getBottomRight().getLatitude() + degrees, originalBoundingBox.getBottomRight().getLongitude() + degrees );
	}
}
