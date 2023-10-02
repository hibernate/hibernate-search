/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Comparator;
import java.util.List;

import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.search.predicate.AbstractSpatialWithinPredicateIT;
import org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.Test;

import org.assertj.core.api.ListAssert;

class DistanceProjectionComplexCasesIT extends AbstractSpatialWithinPredicateIT {

	private static final Comparator<? super Double> APPROX_M_COMPARATOR = TestComparators.approximateDouble( 10.0 );
	private static final Comparator<? super Double> APPROX_KM_COMPARATOR = TestComparators.approximateDouble( 0.010 );

	/**
	 * See also {@code org.hibernate.search.integrationtest.backend.tck.search.projection.DistanceProjectionSingleValuedBaseIT#several()}.
	 * <p>
	 * The main difference is that we're targeting multiple fields here.
	 */
	@Test
	void several() {
		StubMappingScope scope = mainIndex.createScope();

		ListAssert<List<?>> hitsAssert = assertThatQuery( scope.query()
				.select( f -> f.composite(
						f.distance( "geoPoint", GeoPoint.of( 45.749828, 4.854172 ) ),
						f.distance( "geoPoint", GeoPoint.of( 45.763363, 4.833527 ) ),
						f.distance( "geoPoint_1", GeoPoint.of( 45.749828, 4.854172 ) )
								.unit( DistanceUnit.KILOMETERS )
				) )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "string" ).missing().last().asc() )
				.toQuery() )
				.hits().asIs();

		hitsAssert.extracting( tuple -> (Double) tuple.get( 0 ) )
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						430d,
						1300d,
						2730d,
						null
				);
		hitsAssert.extracting( tuple -> (Double) tuple.get( 1 ) )
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						1780d,
						1095d,
						812d,
						null
				);
		hitsAssert.extracting( tuple -> (Double) tuple.get( 2 ) )
				.usingElementComparator( APPROX_KM_COMPARATOR )
				.containsExactlyInAnyOrder(
						135.834,
						136.294,
						134.967,
						null
				);
	}

	/**
	 * See also {@code org.hibernate.search.integrationtest.backend.tck.search.projection.DistanceProjectionSingleValuedBaseIT#sortable_withSort()}.
	 * <p>
	 * The main difference is that we're composing multiple sorts here.
	 */
	@Test
	void withDistanceSort() {
		StubMappingScope scope = mainIndex.createScope();

		GeoPoint center = GeoPoint.of( 45.749828, 4.854172 );

		assertThatQuery( scope.query()
				.select( f -> f.distance( "geoPoint", center ) )
				.where( f -> f.matchAll() )
				.sort( f -> f
						.distance( "geoPoint", GeoPoint.of( 43.749828, 1.854172 ) ).then()
						.distance( "geoPoint", center ) )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactly(
						1300d,
						430d,
						2730d,
						null
				);
	}

	/**
	 * Test that projections will work even with very long field names.
	 * <p>
	 * This is relevant for Elasticsearch, which generates a name for computed values based on the field name.
	 */
	@Test
	void longFieldName() {
		StubMappingScope scope = mainIndex.createScope();

		assertThatQuery( scope.query()
				.select( f -> f.distance(
						"geoPoint_with_a_veeeeeeeeeeeeeeeeeeeeerrrrrrrrrrrrrrrrrryyyyyyyyyyyyyyyy_long_name",
						GeoPoint.of( 45.74982800099999888371, 4.85417200099999888371 )
				)
				)
				.where( f -> f.matchAll() )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						430d,
						1300d,
						2730d,
						null
				);
	}
}
