/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Test;

public class SpatialWithinPolygonSearchPredicateIT extends AbstractSpatialWithinSearchPredicateIT {

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

	private static final GeoPolygon POLYGON_1_1 = movePolygon( POLYGON_1, -1 );
	private static final GeoPolygon POLYGON_1_2 = movePolygon( POLYGON_1, -2 );
	private static final GeoPolygon POLYGON_2_1 = movePolygon( POLYGON_2, -1 );
	private static final GeoPolygon POLYGON_2_2 = movePolygon( POLYGON_2, -2 );

	@Test
	public void within_polygon() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).polygon( POLYGON_2 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).polygon( POLYGON_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );
	}

	@Test
	public void within_unsearchable_polygon() {
		StubMappingScope scope = unsearchableFieldsIndexManager.createScope();

		SubTest.expectException( () ->
				scope.predicate().spatial().within().onField( "geoPoint" ).polygon( POLYGON_2 )
		).assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "is not searchable" )
				.hasMessageContaining( "Make sure the field is marked as searchable" )
				.hasMessageContaining( "geoPoint" );
	}

	@Test
	public void unsupported_field_types() {
		StubMappingScope scope = indexManager.createScope();

		SubTest.expectException(
				"spatial().within().polygon() predicate on field with unsupported type",
				() -> scope.predicate().spatial().within().onField( "string" ).polygon( POLYGON_1 )
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
						.should( f.spatial().within().onField( "geoPoint" ).polygon( CHEZ_MARGOTTE_POLYGON ) )
						// Constant score: 2
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.constantScore().boost( 2 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.bool()
						// Base score boosted 42x: more than 2
						.should( f.spatial().within().onField( "geoPoint" ).boost( 42 ).polygon( CHEZ_MARGOTTE_POLYGON ) )
						// Constant score: 2
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.constantScore().boost( 2 )
						)
				)
				.sort( f -> f.score() )
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
								.polygon( CHEZ_MARGOTTE_POLYGON )
								.boost( 0.1f )
						)
						// Constant score: 2
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.constantScore().boost( 2 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.bool()
						// Base score boosted 39x: more than 2
						.should( f.spatial().within().onField( "geoPoint" )
								.polygon( CHEZ_MARGOTTE_POLYGON )
								.boost( 39 )
						)
						// Constant score: 2
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.constantScore().boost( 2 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void predicateLevelBoost_andFieldLevelBoost() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						// Base score boosted 0.1*7=0.7x: less than 8
						.should( f.spatial().within().onField( "geoPoint" ).boost( 7 )
								.polygon( CHEZ_MARGOTTE_POLYGON )
								.boost( 0.1f )
						)
						// Constant score: 8
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.constantScore().boost( 8 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.bool()
						// Base score boosted 39*10=390x: more than 8
						.should( f.spatial().within().onField( "geoPoint" ).boost( 10 )
								.polygon( CHEZ_MARGOTTE_POLYGON )
								.boost( 39 )
						)
						// Constant score: 8
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.constantScore().boost( 8 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void predicateLevelBoost_multiFields() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						// Base score boosted 0.1x: less than 2
						.should( f.spatial().within().onField( "geoPoint" )
								.orField( "geoPoint_1" )
								.polygon( CHEZ_MARGOTTE_POLYGON )
								.boost( 0.1f )
						)
						// Constant score: 2
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.constantScore().boost( 2 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.bool()
						// Base score boosted 39x: more than 2
						.should( f.spatial().within().onField( "geoPoint" )
								.orField( "geoPoint_1" )
								.polygon( CHEZ_MARGOTTE_POLYGON )
								.boost( 39 )
						)
						// Constant score: 2
						.should( f.match().onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
								.constantScore().boost( 2 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void multi_fields() {
		StubMappingScope scope = indexManager.createScope();

		// onField(...).orField(...)

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orField( "geoPoint_1" ).polygon( POLYGON_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orField( "geoPoint_1" ).polygon( POLYGON_2_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		// onField().orFields(...)

		query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.polygon( POLYGON_2 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.polygon( POLYGON_1_1 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.polygon( POLYGON_2_2 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		// onFields(...)

		query = scope.query()
				.predicate( f -> f.spatial().within().onFields( "geoPoint", "geoPoint_2" ).polygon( POLYGON_2 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within().onFields( "geoPoint", "geoPoint_2" ).polygon( POLYGON_1_2 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );
	}

	@Test
	public void polygon_error_null() {
		StubMappingScope scope = indexManager.createScope();

		SubTest.expectException(
				"spatial().within().boundingBox() predicate with null polygon",
				() -> scope.predicate().spatial().within().onField( "geoPoint" ).polygon( null )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "HSEARCH900000" );
	}

	@Test
	public void unknown_field() {
		StubMappingScope scope = indexManager.createScope();

		SubTest.expectException(
				"spatial().within().polygon() predicate on unknown field",
				() -> scope.predicate().spatial().within().onField( "unknown_field" )
						.polygon( POLYGON_1 ).toPredicate()
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
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).polygon( POLYGON_2 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );
	}

	@Test
	public void multiIndex_incompatibleSearchable() {
		StubMappingScope scope = indexManager.createScope( unsearchableFieldsIndexManager );

		SubTest.expectException( () -> scope.predicate().spatial().within().onField( "geoPoint" ).polygon( POLYGON_2 ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( "geoPoint" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, UNSEARCHABLE_FIELDS_INDEX_NAME )
				) );
	}

	private static GeoPolygon movePolygon(GeoPolygon originalPolygon, double degrees) {
		List<GeoPoint> movedPoints = new ArrayList<>();

		for ( GeoPoint originalPoint : originalPolygon.getPoints() ) {
			movedPoints.add( GeoPoint.of( originalPoint.getLatitude() + degrees, originalPoint.getLongitude() + degrees ) );
		}

		return GeoPolygon.of( movedPoints );
	}
}
