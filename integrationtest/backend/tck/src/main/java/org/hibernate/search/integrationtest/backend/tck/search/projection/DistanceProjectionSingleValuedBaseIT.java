/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableGeoPointWithDistanceFromCenterValues.CENTER_POINT_1;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators.APPROX_M_COMPARATOR;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DistanceProjectionSingleValuedBaseIT extends AbstractDistanceProjectionSingleValuedBaseIT {

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void optional(TestedFieldStructure fieldStructure, DataSet dataSet) {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath( fieldStructure );

		var projection = scope.projection().distance( fieldPath, CENTER_POINT_1 ).optional().toProjection();

		var query = scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( projection )
				.where( f -> f.matchAll() );
		addParameter( query, "center-param", CENTER_POINT_1 );

		assertThatQuery( query.routing( dataSet.routingKey ).toQuery() )
				.hits().asIs()
				.usingElementComparator( TestComparators.optional( APPROX_M_COMPARATOR ) )
				.containsExactlyInAnyOrder(
						Optional.of( dataSet.getFieldDistanceFromCenter1( 1 ) ),
						Optional.of( dataSet.getFieldDistanceFromCenter1( 2 ) ),
						Optional.of( dataSet.getFieldDistanceFromCenter1( 3 ) ),
						Optional.empty() // Empty document
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void set(TestedFieldStructure fieldStructure, DataSet dataSet) {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath( fieldStructure );

		var projection = scope.projection().distance( fieldPath, CENTER_POINT_1 ).set().toProjection();

		var query = scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( projection )
				.where( f -> f.matchAll() );
		addParameter( query, "center-param", CENTER_POINT_1 );

		assertThatQuery( query.routing( dataSet.routingKey ).toQuery() )
				.hits().asIs()
				.usingElementComparator( TestComparators.lexicographic( APPROX_M_COMPARATOR ) )
				.containsExactlyInAnyOrder(
						Set.of( dataSet.getFieldDistanceFromCenter1( 1 ) ),
						Set.of( dataSet.getFieldDistanceFromCenter1( 2 ) ),
						Set.of( dataSet.getFieldDistanceFromCenter1( 3 ) ),
						TckConfiguration.get().getBackendFeatures().accumulatedNullValue( ProjectionCollector.set() ) // Empty document
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void sortedSet(TestedFieldStructure fieldStructure, DataSet dataSet) {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath( fieldStructure );

		Comparator<Double> comparator = Comparator.nullsLast( Comparable::compareTo );
		var projection = scope.projection().distance( fieldPath, CENTER_POINT_1 )
				.sortedSet( comparator ).toProjection();

		var query = scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( projection )
				.where( f -> f.matchAll() );
		addParameter( query, "center-param", CENTER_POINT_1 );

		assertThatQuery( query.routing( dataSet.routingKey ).toQuery() )
				.hits().asIs()
				.usingElementComparator( TestComparators.lexicographic( APPROX_M_COMPARATOR ) )
				.containsExactlyInAnyOrder(
						new TreeSet<>( Set.of( dataSet.getFieldDistanceFromCenter1( 1 ) ) ),
						new TreeSet<>( Set.of( dataSet.getFieldDistanceFromCenter1( 2 ) ) ),
						new TreeSet<>( Set.of( dataSet.getFieldDistanceFromCenter1( 3 ) ) ),
						TckConfiguration.get().getBackendFeatures()
								.accumulatedNullValue( ProjectionCollector.sortedSet( comparator ) ) // Empty document
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void array(TestedFieldStructure fieldStructure, DataSet dataSet) {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath( fieldStructure );

		var projection = scope.projection().distance( fieldPath, CENTER_POINT_1 ).array( Double.class ).toProjection();

		var query = scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( projection )
				.where( f -> f.matchAll() );
		addParameter( query, "center-param", CENTER_POINT_1 );

		assertThatQuery( query.routing( dataSet.routingKey ).toQuery() )
				.hits().asIs()
				.usingElementComparator( TestComparators.array( APPROX_M_COMPARATOR ) )
				.containsExactlyInAnyOrder(
						new Double[] { dataSet.getFieldDistanceFromCenter1( 1 ) },
						new Double[] { dataSet.getFieldDistanceFromCenter1( 2 ) },
						new Double[] { dataSet.getFieldDistanceFromCenter1( 3 ) },
						TckConfiguration.get().getBackendFeatures()
								.accumulatedNullValue( ProjectionCollector.array( Double.class ) ) // Empty document
				);
	}

	@Override
	protected void addParameter(SearchQueryOptionsStep<?, ?, ?, ?, ?, ?> query, String parameterName, Object value) {
		// do nothing
	}

	@Override
	protected DistanceToFieldProjectionValueStep<?, Double> distance(
			SearchProjectionFactory<EntityReference, DocumentReference> projection, String path, GeoPoint center,
			String parameterName) {
		return projection.distance( path, center );
	}

	@Override
	protected ProjectionFinalStep<List<Double>> distanceMulti(
			SearchProjectionFactory<EntityReference, DocumentReference> projection, String path, GeoPoint center,
			String parameterName) {
		return projection.distance( path, center ).list();
	}

	@Override
	protected DistanceToFieldProjectionOptionsStep<?, Double> distance(
			SearchProjectionFactory<EntityReference, DocumentReference> projection, String path, GeoPoint center,
			DistanceUnit unit, String centerParam, String unitParam) {
		return projection.distance( path, center ).unit( unit );
	}

	@Override
	protected SortFinalStep sort(SearchSortFactory sort, String path, GeoPoint center, String parameterName) {
		return sort.distance( path, center );
	}
}
