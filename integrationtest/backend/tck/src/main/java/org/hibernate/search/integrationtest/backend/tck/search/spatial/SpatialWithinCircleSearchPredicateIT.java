/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Test;

public class SpatialWithinCircleSearchPredicateIT extends AbstractSpatialWithinSearchPredicateIT {

	private static final GeoPoint METRO_HOTEL_DE_VILLE = GeoPoint.of( 45.7673396, 4.833743 );
	private static final GeoPoint METRO_GARIBALDI = GeoPoint.of( 45.7515926, 4.8514779 );

	private static final GeoPoint METRO_HOTEL_DE_VILLE_1 = GeoPoint.of( METRO_HOTEL_DE_VILLE.getLatitude() - 1,
			METRO_HOTEL_DE_VILLE.getLongitude() - 1 );
	private static final GeoPoint METRO_HOTEL_DE_VILLE_2 = GeoPoint.of( METRO_HOTEL_DE_VILLE.getLatitude() - 2,
			METRO_HOTEL_DE_VILLE.getLongitude() - 2 );
	private static final GeoPoint METRO_GARIBALDI_1 = GeoPoint.of( METRO_GARIBALDI.getLatitude() - 1, METRO_GARIBALDI.getLongitude() - 1 );
	private static final GeoPoint METRO_GARIBALDI_2 = GeoPoint.of( METRO_GARIBALDI.getLatitude() - 2, METRO_GARIBALDI.getLongitude() - 2 );

	@Test
	public void within_circle() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" )
						.circle( METRO_GARIBALDI, 1_500 )
						.toPredicate()
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" )
						.circle( METRO_HOTEL_DE_VILLE, 500 )
						.toPredicate()
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" )
						.circle( METRO_GARIBALDI.getLatitude(), METRO_GARIBALDI.getLongitude(), 1_500 )
						.toPredicate()
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" )
						.circle( METRO_GARIBALDI.getLatitude(), METRO_GARIBALDI.getLongitude(), 1.5, DistanceUnit.KILOMETERS )
						.toPredicate()
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" )
						.circle( METRO_GARIBALDI, 1.5, DistanceUnit.KILOMETERS )
						.toPredicate()
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );
	}

	@Test
	public void unsupported_field_types() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SubTest.expectException(
				"spatial().within().circle() predicate on field with unsupported type",
				() -> searchTarget.predicate().spatial().within()
						.onField( "string" )
						.circle( METRO_GARIBALDI, 400 )
						.toPredicate()
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
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.spatial().within()
								.onField( "geoPoint" )
								.circle( METRO_GARIBALDI, 400 )
						)
						.should( f.match()
								.onField( "string" ).boostedTo( 42 )
								.matching( OURSON_QUI_BOIT_STRING )
						)
						.toPredicate()
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, CHEZ_MARGOTTE_ID );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.spatial().within()
								.onField( "geoPoint" ).boostedTo( 42 )
								.circle( METRO_GARIBALDI, 400 )
						)
						.should( f.match()
								.onField( "string" )
								.matching( OURSON_QUI_BOIT_STRING )
						)
						.toPredicate()
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void multi_fields() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		// onField(...).orField(...)

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" ).orField( "geoPoint_1" )
						.circle( METRO_GARIBALDI, 400 )
						.toPredicate()
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" ).orField( "geoPoint_1" )
						.circle( METRO_HOTEL_DE_VILLE_1, 500 )
						.toPredicate()
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID );

		// onField().orFields(...)

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.circle( METRO_HOTEL_DE_VILLE, 500 )
						.toPredicate()
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.circle( METRO_GARIBALDI_1, 1_500 )
						.toPredicate()
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.circle( METRO_GARIBALDI_2, 400 )
						.toPredicate()
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID );

		// onFields(...)

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.spatial().within()
						.onFields( "geoPoint", "geoPoint_2" )
						.circle( METRO_GARIBALDI, 400 )
						.toPredicate()
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.spatial().within()
						.onFields( "geoPoint", "geoPoint_2" )
						.circle( METRO_HOTEL_DE_VILLE_2, 500 )
						.toPredicate()
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void circle_error_null() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SubTest.expectException(
				"spatial().within().circle() predicate with null center",
				() -> searchTarget.predicate().spatial().within().onField( "geoPoint" )
						.circle( null, 100 )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "HSEARCH900000" );

		SubTest.expectException(
				"spatial().within().circle() predicate with null distance unit",
				() -> searchTarget.predicate().spatial().within().onField( "geoPoint" )
						.circle( GeoPoint.of( 45, 4 ), 100, null )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "HSEARCH900000" );
	}

	@Test
	public void unknown_field() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SubTest.expectException(
				"spatial().within().circle() predicate on unknown field",
				() -> searchTarget.predicate().spatial().within().onField( "unknown_field" )
						.circle( METRO_GARIBALDI, 100 ).toPredicate()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}
}
