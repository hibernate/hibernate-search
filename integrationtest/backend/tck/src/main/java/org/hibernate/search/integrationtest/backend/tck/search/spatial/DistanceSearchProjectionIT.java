/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

public class DistanceSearchProjectionIT extends AbstractSpatialWithinSearchPredicateIT {

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3618")
	public void distanceProjection_unsortable() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<Double> query = scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( f ->
						f.distance( "projectableUnsortableGeoPoint", GeoPoint.of( 45.749828, 4.854172 ) )
				)
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "string" ).missing().last().asc() )
				.toQuery();
		SearchResult<Double> results = query.fetchAll();

		checkResult( results.hits().get( 0 ), 430d, Offset.offset( 10d ) );
		checkResult( results.hits().get( 1 ), 1300d, Offset.offset( 10d ) );
		checkResult( results.hits().get( 2 ), 2730d, Offset.offset( 10d ) );
		checkResult( results.hits().get( 3 ), null, null );
	}

	@Test
	public void distanceProjection_sortable() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<Double> query = scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( f ->
						f.distance( "geoPoint", GeoPoint.of( 45.749828, 4.854172 ) )
				)
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "string" ).missing().last().asc() )
				.toQuery();
		SearchResult<Double> results = query.fetchAll();

		checkResult( results.hits().get( 0 ), 430d, Offset.offset( 10d ) );
		checkResult( results.hits().get( 1 ), 1300d, Offset.offset( 10d ) );
		checkResult( results.hits().get( 2 ), 2730d, Offset.offset( 10d ) );
		checkResult( results.hits().get( 3 ), null, null );
	}

	@Test
	public void distanceProjection_unit() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<Double> query = scope.query()
				.select( f ->
						f.distance( "geoPoint", GeoPoint.of( 45.749828, 4.854172 ) )
								.unit( DistanceUnit.KILOMETERS )
				)
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "string" ).missing().last().asc() )
				.toQuery();
		SearchResult<Double> results = query.fetchAll();

		checkResult( results.hits().get( 0 ), 0.430d, Offset.offset( 0.010d ) );
		checkResult( results.hits().get( 1 ), 1.300d, Offset.offset( 0.010d ) );
		checkResult( results.hits().get( 2 ), 2.730d, Offset.offset( 0.010d ) );
		checkResult( results.hits().get( 3 ), null, null );
	}

	@Test
	public void distanceProjection_several() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<List<?>> query = scope.query()
				.select( f ->
						f.composite(
								f.distance( "geoPoint", GeoPoint.of( 45.749828, 4.854172 ) ),
								f.distance( "geoPoint", GeoPoint.of( 45.763363, 4.833527 ) ),
								f.distance( "geoPoint_1", GeoPoint.of( 45.749828, 4.854172 ) )
										.unit( DistanceUnit.KILOMETERS )
						)
				)
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "string" ).missing().last().asc() )
				.toQuery();
		SearchResult<List<?>> results = query.fetchAll();

		checkResult( (Double) results.hits().get( 0 ).get( 0 ), 430d, Offset.offset( 10d ) );
		checkResult( (Double) results.hits().get( 1 ).get( 0 ), 1300d, Offset.offset( 10d ) );
		checkResult( (Double) results.hits().get( 2 ).get( 0 ), 2730d, Offset.offset( 10d ) );
		checkResult( (Double) results.hits().get( 3 ).get( 0 ), null, null );

		checkResult( (Double) results.hits().get( 0 ).get( 1 ), 1780d, Offset.offset( 10d ) );
		checkResult( (Double) results.hits().get( 1 ).get( 1 ), 1095d, Offset.offset( 10d ) );
		checkResult( (Double) results.hits().get( 2 ).get( 1 ), 812d, Offset.offset( 10d ) );
		checkResult( (Double) results.hits().get( 3 ).get( 1 ), null, null );

		checkResult( (Double) results.hits().get( 0 ).get( 2 ), 136d, Offset.offset( 10d ) );
		checkResult( (Double) results.hits().get( 1 ).get( 2 ), 136d, Offset.offset( 10d ) );
		checkResult( (Double) results.hits().get( 2 ).get( 2 ), 136d, Offset.offset( 10d ) );
		checkResult( (Double) results.hits().get( 3 ).get( 2 ), null, null );
	}

	@Test
	public void distanceProjection_distanceSort() {
		StubMappingScope scope = mainIndex.createScope();

		GeoPoint center = GeoPoint.of( 45.749828, 4.854172 );

		SearchQuery<Double> query = scope.query()
				.select( f ->
						f.distance( "geoPoint", center )
				)
				.where( f -> f.matchAll() )
				.sort( f -> f
						.distance( "geoPoint", GeoPoint.of( 43.749828, 1.854172 ) ).then()
						.distance( "geoPoint", center )
				)
				.toQuery();
		SearchResult<Double> results = query.fetchAll();

		checkResult( results.hits().get( 0 ), 1300d, Offset.offset( 10d ) );
		checkResult( results.hits().get( 1 ), 430d, Offset.offset( 10d ) );
		checkResult( results.hits().get( 2 ), 2730d, Offset.offset( 10d ) );
		checkResult( results.hits().get( 3 ), null, null );
	}

	@Test
	public void distanceProjection_longCalculatedField() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<Double> query = scope.query()
				.select( f ->
						f.distance(
								"geoPoint_with_a_veeeeeeeeeeeeeeeeeeeeerrrrrrrrrrrrrrrrrryyyyyyyyyyyyyyyy_long_name",
								GeoPoint.of( 45.74982800099999888371, 4.85417200099999888371 )
						)
				)
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "string" ).missing().last().asc() )
				.toQuery();
		SearchResult<Double> results = query.fetchAll();

		checkResult( results.hits().get( 0 ), 430d, Offset.offset( 10d ) );
		checkResult( results.hits().get( 1 ), 1300d, Offset.offset( 10d ) );
		checkResult( results.hits().get( 2 ), 2730d, Offset.offset( 10d ) );
		checkResult( results.hits().get( 3 ), null, null );
	}

	@Test
	public void distanceProjection_invalidType() {
		StubMappingScope scope = mainIndex.createScope();

		assertThatThrownBy( () -> scope.projection()
				.distance( "string", GeoPoint.of( 43.749828, 1.854172 ) )
				.toProjection()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Distance related operations are not supported",
						"string"
				);
	}

	@Test
	public void distanceProjection_nullCenter() {
		StubMappingScope scope = mainIndex.createScope();

		assertThatThrownBy( () -> scope.projection()
				.distance( "geoPoint", null )
				.toProjection()
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll(
						"center",
						"must not be null"
				);
	}

	@Test
	public void distanceProjection_nullUnit() {
		StubMappingScope scope = mainIndex.createScope();

		assertThatThrownBy( () -> scope.projection()
				.distance( "geoPoint", GeoPoint.of( 45.749828, 4.854172 ) ).unit( null )
				.toProjection()
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll(
						"unit",
						"must not be null"
				);
	}

	@Test
	public void distanceProjection_nonProjectable() {
		StubMappingScope scope = mainIndex.createScope();

		Assertions.assertThatThrownBy( () -> {
			scope.projection().field( "nonProjectableGeoPoint", GeoPoint.class ).toProjection();
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Projections are not enabled for field" )
				.hasMessageContaining( "nonProjectableGeoPoint" );

		Assertions.assertThatThrownBy( () -> {
			scope.projection().distance( "nonProjectableGeoPoint", GeoPoint.of( 43d, 4d ) ).toProjection();
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Projections are not enabled for field" )
				.hasMessageContaining( "nonProjectableGeoPoint" );
	}

	@Test
	public void distanceSort_unsortable() {
		StubMappingScope scope = mainIndex.createScope();

		Assertions.assertThatThrownBy( () -> {
			scope.sort().distance( "unsortableGeoPoint", GeoPoint.of( 43d, 4d ) );
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Sorting is not enabled for field" )
				.hasMessageContaining( "unsortableGeoPoint" );
	}

	private void checkResult(Double actual, Double expected, Offset<Double> offset) {
		if ( expected == null ) {
			Assertions.assertThat( actual ).isNull();
		}
		else {
			Assertions.assertThat( actual )
					.isCloseTo( expected, offset );
		}
	}
}
