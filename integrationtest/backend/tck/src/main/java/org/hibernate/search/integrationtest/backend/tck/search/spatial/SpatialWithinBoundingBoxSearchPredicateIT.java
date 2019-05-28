/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
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
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).boundingBox( BOUNDING_BOX_2 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).boundingBox( BOUNDING_BOX_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" )
						.boundingBox( BOUNDING_BOX_2.getTopLeft().getLatitude(), BOUNDING_BOX_2.getTopLeft().getLongitude(),
								BOUNDING_BOX_2.getBottomRight().getLatitude(), BOUNDING_BOX_2.getBottomRight().getLongitude() )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" )
						.boundingBox( BOUNDING_BOX_1.getTopLeft().getLatitude(), BOUNDING_BOX_1.getTopLeft().getLongitude(),
								BOUNDING_BOX_1.getBottomRight().getLatitude(), BOUNDING_BOX_1.getBottomRight().getLongitude() )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );
	}

	@Test
	public void within_unsearchable_boundingBox() {
		StubMappingScope scope = unsearchableFieldsIndexManager.createScope();

		SubTest.expectException( () ->
				scope.predicate().spatial().within().onField( "geoPoint" ).boundingBox( BOUNDING_BOX_2 )
		).assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "is not searchable" )
				.hasMessageContaining( "Make sure the field is marked as searchable" )
				.hasMessageContaining( "geoPoint" );
	}

	@Test
	public void boundingBox_consistency() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" )
						.boundingBox( GeoBoundingBox.of( GeoPoint.of( 25, 23 ), GeoPoint.of( 24, 26 ) ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, ADDITIONAL_POINT_1_ID, ADDITIONAL_POINT_2_ID );
	}

	@Test
	public void unsupported_field_types() {
		StubMappingScope scope = indexManager.createScope();

		SubTest.expectException(
				"spatial().within().boundingBox() predicate on field with unsupported type",
				() -> scope.predicate().spatial().within().onField( "string" ).boundingBox( BOUNDING_BOX_1 )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Spatial predicates are not supported by" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( "string" )
				) );
	}

	@Test
	public void fieldLevelBoost() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						// Base score: less than 2
						.should( f.spatial().within().onField( "geoPoint" )
								.boundingBox( CHEZ_MARGOTTE_BOUNDING_BOX )
						)
						// Constant score: 2
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.withConstantScore().boostedTo( 2 )
						)
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.bool()
						// Base score boosted 42x: more than 2
						.should( f.spatial().within().onField( "geoPoint" ).boostedTo( 42 )
								.boundingBox( CHEZ_MARGOTTE_BOUNDING_BOX )
						)
						// Constant score: 2
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.withConstantScore().boostedTo( 2 )
						)
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void predicateLevelBoost() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						// Base score: less than 2
						.should( f.spatial().within().onField( "geoPoint" )
								.boundingBox( CHEZ_MARGOTTE_BOUNDING_BOX )
						)
						// Constant score: 2
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.withConstantScore().boostedTo( 2 )
						)
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.bool()
						// Base score boosted 39x: more than 2
						.should( f.spatial().within().onField( "geoPoint" )
								.boundingBox( CHEZ_MARGOTTE_BOUNDING_BOX )
								.boostedTo( 39 )
						)
						// Constant score: 2
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.withConstantScore().boostedTo( 2 )
						)
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void predicateLevelBoost_andFieldLevelBoost() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						// Base score boosted 2*3=6x: less than 8
						.should( f.spatial().within().onField( "geoPoint" ).boostedTo( 3 )
								.boundingBox( CHEZ_MARGOTTE_BOUNDING_BOX )
								.boostedTo( 2 )
						)
						// Constant score: 8
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.withConstantScore().boostedTo( 8 )
						)
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.bool()
						// Base score boosted 3*4=12x: more than 8
						.should( f.spatial().within().onField( "geoPoint" ).boostedTo( 4 )
								.boundingBox( CHEZ_MARGOTTE_BOUNDING_BOX )
								.boostedTo( 3 )
						)
						// Constant score: 8
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.withConstantScore().boostedTo( 8 )
						)
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void predicateLevelBoost_multiFields() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						// Base score boosted 0.001x: less than 2
						.should( f.spatial().within().onField( "geoPoint" )
								.orField( "geoPoint_1" )
								.boundingBox( CHEZ_MARGOTTE_BOUNDING_BOX )
								.boostedTo( 0.001f )
						)
						// Constant score: 2
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.withConstantScore().boostedTo( 2 )
						)
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.bool()
						// Base score boosted 39x: more than 2
						.should( f.spatial().within().onField( "geoPoint" )
								.orField( "geoPoint_1" )
								.boundingBox( CHEZ_MARGOTTE_BOUNDING_BOX )
								.boostedTo( 39 )
						)
						// Constant score: 2
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.withConstantScore().boostedTo( 2 )
						)
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void multi_fields() {
		StubMappingScope scope = indexManager.createScope();

		// onField(...).orField(...)

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orField( "geoPoint_1" ).boundingBox( BOUNDING_BOX_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orField( "geoPoint_1" ).boundingBox( BOUNDING_BOX_2_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		// onField().orFields(...)

		query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.boundingBox( BOUNDING_BOX_2 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.boundingBox( BOUNDING_BOX_1_1 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.boundingBox( BOUNDING_BOX_2_2 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		// onFields(...)

		query = scope.query()
				.predicate( f -> f.spatial().within().onFields( "geoPoint", "geoPoint_2" ).boundingBox( BOUNDING_BOX_2 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within().onFields( "geoPoint", "geoPoint_2" ).boundingBox( BOUNDING_BOX_1_2 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );
	}

	@Test
	public void boundingBox_error_null() {
		StubMappingScope scope = indexManager.createScope();

		SubTest.expectException(
				"spatial().within().boundingBox() predicate with null bounding box",
				() -> scope.predicate().spatial().within().onField( "geoPoint" ).boundingBox( null )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "HSEARCH900000" );
	}

	@Test
	public void unknown_field() {
		StubMappingScope scope = indexManager.createScope();

		SubTest.expectException(
				"spatial().within().boundingBox() predicate on unknown field",
				() -> scope.predicate().spatial().within().onField( "unknown_field" )
						.boundingBox( BOUNDING_BOX_1 ).toPredicate()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	@Test
	public void multiIndex_withCompatibleIndexManager() {
		StubMappingScope scope = indexManager.createScope( compatibleIndexManager );

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).boundingBox( BOUNDING_BOX_2 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );
	}

	@Test
	public void multiIndex_incompatibleSearchable() {
		StubMappingScope scope = indexManager.createScope( unsearchableFieldsIndexManager );

		SubTest.expectException( () -> scope.predicate().spatial().within().onField( "geoPoint" ).boundingBox( BOUNDING_BOX_2 ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( "geoPoint" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, UNSEARCHABLE_FIELDS_INDEX_NAME )
				) );
	}

	@Override
	protected void initData() {
		super.initData();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( ADDITIONAL_POINT_1_ID ), document -> {
			document.addValue( indexMapping.geoPoint, ADDITIONAL_POINT_1_GEO_POINT );
		} );
		workPlan.add( referenceProvider( ADDITIONAL_POINT_2_ID ), document -> {
			document.addValue( indexMapping.geoPoint, ADDITIONAL_POINT_2_GEO_POINT );
		} );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID, CHEZ_MARGOTTE_ID, EMPTY_ID, ADDITIONAL_POINT_1_ID,
				ADDITIONAL_POINT_2_ID );
	}

	private static GeoBoundingBox moveBoundingBox(GeoBoundingBox originalBoundingBox, double degrees) {
		return GeoBoundingBox.of( originalBoundingBox.getTopLeft().getLatitude() + degrees, originalBoundingBox.getTopLeft().getLongitude() + degrees,
				originalBoundingBox.getBottomRight().getLatitude() + degrees, originalBoundingBox.getBottomRight().getLongitude() + degrees );
	}
}
