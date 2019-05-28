/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DistanceSearchProjectionIT extends AbstractSpatialWithinSearchPredicateIT {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void distanceProjection() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<List<?>> query = scope.query()
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.distance( "geoPoint", GeoPoint.of( 45.749828, 4.854172 ) )
						)
				)
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( "string" ).onMissingValue().sortLast().asc() )
				.toQuery();
		SearchResult<List<?>> results = query.fetch();

		checkResult( results.getHits().get( 0 ), "Chez Margotte", 1, 430d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 1 ), "Imouto", 1, 1300d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 2 ), "L'ourson qui boit", 1, 2730d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 3 ), null, 1, null, null );
	}

	@Test
	public void distanceProjection_unit() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<List<?>> query = scope.query()
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.distance( "geoPoint", GeoPoint.of( 45.749828, 4.854172 ) )
										.unit( DistanceUnit.KILOMETERS )
						)
				)
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( "string" ).onMissingValue().sortLast().asc() )
				.toQuery();
		SearchResult<List<?>> results = query.fetch();

		checkResult( results.getHits().get( 0 ), "Chez Margotte", 1, 0.430d, Offset.offset( 0.010d ) );
		checkResult( results.getHits().get( 1 ), "Imouto", 1, 1.300d, Offset.offset( 0.010d ) );
		checkResult( results.getHits().get( 2 ), "L'ourson qui boit", 1, 2.730d, Offset.offset( 0.010d ) );
		checkResult( results.getHits().get( 3 ), null, 1, null, null );
	}

	@Test
	public void distanceProjection_several() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<List<?>> query = scope.query()
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.distance( "geoPoint", GeoPoint.of( 45.749828, 4.854172 ) ),
								f.distance( "geoPoint", GeoPoint.of( 45.763363, 4.833527 ) ),
								f.distance( "geoPoint_1", GeoPoint.of( 45.749828, 4.854172 ) )
										.unit( DistanceUnit.KILOMETERS )
						)
				)
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( "string" ).onMissingValue().sortLast().asc() )
				.toQuery();
		SearchResult<List<?>> results = query.fetch();

		checkResult( results.getHits().get( 0 ), "Chez Margotte", 1, 430d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 1 ), "Imouto", 1, 1300d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 2 ), "L'ourson qui boit", 1, 2730d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 3 ), null, 1, null, null );

		checkResult( results.getHits().get( 0 ), "Chez Margotte", 2, 1780d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 1 ), "Imouto", 2, 1095d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 2 ), "L'ourson qui boit", 2, 812d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 3 ), null, 2, null, null );

		checkResult( results.getHits().get( 0 ), "Chez Margotte", 3, 136d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 1 ), "Imouto", 3, 136d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 2 ), "L'ourson qui boit", 3, 136d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 3 ), null, 3, null, null );
	}

	@Test
	public void distanceProjection_distanceSort() {
		StubMappingScope scope = indexManager.createScope();

		GeoPoint center = GeoPoint.of( 45.749828, 4.854172 );

		SearchQuery<List<?>> query = scope.query()
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.distance( "geoPoint", center )
						)
				)
				.predicate( f -> f.matchAll() )
				.sort( c -> c
						.byField( "string" ).onMissingValue().sortLast().asc().then()
						.byDistance( "geoPoint", GeoPoint.of( 43.749828, 1.854172 ) ).then()
						.byDistance( "geoPoint", center )
				)
				.toQuery();
		SearchResult<List<?>> results = query.fetch();

		checkResult( results.getHits().get( 0 ), "Chez Margotte", 1, 430d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 1 ), "Imouto", 1, 1300d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 2 ), "L'ourson qui boit", 1, 2730d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 3 ), null, 1, null, null );
	}

	@Test
	public void distanceProjection_longCalculatedField() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<List<?>> query = scope.query()
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.distance(
										"geoPoint_with_a_veeeeeeeeeeeeeeeeeeeeerrrrrrrrrrrrrrrrrryyyyyyyyyyyyyyyy_long_name",
										GeoPoint.of( 45.74982800099999888371, 4.85417200099999888371 )
								)
						)
				)
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( "string" ).onMissingValue().sortLast().asc() )
				.toQuery();
		SearchResult<List<?>> results = query.fetch();

		checkResult( results.getHits().get( 0 ), "Chez Margotte", 1, 430d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 1 ), "Imouto", 1, 1300d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 2 ), "L'ourson qui boit", 1, 2730d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 3 ), null, 1, null, null );
	}

	@Test
	public void distanceProjection_invalidType() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Distance related operations are not supported" );
		thrown.expectMessage( "string" );

		StubMappingScope scope = indexManager.createScope();
		scope.projection()
				.distance( "string", GeoPoint.of( 43.749828, 1.854172 ) )
				.toProjection();
	}

	@Test
	public void distanceProjection_nullCenter() {
		thrown.expect( IllegalArgumentException.class );
		thrown.expectMessage( "center" );
		thrown.expectMessage( "must not be null" );

		StubMappingScope scope = indexManager.createScope();
		scope.projection()
				.distance( "geoPoint", null )
				.toProjection();
	}

	@Test
	public void distanceProjection_nullUnit() {
		thrown.expect( IllegalArgumentException.class );
		thrown.expectMessage( "unit" );
		thrown.expectMessage( "must not be null" );

		StubMappingScope scope = indexManager.createScope();
		scope.projection()
				.distance( "geoPoint", GeoPoint.of( 45.749828, 4.854172 ) ).unit( null )
				.toProjection();
	}

	@Test
	public void distanceProjection_nonProjectable() {
		StubMappingScope scope = indexManager.createScope();

		SubTest.expectException( () -> {
			scope.projection().field( "nonProjectableGeoPoint", GeoPoint.class ).toProjection();
		} ).assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Projections are not enabled for field" )
				.hasMessageContaining( "nonProjectableGeoPoint" );

		SubTest.expectException( () -> {
			scope.projection().distance( "nonProjectableGeoPoint", GeoPoint.of( 43d, 4d ) ).toProjection();
		} ).assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Projections are not enabled for field" )
				.hasMessageContaining( "nonProjectableGeoPoint" );
	}

	@Test
	public void distanceSort_unsortable() {
		StubMappingScope scope = indexManager.createScope();

		SubTest.expectException( () -> {
			scope.sort().byDistance( "unsortableGeoPoint", GeoPoint.of( 43d, 4d ) );
		} ).assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Sorting is not enabled for field" )
				.hasMessageContaining( "unsortableGeoPoint" );
	}

	private void checkResult(List<?> result, String name, int distanceIndex, Double distance, Offset<Double> offset) {
		Assertions.assertThat( result.get( 0 ) ).as( name + " - name" ).isEqualTo( name );
		if ( distance == null ) {
			Assertions.assertThat( result.get( distanceIndex ) ).as( name + " - distance" ).isNull();
		}
		else {
			Assertions.assertThat( (Double) result.get( distanceIndex ) )
					.as( name + " - distance" )
					.isCloseTo( distance, offset );
		}
	}
}
