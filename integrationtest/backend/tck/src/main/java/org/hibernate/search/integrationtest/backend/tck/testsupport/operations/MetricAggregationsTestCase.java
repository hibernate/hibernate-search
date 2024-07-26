/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.operations;

import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.MetricAggregationsValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldLocation;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldValueCardinality;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubLoadingOptionsStep;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

/**
 * Denotes a metric aggregations test case for a particular {@link FieldTypeDescriptor}.
 */
public class MetricAggregationsTestCase<F> {

	private final FieldTypeDescriptor<F, ?> typeDescriptor;
	private final MetricAggregationsValues<F> metricAggregationsValues;

	public MetricAggregationsTestCase(FieldTypeDescriptor<F, ?> typeDescriptor) {
		this.typeDescriptor = typeDescriptor;
		metricAggregationsValues = typeDescriptor.metricAggregationsValues();
	}

	public int contribute(BulkIndexer indexer, SingleFieldIndexBinding binding) {
		int i = 0;
		for ( F value : metricAggregationsValues.values() ) {
			String uniqueName = typeDescriptor.getUniqueName();
			String keyA = String.format( Locale.ROOT, "%03d_ROOT_%s", ++i, uniqueName );
			String keyB = String.format( Locale.ROOT, "%03d_NEST_%s", i, uniqueName );
			String keyC = String.format( Locale.ROOT, "%03d_FLAT_%s", i, uniqueName );
			indexer.add( documentProvider( keyA, document -> binding.initSingleValued( typeDescriptor,
					IndexFieldLocation.ROOT, document, value ) ) );
			indexer.add( documentProvider( keyB, document -> binding.initSingleValued(
					typeDescriptor, IndexFieldLocation.IN_NESTED, document, value ) ) );
			indexer.add( documentProvider( keyC, document -> binding.initSingleValued(
					typeDescriptor, IndexFieldLocation.IN_FLATTENED, document, value ) ) );
		}
		return metricAggregationsValues.values().size() * 3;
	}

	public Result<F> testMetricsAggregation(StubMappingScope scope, SingleFieldIndexBinding binding) {
		InternalResult<F> result = new InternalResult<>();
		String fieldPath = binding.getFieldPath( TestedFieldStructure.of(
				IndexFieldLocation.ROOT, IndexFieldValueCardinality.SINGLE_VALUED ), typeDescriptor );

		SearchQueryOptionsStep<?, DocumentReference, StubLoadingOptionsStep, ?, ?> step = scope.query().where(
				SearchPredicateFactory::matchAll )
				.aggregation( result.minKey, f -> f.min().field( fieldPath, typeDescriptor.getJavaType() ) )
				.aggregation( result.maxKey, f -> f.max().field( fieldPath, typeDescriptor.getJavaType() ) )
				.aggregation( result.countKey, f -> f.count().field( fieldPath ) )
				.aggregation( result.countDistinctKey, f -> f.countDistinct().field( fieldPath ) )
				.aggregation( result.avgKey, f -> f.avg().field( fieldPath, typeDescriptor.getJavaType() ) );

		if ( metricAggregationsValues.sum() != null ) {
			step.aggregation( result.sumKey, f -> f.sum().field( fieldPath, typeDescriptor.getJavaType() ) );
		}

		SearchQuery<DocumentReference> query = step
				.toQuery();
		result.apply( query, metricAggregationsValues );
		return new Result<>( typeDescriptor.getJavaType(), metricAggregationsValues, result );
	}

	@Override
	public String toString() {
		return "Case{" + typeDescriptor + '}';
	}

	public static class Result<F> {
		private final Class<F> javaType;
		private final MetricAggregationsValues<F> metricAggregationsValues;
		private final InternalResult<F> result;

		private Result(Class<F> javaType, MetricAggregationsValues<F> metricAggregationsValues,
				InternalResult<F> result) {
			this.javaType = javaType;
			this.metricAggregationsValues = metricAggregationsValues;
			this.result = result;
		}

		public List<F> values() {
			return metricAggregationsValues.values();
		}

		// expected* can return null, which would mean that this type does not support a particular aggregation
		public F expectedSum() {
			return metricAggregationsValues.sum();
		}

		public F expectedMin() {
			return metricAggregationsValues.min();
		}

		public F expectedMax() {
			return metricAggregationsValues.max();
		}

		public Long expectedCount() {
			return metricAggregationsValues.count();
		}

		public Long expectedCountDistinct() {
			return metricAggregationsValues.countDistinct();
		}

		public F expectedAvg() {
			return metricAggregationsValues.avg();
		}

		public F computedSum() {
			return result.sum;
		}

		public F computedMax() {
			return result.max;
		}

		public F computedMin() {
			return result.min;
		}

		public Long computedCount() {
			return result.count;
		}

		public Long computedCountDistinct() {
			return result.countDistinct;
		}

		public F computedAvg() {
			return result.avg;
		}

		@Override
		public String toString() {
			return new StringJoiner( ", ", Result.class.getSimpleName() + "[", "]" )
					.add( "javaType=" + javaType )
					.add( "values=" + values() )
					.toString();
		}
	}

	private static class InternalResult<F> {
		AggregationKey<F> sumKey = AggregationKey.of( "sum" );
		AggregationKey<F> minKey = AggregationKey.of( "min" );
		AggregationKey<F> maxKey = AggregationKey.of( "max" );
		AggregationKey<Long> countKey = AggregationKey.of( "count" );
		AggregationKey<Long> countDistinctKey = AggregationKey.of( "countDistinct" );
		AggregationKey<F> avgKey = AggregationKey.of( "avg" );

		F sum;
		F min;
		F max;
		Long count;
		Long countDistinct;
		F avg;

		void apply(SearchQuery<DocumentReference> query, MetricAggregationsValues<F> metricAggregationsValues) {
			SearchResult<DocumentReference> result = query.fetch( 0 );
			if ( metricAggregationsValues.sum() != null ) {
				sum = result.aggregation( sumKey );
			}
			min = result.aggregation( minKey );
			max = result.aggregation( maxKey );
			count = result.aggregation( countKey );
			countDistinct = result.aggregation( countDistinctKey );
			avg = result.aggregation( avgKey );
		}
	}
}
