/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
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
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" )
						.circle( METRO_GARIBALDI, 1_500 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" )
						.circle( METRO_HOTEL_DE_VILLE, 500 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" )
						.circle( METRO_GARIBALDI.getLatitude(), METRO_GARIBALDI.getLongitude(), 1_500 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" )
						.circle( METRO_GARIBALDI.getLatitude(), METRO_GARIBALDI.getLongitude(), 1.5, DistanceUnit.KILOMETERS )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" )
						.circle( METRO_GARIBALDI, 1.5, DistanceUnit.KILOMETERS )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );
	}

	@Test
	public void within_unsearchable_circle() {
		StubMappingScope scope = unsearchableFieldsIndexManager.createScope();

		SubTest.expectException( () ->
				scope.predicate().spatial().within().onField( "geoPoint" ).circle( METRO_GARIBALDI, 1_500 )
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
				"spatial().within().circle() predicate on field with unsupported type",
				() -> scope.predicate().spatial().within()
						.onField( "string" )
						.circle( METRO_GARIBALDI, 400 )
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
						.should( f.spatial().within()
								.onField( "geoPoint" )
								.circle( METRO_GARIBALDI, 400 )
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
						.should( f.spatial().within()
								.onField( "geoPoint" ).boostedTo( 42 )
								.circle( METRO_GARIBALDI, 400 )
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
						// Base score boosted 0.123x: less than 2
						.should( f.spatial().within()
								.onField( "geoPoint" )
								.circle( METRO_GARIBALDI, 400 )
								.boostedTo( 0.123f )
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
						.should( f.spatial().within()
								.onField( "geoPoint" )
								.circle( METRO_GARIBALDI, 400 )
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
						// Base score boosted 0.123*4=0.492x: less than 2
						.should( f.spatial().within()
								.onField( "geoPoint" ).boostedTo( 4 )
								.circle( METRO_GARIBALDI, 400 )
								.boostedTo( 0.123f )
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
						// Base score boosted 39*7*4=273x: more than 2
						.should( f.spatial().within()
								.onField( "geoPoint" ).boostedTo( 7 )
								.circle( METRO_GARIBALDI, 400 )
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
	public void predicateLevelBoost_multiFields() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						// Base score boosted 0.123x: less than 2
						.should( f.spatial().within()
								.onField( "geoPoint" ).orField( "geoPoint_1" )
								.circle( METRO_GARIBALDI, 400 )
								.boostedTo( 0.123f )
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
						.should( f.spatial().within()
								.onField( "geoPoint" ).orField( "geoPoint_1" )
								.circle( METRO_GARIBALDI, 400 )
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
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" ).orField( "geoPoint_1" )
						.circle( METRO_GARIBALDI, 400 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" ).orField( "geoPoint_1" )
						.circle( METRO_HOTEL_DE_VILLE_1, 500 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID );

		// onField().orFields(...)

		query = scope.query()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.circle( METRO_HOTEL_DE_VILLE, 500 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.circle( METRO_GARIBALDI_1, 1_500 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within()
						.onField( "geoPoint" ).orFields( "geoPoint_1" ).orFields( "geoPoint_2" )
						.circle( METRO_GARIBALDI_2, 400 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID );

		// onFields(...)

		query = scope.query()
				.predicate( f -> f.spatial().within()
						.onFields( "geoPoint", "geoPoint_2" )
						.circle( METRO_GARIBALDI, 400 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID );

		query = scope.query()
				.predicate( f -> f.spatial().within()
						.onFields( "geoPoint", "geoPoint_2" )
						.circle( METRO_HOTEL_DE_VILLE_2, 500 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void circle_error_null() {
		StubMappingScope scope = indexManager.createScope();

		SubTest.expectException(
				"spatial().within().circle() predicate with null center",
				() -> scope.predicate().spatial().within().onField( "geoPoint" )
						.circle( null, 100 )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "HSEARCH900000" );

		SubTest.expectException(
				"spatial().within().circle() predicate with null distance unit",
				() -> scope.predicate().spatial().within().onField( "geoPoint" )
						.circle( GeoPoint.of( 45, 4 ), 100, null )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "HSEARCH900000" );
	}

	@Test
	public void unknown_field() {
		StubMappingScope scope = indexManager.createScope();

		SubTest.expectException(
				"spatial().within().circle() predicate on unknown field",
				() -> scope.predicate().spatial().within().onField( "unknown_field" )
						.circle( METRO_GARIBALDI, 100 ).toPredicate()
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
				.predicate( f -> f.spatial().within().onField( "geoPoint" ).circle( METRO_GARIBALDI, 1_500 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, CHEZ_MARGOTTE_ID, IMOUTO_ID );
	}

	@Test
	public void multiIndex_incompatibleSearchable() {
		StubMappingScope scope = indexManager.createScope( unsearchableFieldsIndexManager );

		SubTest.expectException( () -> scope.predicate().spatial().within().onField( "geoPoint" ).circle( METRO_GARIBALDI, 1_500 ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( "geoPoint" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, UNSEARCHABLE_FIELDS_INDEX_NAME )
				) );
	}
}
