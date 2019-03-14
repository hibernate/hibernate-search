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

import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
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
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).polygon( POLYGON_2 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.asReference()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).polygon( POLYGON_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );
	}

	@Test
	public void unsupported_field_types() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

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
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.spatial().within().onField( "geoPoint" ).polygon( CHEZ_MARGOTTE_POLYGON ) )
						.should( f.match().onField( "string" ).boostedTo( 42 ).matching( OURSON_QUI_BOIT_STRING ) )
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.spatial().within().onField( "geoPoint" ).boostedTo( 42 ).polygon( CHEZ_MARGOTTE_POLYGON ) )
						.should( f.match().onField( "string" ).matching( OURSON_QUI_BOIT_STRING ) )
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void predicateLevelBoost() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.spatial().boostedTo( 0.1f ).within().onField( "geoPoint" ).polygon( CHEZ_MARGOTTE_POLYGON ) )
						.should( f.match().onField( "string" ).matching( OURSON_QUI_BOIT_STRING ) )
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.spatial().boostedTo( 39 ).within().onField( "geoPoint" ).polygon( CHEZ_MARGOTTE_POLYGON ) )
						.should( f.match().onField( "string" ).matching( OURSON_QUI_BOIT_STRING ) )
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void predicateLevelBoost_andFieldLevelBoost() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						// 0.1 * 7 => boost x0.7
						.should( f.spatial().boostedTo( 0.1f ).within().onField( "geoPoint" ).boostedTo( 7 ).polygon( CHEZ_MARGOTTE_POLYGON ) )
						// 1 * 7 => boost x7
						.should( f.match().boostedTo( 1 ).onField( "string" ).boostedTo( 7 ).matching( OURSON_QUI_BOIT_STRING ) )
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						// 39 * 10 => boost x390
						.should( f.spatial().boostedTo( 39 ).within().onField( "geoPoint" ).boostedTo( 10 ).polygon( CHEZ_MARGOTTE_POLYGON ) )
						// 20 * 15 => boost x300
						.should( f.match().boostedTo( 20 ).onField( "string" ).boostedTo( 15 ).matching( OURSON_QUI_BOIT_STRING ) )
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void predicateLevelBoost_multiFields() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.spatial().boostedTo( 0.1f ).within().onField( "geoPoint" ).orField( "geoPoint_1" ).polygon( CHEZ_MARGOTTE_POLYGON ) )
						.should( f.match().onField( "string" ).matching( OURSON_QUI_BOIT_STRING ) )
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.spatial().boostedTo( 39 ).within().onField( "geoPoint" ).orField( "geoPoint_1" ).polygon( CHEZ_MARGOTTE_POLYGON ) )
						.should( f.match().onField( "string" ).matching( OURSON_QUI_BOIT_STRING ) )
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void multi_fields() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		// onField(...).orField(...)

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orField( "geoPoint_1" ).polygon( POLYGON_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );

		query = scope.query()
				.asReference()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orField( "geoPoint_1" ).polygon( POLYGON_2_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		// onField().orFields(...)

		query = scope.query()
				.asReference()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.polygon( POLYGON_2 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.asReference()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.polygon( POLYGON_1_1 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );

		query = scope.query()
				.asReference()
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.polygon( POLYGON_2_2 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		// onFields(...)

		query = scope.query()
				.asReference()
				.predicate( f -> f.spatial().within().onFields( "geoPoint", "geoPoint_2" ).polygon( POLYGON_2 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );

		query = scope.query()
				.asReference()
				.predicate( f -> f.spatial().within().onFields( "geoPoint", "geoPoint_2" ).polygon( POLYGON_1_2 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );
	}

	@Test
	public void polygon_error_null() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

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
		StubMappingSearchScope scope = indexManager.createSearchScope();

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

	private static GeoPolygon movePolygon(GeoPolygon originalPolygon, double degrees) {
		List<GeoPoint> movedPoints = new ArrayList<>();

		for ( GeoPoint originalPoint : originalPolygon.getPoints() ) {
			movedPoints.add( GeoPoint.of( originalPoint.getLatitude() + degrees, originalPoint.getLongitude() + degrees ) );
		}

		return GeoPolygon.of( movedPoints );
	}
}
