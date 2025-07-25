/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.normalize;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatResult;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.CompositeAggregationFrom1AsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.AggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.TermsAggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests behavior specific to the terms aggregation on supported field types.
 * <p>
 * Behavior common to all single-field aggregations is tested in {@link SingleFieldAggregationBaseIT}.
 */

class TermsAggregationSpecificsIT<F> {

	private static final String AGGREGATION_NAME = "aggregationName";

	private static final Set<StandardFieldTypeDescriptor<?>> supportedFieldTypes = new LinkedHashSet<>();
	private static final List<DataSet<?>> dataSets = new ArrayList<>();
	private static final List<Arguments> parameters = new ArrayList<>();

	static {
		AggregationDescriptor aggregationDescriptor = TermsAggregationDescriptor.INSTANCE;
		for ( StandardFieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAllStandard() ) {
			if ( aggregationDescriptor.getSingleFieldAggregationExpectations( fieldType ).isSupported() ) {
				supportedFieldTypes.add( fieldType );
				DataSet<?> dataSet = new DataSet<>( fieldType );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( fieldType, dataSet ) );
			}
		}
	}

	public static List<? extends Arguments> params() {
		return parameters;
	}

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		for ( DataSet<?> dataSet : dataSets ) {
			dataSet.init();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void superClassFieldType(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		Class<? super F> superClass = fieldType.getJavaType().getSuperclass();

		doTestSuperClassFieldType( superClass, fieldType, dataSet );
	}

	private <S> void doTestSuperClassFieldType(Class<S> superClass, FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<S, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, superClass ) )
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// All documents should be mentioned in the aggregation, even those excluded by the limit/offset
						containsInAnyOrder( c -> {
							dataSet.documentIdPerTerm.forEach( (key, value) -> c.accept( key, (long) value.size() ) );
						}, fieldType )
				);
	}

	/**
	 * Check that defining a predicate will affect the aggregation result.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void predicate(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		Map.Entry<F, List<String>> firstTermEntry = dataSet.documentIdPerTerm.entrySet().iterator().next();

		assertThatQuery(
				index.createScope().query()
						.where( f -> f.id()
								.matching( firstTermEntry.getValue().get( 0 ) )
								.matching( firstTermEntry.getValue().get( 1 ) )
						)
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() ) )
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// Only document 0 should be taken into account by the aggregation
						containsInAnyOrder( c -> {
							c.accept( firstTermEntry.getKey(), 2L );
						}, fieldType )
				);
	}

	/**
	 * Check that defining a limit and offset will <strong>not</strong> affect the aggregation result.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void limitAndOffset(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatResult(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() ) )
						.fetch( 3, 4 )
		)
				.aggregation(
						aggregationKey,
						// All documents should be mentioned in the aggregation, even those excluded by the limit/offset
						containsInAnyOrder( c -> {
							dataSet.documentIdPerTerm.forEach( (key, value) -> c.accept( key, (long) value.size() ) );
						}, fieldType )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testDefaultSortOrderIsCount")
	void order_default(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() ) )
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with decreasing term count
						containsExactly( c -> {
							for ( F value : dataSet.valuesInDescendingDocumentCountOrder ) {
								c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
							}
						}, fieldType )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testCountSortOrderDesc")
	void orderByCountDescending(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.orderByCountDescending()
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with decreasing term count
						containsExactly( c -> {
							for ( F value : dataSet.valuesInDescendingDocumentCountOrder ) {
								c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
							}
						}, fieldType )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testCountSortOrderAsc")
	void orderByCountAscending(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.orderByCountAscending()
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with increasing term count
						containsExactly( c -> {
							for ( F value : dataSet.valuesInAscendingDocumentCountOrder ) {
								c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
							}
						}, fieldType )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void orderByTermDescending(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.orderByTermDescending()
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with decreasing term dataSet.values
						containsExactly( c -> {
							for ( F value : dataSet.valuesInDescendingOrder ) {
								c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
							}
						}, fieldType )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testAlphabeticalSortOrder")
	void orderByTermAscending(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.orderByTermAscending()
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// The result should present buckets with increasing term dataSet.values
						containsExactly( c -> {
							for ( F value : dataSet.valuesInAscendingOrder ) {
								c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
							}
						}, fieldType )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testZeroCountsExcluded")
	void minDocumentCount_positive(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.minDocumentCount( 2 )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						// Only buckets with the minimum required document count should appear in the result
						containsInAnyOrder( c -> {
							dataSet.documentIdPerTerm.forEach( (key, value) -> {
								int documentCount = value.size();
								if ( documentCount >= 2 ) {
									c.accept( key, (long) documentCount );
								}
							} );
						}, fieldType )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testZeroCountsIncluded")
	void minDocumentCount_zero(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		Map.Entry<F, List<String>> firstTermEntry = dataSet.documentIdPerTerm.entrySet().iterator().next();

		assertThatQuery(
				index.createScope().query()
						// Exclude documents containing the first term from matches
						.where( f -> f.matchAll().except(
								f.id().matchingAny( firstTermEntry.getValue() )
						) )
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.minDocumentCount( 0 )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * Buckets with a count of 0 should appear for dataSet.values that are in the index,
						 * but are not encountered in any matching document.
						 */
						containsInAnyOrder( c -> {
							dataSet.documentIdPerTerm.entrySet().stream().skip( 1 ).forEach( e -> {
								c.accept( e.getKey(), (long) e.getValue().size() );
							} );
							c.accept( firstTermEntry.getKey(), 0L );
						}, fieldType )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void minDocumentCount_zero_noMatch(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				index.createScope().query()
						// Exclude all documents from the matches
						.where( f -> f.id().matching( "none" ) )
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.minDocumentCount( 0 )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * All indexed terms should appear in a bucket, in ascending value order, with a count of zero.
						 */
						containsInAnyOrder( c -> {
							for ( F value : dataSet.valuesInAscendingOrder ) {
								c.accept( value, 0L );
							}
						}, fieldType )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void minDocumentCount_zero_noMatch_orderByTermDescending(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				index.createScope().query()
						// Exclude all documents from the matches
						.where( f -> f.id().matching( "none" ) )
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.minDocumentCount( 0 )
								.orderByTermDescending()
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * All indexed terms should appear in a bucket, in descending value order, with a count of zero.
						 */
						containsInAnyOrder( c -> {
							for ( F value : dataSet.valuesInDescendingOrder ) {
								c.accept( value, 0L );
							}
						}, fieldType )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void minDocumentCount_negative(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> index.createScope().aggregation().terms().field( fieldPath, fieldType.getJavaType() )
				.minDocumentCount( -1 ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'minDocumentCount'" )
				.hasMessageContaining( "must be positive or zero" );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-776")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testMaxFacetCounts")
	void maxTermCount_positive(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.maxTermCount( 1 )
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * Only the bucket with the most documents should be returned.
						 */
						containsInAnyOrder( c -> {
							F valueWithMostDocuments = dataSet.valuesInDescendingDocumentCountOrder.get( 0 );
							c.accept( valueWithMostDocuments,
									(long) dataSet.documentIdPerTerm.get( valueWithMostDocuments ).size() );
						}, fieldType )
				);
	}

	/**
	 * Test maxTermCount with a non-default sort by ascending term value.
	 * The returned terms should be the "lowest" dataSet.values.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void maxTermCount_positive_orderByTermAscending(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.maxTermCount( 1 )
								.orderByTermAscending()
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * Only the bucket with the "lowest" value should be returned.
						 */
						containsInAnyOrder( c -> {
							F lowestValue = dataSet.valuesInAscendingOrder.get( 0 );
							c.accept( lowestValue, (long) dataSet.documentIdPerTerm.get( lowestValue ).size() );
						}, fieldType )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void maxTermCount_positive_orderByCountAscending(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery(
				matchAllQuery()
						.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.maxTermCount( 1 )
								.orderByCountAscending()
						)
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						/*
						 * Only the bucket with the least documents should be returned.
						 */
						containsInAnyOrder( c -> {
							F valueWithLeastDocuments = dataSet.valuesInAscendingDocumentCountOrder.get( 0 );
							c.accept( valueWithLeastDocuments,
									(long) dataSet.documentIdPerTerm.get( valueWithLeastDocuments ).size() );
						}, fieldType )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void maxTermCount_zero(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> index.createScope().aggregation().terms().field( fieldPath, fieldType.getJavaType() )
				.maxTermCount( 0 ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'maxTermCount'" )
				.hasMessageContaining( "must be strictly positive" );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void maxTermCount_negative(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> index.createScope().aggregation().terms().field( fieldPath, fieldType.getJavaType() )
				.maxTermCount( -1 ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'maxTermCount'" )
				.hasMessageContaining( "must be strictly positive" );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4544")
	void maxTermCount_integerMaxValue(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery( matchAllQuery()
				.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
						.maxTermCount( Integer.MAX_VALUE ) )
				.routing( dataSet.name ) )
				.aggregation(
						aggregationKey,
						// All buckets should be returned.
						containsInAnyOrder( c -> {
							for ( F value : dataSet.valuesInDescendingOrder ) {
								c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
							}
						}, fieldType )
				);
	}

	// This is interesting even if we already test Integer.MAX_VALUE (see above),
	// because Lucene has some hardcoded limits for PriorityQueue sizes,
	// somewhere around 2147483631, which is lower than Integer.MAX_VALUE.
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4544")
	void maxTermCount_veryLarge(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery( matchAllQuery()
				.aggregation( aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
						.maxTermCount( 2_000_000_000 ) )
				.routing( dataSet.name ) )
				.aggregation(
						aggregationKey,
						// All buckets should be returned.
						containsInAnyOrder( c -> {
							for ( F value : dataSet.valuesInDescendingOrder ) {
								c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
							}
						}, fieldType )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void terms_explicitDocCount(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, Long>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery( matchAllQuery()
				.aggregation(
						aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								.value( f.countDocuments() )
				)
				.routing( dataSet.name ) )
				.aggregation(
						aggregationKey,
						// All buckets should be returned.
						containsInAnyOrder(
								c -> {
									for ( F value : dataSet.valuesInDescendingOrder ) {
										c.accept( value, (long) dataSet.documentIdPerTerm.get( value ).size() );
									}
								}, fieldType
						)
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void terms_min(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		assumeTrue( fieldType.supportsMetricAggregation(),
				"Since the value is a metric aggregation on the same field, we want to be sure that only those fields that support it are included." );
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, F>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery( matchAllQuery()
				.aggregation(
						aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								// while maybe silly as min/max == the same term as the key it is here just to test the nesting and aggregations:
								.value( (AggregationFinalStep<F>) f.min().field( fieldPath, fieldType.getJavaType() ) )
				)
				.routing( dataSet.name ) )
				.aggregation(
						aggregationKey,
						// All buckets should be returned.
						containsInAnyOrder(
								c -> {
									for ( F value : dataSet.valuesInDescendingOrder ) {
										c.accept( value, fieldType.normalize( value ) );
									}
								}, fieldType
						)
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void terms_max(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		assumeTrue( fieldType.supportsMetricAggregation(),
				"Since the value is a metric aggregation on the same field, we want to be sure that only those fields that support it are included." );
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, F>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery( matchAllQuery()
				.aggregation(
						aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								// while maybe silly as min/max == the same term as the key it is here just to test the nesting and aggregations:
								.value( (AggregationFinalStep<F>) f.max().field( fieldPath, fieldType.getJavaType() ) )
				)
				.routing( dataSet.name ) )
				.aggregation(
						aggregationKey,
						// All buckets should be returned.
						containsInAnyOrder(
								c -> {
									for ( F value : dataSet.valuesInDescendingOrder ) {
										c.accept( value, fieldType.normalize( value ) );
									}
								}, fieldType
						)
				);
	}

	@SuppressWarnings("unchecked") // for the eclipse compiler
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void terms_composite(FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		assumeTrue( fieldType.supportsMetricAggregation(),
				"Since the value is a metric aggregation on the same field, we want to be sure that only those fields that support it are included." );
		String fieldPath = index.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationKey<Map<F, F>> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		assertThatQuery( matchAllQuery()
				.aggregation(
						aggregationKey, f -> f.terms().field( fieldPath, fieldType.getJavaType() )
								// while maybe silly as min/max == the same term as the key it is here just to test the nesting and aggregations:
								.value( ( (CompositeAggregationFrom1AsStep<F>) f.composite() // cast here is for the eclipse compiler ...
										.from( f.max().field( fieldPath, fieldType.getJavaType() ) ) )
										.as( Function.<F>identity() ) )
				)
				.routing( dataSet.name ) )
				.aggregation(
						aggregationKey,
						// All buckets should be returned.
						containsInAnyOrder(
								c -> {
									for ( F value : dataSet.valuesInDescendingOrder ) {
										c.accept( value, fieldType.normalize( value ) );
									}
								}, fieldType
						)
				);
	}

	private SearchQueryOptionsStep<Object, ?, DocumentReference, ?, ?, ?> matchAllQuery() {
		return index.createScope().query().where( f -> f.matchAll() );
	}

	@SuppressWarnings("unchecked")
	private <K, V> Consumer<Map<F, V>> containsExactly(Consumer<BiConsumer<F, V>> expectationBuilder,
			FieldTypeDescriptor<F, ?> fieldType) {
		List<Map.Entry<F, V>> expected = new ArrayList<>();
		expectationBuilder.accept( (k, v) -> expected.add( entry( fieldType.toExpectedDocValue( k ), v ) ) );
		return actual -> assertThat( normalize( actual ) )
				.containsExactly( normalize( expected ).toArray( new Map.Entry[0] ) );
	}

	@SuppressWarnings("unchecked")
	private <K, V> Consumer<Map<K, V>> containsInAnyOrder(Consumer<BiConsumer<F, V>> expectationBuilder,
			FieldTypeDescriptor<F, ?> fieldType) {
		List<Map.Entry<F, V>> expected = new ArrayList<>();
		expectationBuilder.accept( (k, v) -> expected.add( entry( fieldType.toExpectedDocValue( k ), v ) ) );
		return actual -> assertThat( normalize( actual ).entrySet() )
				.containsExactlyInAnyOrder( normalize( expected ).toArray( new Map.Entry[0] ) );
	}

	private static class DataSet<F> {
		final FieldTypeDescriptor<F, ?> fieldType;
		final String name;
		final Map<F, List<String>> documentIdPerTerm;
		final List<F> valuesInAscendingOrder;
		final List<F> valuesInDescendingOrder;
		final List<F> valuesInAscendingDocumentCountOrder;
		final List<F> valuesInDescendingDocumentCountOrder;

		private DataSet(FieldTypeDescriptor<F, ?> fieldType) {
			this.fieldType = fieldType;
			this.name = fieldType.getUniqueName();
			this.documentIdPerTerm = new LinkedHashMap<>();

			this.valuesInAscendingOrder = fieldType.getAscendingUniqueTermValues().getSingle();

			this.valuesInDescendingOrder = new ArrayList<>( valuesInAscendingOrder );
			Collections.reverse( valuesInDescendingOrder );

			this.valuesInDescendingDocumentCountOrder = new ArrayList<>( valuesInAscendingOrder );
			/*
			 * Mess with the value order, because some tests would be pointless
			 * if the document count order was the same as (or the opposite of) the value order
			 */
			valuesInDescendingDocumentCountOrder.add( valuesInDescendingDocumentCountOrder.get( 0 ) );
			valuesInDescendingDocumentCountOrder.remove( 0 );
			valuesInDescendingDocumentCountOrder.add( valuesInDescendingDocumentCountOrder.get( 0 ) );
			valuesInDescendingDocumentCountOrder.remove( 0 );

			this.valuesInAscendingDocumentCountOrder = new ArrayList<>( valuesInDescendingDocumentCountOrder );
			Collections.reverse( valuesInAscendingDocumentCountOrder );

			// Simple dataset: strictly decreasing number of documents for each term
			int documentIdAsInteger = 0;
			int numberOfDocuments = valuesInDescendingDocumentCountOrder.size();
			for ( F value : valuesInDescendingDocumentCountOrder ) {
				ArrayList<String> documentIdsForTerm = new ArrayList<>();
				documentIdPerTerm.put( value, documentIdsForTerm );
				for ( int i = 0; i < numberOfDocuments; i++ ) {
					String documentId = name + "_document_" + documentIdAsInteger;
					++documentIdAsInteger;
					documentIdsForTerm.add( documentId );
				}
				--numberOfDocuments;
			}
		}

		private void init() {
			BulkIndexer indexer = index.bulkIndexer();
			for ( Map.Entry<F, List<String>> entry : documentIdPerTerm.entrySet() ) {
				F value = entry.getKey();
				for ( String documentId : entry.getValue() ) {
					indexer.add( documentId, name, document -> {
						document.addValue( index.binding().fieldModels.get( fieldType ).reference, value );
						document.addValue( index.binding().fieldWithConverterModels.get( fieldType ).reference, value );
					} );
				}
			}
			indexer.add( name + "_document_empty", name, document -> {} );
			indexer.join();
		}

	}

	private static class IndexBinding {
		final SimpleFieldModelsByType fieldModels;
		final SimpleFieldModelsByType fieldWithConverterModels;
		final SimpleFieldModelsByType fieldWithAggregationDisabledModels;

		IndexBinding(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"", c -> c.aggregable( Aggregable.YES )
							.searchable( Searchable.NO ) // Terms aggregations should not need this
			);
			fieldWithConverterModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"converted_", c -> c.aggregable( Aggregable.YES )
							.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() )
							.projectionConverter( ValueWrapper.class, ValueWrapper.fromDocumentValueConverter() )
			);
			fieldWithAggregationDisabledModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"nonAggregable_", c -> c.aggregable( Aggregable.NO )
			);
		}
	}

}
