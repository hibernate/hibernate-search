/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert;
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
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" ).polygon( POLYGON_2 ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" ).polygon( POLYGON_1 ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );
	}

	@Test
	public void unsupported_field_types() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SubTest.expectException(
				"spatial().within().polygon() predicate on field with unsupported type",
				() -> searchTarget.predicate().spatial().within().onField( "string" ).polygon( POLYGON_1 )
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
						.should( c -> c.spatial().within().onField( "geoPoint" ).polygon( CHEZ_MARGOTTE_POLYGON ) )
						.should( c -> c.match().onField( "string" ).boostedTo( 42 ).matching( OURSON_QUI_BOIT_STRING ) )
				)
				.sort( c -> c.byScore() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.bool()
						.should( c -> c.spatial().within().onField( "geoPoint" ).boostedTo( 42 ).polygon( CHEZ_MARGOTTE_POLYGON ) )
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
				.predicate( root -> root.spatial().within().onField( "geoPoint" ).orField( "geoPoint_1" ).polygon( POLYGON_1 ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" ).orField( "geoPoint_1" ).polygon( POLYGON_2_1 ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		// onField().orFields(...)

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.polygon( POLYGON_2 )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.polygon( POLYGON_1_1 )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.polygon( POLYGON_2_2 )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, IMOUTO_ID, CHEZ_MARGOTTE_ID );

		// onFields(...)

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onFields( "geoPoint", "geoPoint_2" ).polygon( POLYGON_2 ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.spatial().within().onFields( "geoPoint", "geoPoint_2" ).polygon( POLYGON_1_2 ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID );
	}

	@Test
	public void polygon_error_null() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SubTest.expectException(
				"spatial().within().boundingBox() predicate with null polygon",
				() -> searchTarget.predicate().spatial().within().onField( "geoPoint" ).polygon( null )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "HSEARCH900000" );
	}

	@Test
	public void unknown_field() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SubTest.expectException(
				"spatial().within().polygon() predicate on unknown field",
				() -> searchTarget.predicate().spatial().within().onField( "unknown_field" )
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
