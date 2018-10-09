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
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.SearchException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DistanceSearchProjectionIT extends AbstractSpatialWithinSearchPredicateIT {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void distanceProjection() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<List<?>> query = searchTarget.query( sessionContext )
				.asProjections(
						searchTarget.projection().field( "string", String.class ).toProjection(),
						searchTarget.projection().distance( "geoPoint", GeoPoint.of( 45.749828, 4.854172 ) ).toProjection()
				)
				.predicate().matchAll().end()
				.sort().byField( "string" ).onMissingValue().sortLast().asc().end()
				.build();
		SearchResult<List<?>> results = query.execute();

		checkResult( results.getHits().get( 0 ), "Chez Margotte", 1, 430d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 1 ), "Imouto", 1, 1300d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 2 ), "L'ourson qui boit", 1, 2730d, Offset.offset( 10d ) );
		checkResult( results.getHits().get( 3 ), null, 1, null, null );
	}

	@Test
	public void distanceProjection_unit() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<List<?>> query = searchTarget.query( sessionContext )
				.asProjections(
						searchTarget.projection().field( "string", String.class ).toProjection(),
						searchTarget.projection()
								.distance( "geoPoint", GeoPoint.of( 45.749828, 4.854172 ) )
								.unit( DistanceUnit.KILOMETERS )
								.toProjection()
				)
				.predicate().matchAll().end()
				.sort().byField( "string" ).onMissingValue().sortLast().asc().end()
				.build();
		SearchResult<List<?>> results = query.execute();

		checkResult( results.getHits().get( 0 ), "Chez Margotte", 1, 0.430d, Offset.offset( 0.010d ) );
		checkResult( results.getHits().get( 1 ), "Imouto", 1, 1.300d, Offset.offset( 0.010d ) );
		checkResult( results.getHits().get( 2 ), "L'ourson qui boit", 1, 2.730d, Offset.offset( 0.010d ) );
		checkResult( results.getHits().get( 3 ), null, 1, null, null );
	}

	@Test
	public void distanceProjection_several() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<List<?>> query = searchTarget.query( sessionContext )
				.asProjections(
						searchTarget.projection().field( "string", String.class ).toProjection(),
						searchTarget.projection()
								.distance( "geoPoint", GeoPoint.of( 45.749828, 4.854172 ) )
								.toProjection(),
						searchTarget.projection()
								.distance( "geoPoint", GeoPoint.of( 45.763363, 4.833527 ) )
								.toProjection(),
						searchTarget.projection()
								.distance( "geoPoint_1", GeoPoint.of( 45.749828, 4.854172 ) )
								.unit( DistanceUnit.KILOMETERS )
								.toProjection()
				)
				.predicate().matchAll().end()
				.sort().byField( "string" ).onMissingValue().sortLast().asc().end()
				.build();
		SearchResult<List<?>> results = query.execute();

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
	public void distanceProjection_invalidType() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Invalid type" );
		thrown.expectMessage( "string" );

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		searchTarget.projection()
				.distance( "string", GeoPoint.of( 43.749828, 1.854172 ) )
				.toProjection();
	}

	@Test
	public void distanceProjection_nullCenter() {
		thrown.expect( IllegalArgumentException.class );
		thrown.expectMessage( "center" );
		thrown.expectMessage( "must not be null" );

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		searchTarget.projection()
				.distance( "geoPoint", null )
				.toProjection();
	}

	@Test
	public void distanceProjection_nullUnit() {
		thrown.expect( IllegalArgumentException.class );
		thrown.expectMessage( "unit" );
		thrown.expectMessage( "must not be null" );

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		searchTarget.projection()
				.distance( "geoPoint", GeoPoint.of( 45.749828, 4.854172 ) ).unit( null )
				.toProjection();
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
