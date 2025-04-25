/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableGeoPointWithDistanceFromCenterValues.CENTER_POINT_1;
import static org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableGeoPointWithDistanceFromCenterValues.CENTER_POINT_2;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueDistanceFromCenterValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableGeoPointWithDistanceFromCenterValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.data.Pair;
import org.hibernate.search.util.impl.test.data.Triplet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.assertj.core.api.ListAssert;

/**
 * Tests basic behavior of projections on the distance between a given point and the value of a multi-valued field.
 */
@TestForIssue(jiraKey = "HSEARCH-3391")
abstract class AbstractDistanceProjectionMultiValuedBaseIT {

	private static final GeoPointFieldTypeDescriptor fieldType = GeoPointFieldTypeDescriptor.INSTANCE;
	private static final Set<StandardFieldTypeDescriptor<GeoPoint>> supportedFieldTypes = Collections.singleton( fieldType );
	private static final List<DataSet> dataSets = new ArrayList<>();
	private static final List<Arguments> parameters = new ArrayList<>();
	private static final Comparator<Iterable<Double>> APPROX_M_COMPARATOR =
			TestComparators.lexicographic( TestComparators.APPROX_M_COMPARATOR );
	private static final Comparator<Iterable<Double>> APPROX_KM_COMPARATOR =
			TestComparators.lexicographic( TestComparators.APPROX_KM_COMPARATOR );
	private static final Comparator<Iterable<Double>> APPROX_MILES_COMPARATOR =
			TestComparators.lexicographic( TestComparators.APPROX_MILES_COMPARATOR );

	static {
		for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
			if ( fieldStructure.isSingleValued() ) {
				continue;
			}
			DataSet dataSet = new DataSet( fieldStructure );
			dataSets.add( dataSet );
			parameters.add( Arguments.of( fieldStructure, dataSet ) );
		}
	}

	public static List<? extends Arguments> params() {
		return parameters;
	}

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<SingleFieldIndexBinding> mainIndex =
			SimpleMappedIndex.of(
					root -> SingleFieldIndexBinding.create(
							root,
							supportedFieldTypes,
							TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault()
									? c -> {}
									: c -> c.projectable( Projectable.YES )
					)
			)
					.name( "main" );
	private static final SimpleMappedIndex<SingleFieldIndexBinding> sortableIndex =
			SimpleMappedIndex.of(
					root -> SingleFieldIndexBinding.create(
							root,
							supportedFieldTypes,
							TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault()
									? c -> {}
									: c -> c.projectable( Projectable.YES ).sortable( Sortable.YES )
					)
			)
					.name( "sortable" );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndexes( mainIndex, sortableIndex ).setup();

		BulkIndexer mainIndexer = mainIndex.bulkIndexer();
		BulkIndexer sortableIndexer = sortableIndex.bulkIndexer();
		for ( DataSet dataSet : dataSets ) {
			dataSet.contribute( mainIndexer, sortableIndexer );
		}
		mainIndexer.join( sortableIndexer );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void simple(TestedFieldStructure fieldStructure, DataSet dataSet) {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath( fieldStructure );

		var projection = distance( scope.projection(), fieldPath, CENTER_POINT_1, "center-param" ).toProjection();

		var query = scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( projection )
				.where( f -> f.matchAll() );
		addParameter( query, "center-param", CENTER_POINT_1 );

		assertThatQuery( query
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistancesFromCenter1( 1 ),
						dataSet.getFieldDistancesFromCenter1( 2 ),
						dataSet.getFieldDistancesFromCenter1( 3 ),
						Collections.emptyList() // Empty document
				);
	}

	/**
	 * Test that mentioning the same projection twice works as expected.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void duplicated(TestedFieldStructure fieldStructure, DataSet dataSet) {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath( fieldStructure );

		var projectionFactory = scope.projection();
		var projection = projectionFactory.composite()
				.from(
						distance( projectionFactory, fieldPath, CENTER_POINT_1, "center-param" ),
						distance( projectionFactory, fieldPath, CENTER_POINT_1, "center-param" )
				)
				.as( Pair::new ).toProjection();

		var query = scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( projection )
				.where( f -> f.matchAll() );
		addParameter( query, "center-param", CENTER_POINT_1 );

		ListAssert<Pair<List<Double>, List<Double>>> hitsAssert = assertThatQuery( query
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs();

		hitsAssert.extracting( Pair::elem0 )
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistancesFromCenter1( 1 ),
						dataSet.getFieldDistancesFromCenter1( 2 ),
						dataSet.getFieldDistancesFromCenter1( 3 ),
						Collections.emptyList() // Empty document
				);
		hitsAssert.extracting( Pair::elem1 )
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistancesFromCenter1( 1 ),
						dataSet.getFieldDistancesFromCenter1( 2 ),
						dataSet.getFieldDistancesFromCenter1( 3 ),
						Collections.emptyList() // Empty document
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3618")
	void sortable_withoutSort(TestedFieldStructure fieldStructure, DataSet dataSet) {
		StubMappingScope scope = sortableIndex.createScope();

		String fieldPath = sortableIndex.binding().getFieldPath( fieldStructure, fieldType );

		var projection =
				distance( scope.projection(), fieldPath, AscendingUniqueDistanceFromCenterValues.CENTER_POINT, "center-param" )
						.toProjection();

		var query = scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( projection )
				.where( f -> f.matchAll() );
		addParameter( query, "center-param", AscendingUniqueDistanceFromCenterValues.CENTER_POINT );

		assertThatQuery( query
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getSortableFieldDistancesFromCenter( 1 ),
						dataSet.getSortableFieldDistancesFromCenter( 2 ),
						dataSet.getSortableFieldDistancesFromCenter( 3 )
				);
	}

	/**
	 * This is relevant because projections on the distance can
	 * be optimized when also sorting by distance to the same point.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3618")
	void sortable_withSort(TestedFieldStructure fieldStructure, DataSet dataSet) {
		StubMappingScope scope = sortableIndex.createScope();

		String fieldPath = sortableIndex.binding().getFieldPath( fieldStructure, fieldType );

		var projection =
				distance( scope.projection(), fieldPath, AscendingUniqueDistanceFromCenterValues.CENTER_POINT, "center-param" )
						.toProjection();

		var query = scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( projection )
				.where( f -> f.matchAll() );
		addParameter( query, "center-param", AscendingUniqueDistanceFromCenterValues.CENTER_POINT );

		assertThatQuery( query
				.sort( sort( scope.sort(), fieldPath, AscendingUniqueDistanceFromCenterValues.CENTER_POINT, "center-param" )
						.toSort() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactly( // in this order
						dataSet.getSortableFieldDistancesFromCenter( 1 ),
						dataSet.getSortableFieldDistancesFromCenter( 2 ),
						dataSet.getSortableFieldDistancesFromCenter( 3 )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void unit_km(TestedFieldStructure fieldStructure, DataSet dataSet) {
		StubMappingScope scope = mainIndex.createScope();

		var projection =
				distance( scope.projection(), getFieldPath( fieldStructure ), CENTER_POINT_1, DistanceUnit.KILOMETERS,
						"center-param", "unit-param" )
						.toProjection();

		var query = scope.query()
				.select( projection )
				.where( f -> f.matchAll() );
		addParameter( query, "center-param", CENTER_POINT_1 );
		addParameter( query, "unit-param", DistanceUnit.KILOMETERS );

		assertThatQuery( query
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_KM_COMPARATOR )
				.containsExactlyInAnyOrder(
						divideAll( dataSet.getFieldDistancesFromCenter1( 1 ), 1000 ),
						divideAll( dataSet.getFieldDistancesFromCenter1( 2 ), 1000 ),
						divideAll( dataSet.getFieldDistancesFromCenter1( 3 ), 1000 ),
						Collections.emptyList() // Empty document
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void unit_miles(TestedFieldStructure fieldStructure, DataSet dataSet) {
		StubMappingScope scope = mainIndex.createScope();

		var projection =
				distance( scope.projection(), getFieldPath( fieldStructure ), CENTER_POINT_1, DistanceUnit.MILES,
						"center-param", "unit-param" ).toProjection();

		var query = scope.query()
				.select( projection )
				.where( f -> f.matchAll() );
		addParameter( query, "center-param", CENTER_POINT_1 );
		addParameter( query, "unit-param", DistanceUnit.MILES );

		assertThatQuery( query
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_MILES_COMPARATOR )
				.containsExactlyInAnyOrder(
						divideAll( dataSet.getFieldDistancesFromCenter1( 1 ), 1_609.344 ),
						divideAll( dataSet.getFieldDistancesFromCenter1( 2 ), 1_609.344 ),
						divideAll( dataSet.getFieldDistancesFromCenter1( 3 ), 1_609.344 ),
						Collections.emptyList() // Empty document
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void several(TestedFieldStructure fieldStructure, DataSet dataSet) {
		StubMappingScope scope = mainIndex.createScope();

		var projectionFactory = scope.projection();
		var projection = projectionFactory.composite()
				.from(
						distance( projectionFactory, getFieldPath( fieldStructure ), CENTER_POINT_1, "center-param-1" ),
						distance( projectionFactory, getFieldPath( fieldStructure ), CENTER_POINT_2, "center-param-2" ),
						distance( projectionFactory, getFieldPath( fieldStructure ), CENTER_POINT_1, DistanceUnit.KILOMETERS,
								"center-param-1", "unit-param-1" )
				)
				.as( Triplet::new )
				.toProjection();

		var query = scope.query()
				.select( projection )
				.where( f -> f.matchAll() );
		addParameter( query, "center-param-1", CENTER_POINT_1 );
		addParameter( query, "center-param-2", CENTER_POINT_2 );
		addParameter( query, "unit-param-1", DistanceUnit.KILOMETERS );


		ListAssert<Triplet<List<Double>, List<Double>, List<Double>>> hitsAssert = assertThatQuery( query
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs();

		hitsAssert.extracting( Triplet::elem0 )
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistancesFromCenter1( 1 ),
						dataSet.getFieldDistancesFromCenter1( 2 ),
						dataSet.getFieldDistancesFromCenter1( 3 ),
						Collections.emptyList() // Empty document
				);
		hitsAssert.extracting( Triplet::elem1 )
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistancesFromCenter2( 1 ),
						dataSet.getFieldDistancesFromCenter2( 2 ),
						dataSet.getFieldDistancesFromCenter2( 3 ),
						Collections.emptyList() // Empty document
				);
		hitsAssert.extracting( Triplet::elem2 )
				.usingElementComparator( APPROX_KM_COMPARATOR )
				.containsExactlyInAnyOrder(
						divideAll( dataSet.getFieldDistancesFromCenter1( 1 ), 1000 ),
						divideAll( dataSet.getFieldDistancesFromCenter1( 2 ), 1000 ),
						divideAll( dataSet.getFieldDistancesFromCenter1( 3 ), 1000 ),
						Collections.emptyList() // Empty document
				);
	}

	private String getFieldPath(TestedFieldStructure fieldStructure) {
		return mainIndex.binding().getFieldPath( fieldStructure, fieldType );
	}

	private static List<Double> divideAll(List<Double> distances, double denominator) {
		return distances.stream().map( v -> v / denominator ).collect( Collectors.toList() );
	}

	protected abstract void addParameter(SearchQueryOptionsStep<?, ?, ?, ?, ?> query, String parameterName, Object value);

	protected abstract ProjectionFinalStep<List<Double>> distance(
			SearchProjectionFactory<EntityReference, DocumentReference> projection, String path, GeoPoint center,
			String parameterName);

	protected abstract ProjectionFinalStep<List<Double>> distance(
			SearchProjectionFactory<EntityReference, DocumentReference> projection, String path, GeoPoint center,
			DistanceUnit unit, String centerParam, String unitParam);

	protected abstract SortFinalStep sort(SearchSortFactory sort, String path, GeoPoint center,
			String parameterName);

	private static class DataSet {
		private final TestedFieldStructure fieldStructure;
		private final String routingKey;

		private DataSet(TestedFieldStructure fieldStructure) {
			this.fieldStructure = fieldStructure;
			this.routingKey = fieldStructure.getUniqueName();
		}

		private String docId(int docNumber) {
			return routingKey + "_doc_" + docNumber;
		}

		private String emptyDocId(int docNumber) {
			return routingKey + "_emptyDoc_" + docNumber;
		}

		private void contribute(BulkIndexer mainIndexer, BulkIndexer sortableIndexer) {
			mainIndexer.add( documentProvider( emptyDocId( 1 ), routingKey,
					document -> mainIndex.binding().initMultiValued( fieldType, fieldStructure.location,
							document, Collections.emptyList() ) ) );
			mainIndexer.add( documentProvider( docId( 1 ), routingKey,
					document -> mainIndex.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getFieldValues( 1 ) ) ) );
			mainIndexer.add( documentProvider( docId( 2 ), routingKey,
					document -> mainIndex.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getFieldValues( 2 ) ) ) );
			mainIndexer.add( documentProvider( docId( 3 ), routingKey,
					document -> mainIndex.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getFieldValues( 3 ) ) ) );

			sortableIndexer.add( documentProvider( docId( 2 ), routingKey,
					document -> sortableIndex.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getSortableFieldValues( 2 ) ) ) );
			sortableIndexer.add( documentProvider( docId( 1 ), routingKey,
					document -> sortableIndex.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getSortableFieldValues( 1 ) ) ) );
			sortableIndexer.add( documentProvider( docId( 3 ), routingKey,
					document -> sortableIndex.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getSortableFieldValues( 3 ) ) ) );
		}

		private List<GeoPoint> getFieldValues(int documentNumber) {
			return IndexableGeoPointWithDistanceFromCenterValues.INSTANCE.getMulti().get( documentNumber - 1 );
		}

		private List<GeoPoint> getSortableFieldValues(int documentNumber) {
			return AscendingUniqueDistanceFromCenterValues.INSTANCE.getMultiResultingInSingle( SortMode.MIN )
					.get( documentNumber - 1 );
		}

		private List<Double> getFieldDistancesFromCenter1(int documentNumber) {
			return IndexableGeoPointWithDistanceFromCenterValues.INSTANCE.getMultiDistancesFromCenterPoint1()
					.get( documentNumber - 1 );
		}

		private List<Double> getFieldDistancesFromCenter2(int documentNumber) {
			return IndexableGeoPointWithDistanceFromCenterValues.INSTANCE.getMultiDistancesFromCenterPoint2()
					.get( documentNumber - 1 );
		}

		private List<Double> getSortableFieldDistancesFromCenter(int documentNumber) {
			return AscendingUniqueDistanceFromCenterValues.INSTANCE.getMultiDistancesFromCenterPointForMinDataset()
					.get( documentNumber - 1 );
		}
	}
}
