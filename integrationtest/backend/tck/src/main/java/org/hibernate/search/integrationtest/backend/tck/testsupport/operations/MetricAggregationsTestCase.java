/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.ByteFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.IntegerFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.LongFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.ShortFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.MetricAggregationsValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldLocation;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldValueCardinality;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TypeAssertionHelper;
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
		this.metricAggregationsValues = typeDescriptor.metricAggregationsValues();
	}

	public FieldTypeDescriptor<F, ?> typeDescriptor() {
		return typeDescriptor;
	}

	public int contribute(BulkIndexer indexer, SingleFieldIndexBinding binding) {
		int i = 0;
		for ( F value : metricAggregationsValues.values() ) {
			String uniqueName = typeDescriptor.getUniqueName();
			String keyA = String.format( Locale.ROOT, "%03d_ROOT_%s", ++i, uniqueName );
			String keyB = String.format( Locale.ROOT, "%03d_NEST_%s", i, uniqueName );
			String keyC = String.format( Locale.ROOT, "%03d_FLAT_%s", i, uniqueName );
			indexer.add( documentProvider( keyA, document -> binding.initSingleValued( typeDescriptor,
					IndexFieldLocation.ROOT, document, value
			) ) );
			indexer.add( documentProvider( keyB, document -> binding.initSingleValued(
					typeDescriptor, IndexFieldLocation.IN_NESTED, document, value ) ) );
			indexer.add( documentProvider( keyC, document -> binding.initSingleValued(
					typeDescriptor, IndexFieldLocation.IN_FLATTENED, document, value ) ) );
		}
		return metricAggregationsValues.values().size() * 3;
	}

	public <T> Result<T> testMetricsAggregation(StubMappingScope scope, SingleFieldIndexBinding binding, ValueModel valueModel,
			TypeAssertionHelper<F, T> typeAssertionHelper) {
		InternalResult<T> result = new InternalResult<>();
		String fieldPath = binding.getFieldPath(
				TestedFieldStructure.of( IndexFieldLocation.ROOT, IndexFieldValueCardinality.SINGLE_VALUED ), typeDescriptor );
		Class<T> javaClass = typeAssertionHelper.getJavaClass();

		SearchQueryOptionsStep<?, ?, DocumentReference, StubLoadingOptionsStep, ?, ?> step = scope.query()
				.where( SearchPredicateFactory::matchAll )
				.aggregation( result.minKey, f -> f.min().field( fieldPath, javaClass, valueModel ) )
				.aggregation( result.maxKey, f -> f.max().field( fieldPath, javaClass, valueModel ) )
				.aggregation( result.countKey, f -> f.count().field( fieldPath ) )
				.aggregation( result.countDistinctKey, f -> f.count().field( fieldPath ).distinct() )
				.aggregation( result.avgKey, f -> f.avg().field( fieldPath, javaClass, valueModel ) );

		if ( metricAggregationsValues.sum() != null ) {
			step.aggregation( result.sumKey, f -> f.sum().field( fieldPath, javaClass, valueModel ) );
		}

		SearchQuery<DocumentReference> query = step.toQuery();
		result.apply( query );

		return new Result<>( result, typeAssertionHelper, valueModel );
	}

	@Override
	public String toString() {
		return "Case{" + typeDescriptor + '}';
	}

	public class Result<T> {
		private final InternalResult<T> result;
		private final TypeAssertionHelper<F, T> typeAssertionHelper;
		private final ValueModel valueModel;

		private Result(InternalResult<T> result, TypeAssertionHelper<F, T> typeAssertionHelper, ValueModel valueModel) {
			this.result = result;
			this.typeAssertionHelper = typeAssertionHelper;
			this.valueModel = valueModel;
		}

		public List<F> values() {
			return metricAggregationsValues.values();
		}

		@SuppressWarnings("unchecked")
		public void validate() {
			validateCommon();
			if ( metricAggregationsValues.sum() != null ) {
				typeAssertionHelper.assertSameAggregation( result.sum, metricAggregationsValues.sum() );
			}
			typeAssertionHelper.assertSameAggregation( result.min, metricAggregationsValues.min() );
			typeAssertionHelper.assertSameAggregation( result.max, metricAggregationsValues.max() );

			// Elasticsearch would return a double average even for int types, and if we access a raw value,
			// it can contain decimals, so we handle this case differently to the others:
			if ( typeAssertionHelper.getJavaClass().equals( String.class )
					&& ValueModel.RAW.equals( valueModel )
					&& ( IntegerFieldTypeDescriptor.INSTANCE.equals( typeDescriptor )
							|| LongFieldTypeDescriptor.INSTANCE.equals( typeDescriptor )
							|| ShortFieldTypeDescriptor.INSTANCE.equals( typeDescriptor )
							|| ByteFieldTypeDescriptor.INSTANCE.equals( typeDescriptor ) ) ) {
				// the cast is "safe" as we've tested the `getJavaClass` just above.
				typeAssertionHelper.assertSameAggregation( result.avg,
						(F) Double.toString( metricAggregationsValues.avgRaw() ) );
			}
			else {
				typeAssertionHelper.assertSameAggregation( result.avg, metricAggregationsValues.avg() );
			}
		}

		public void validateDouble() {
			validateCommon();
			if ( metricAggregationsValues.sum() != null ) {
				assertThat( ( (Number) result.sum ).doubleValue() ).isEqualTo( metricAggregationsValues.sumRaw() );
			}
			assertThat( ( (Number) result.min ).doubleValue() ).isEqualTo( metricAggregationsValues.minRaw() );
			assertThat( ( (Number) result.max ).doubleValue() ).isEqualTo( metricAggregationsValues.maxRaw() );
			assertThat( ( (Number) result.avg ).doubleValue() ).isEqualTo( metricAggregationsValues.avgRaw() );
		}

		private void validateCommon() {
			assertThat( result.count ).isEqualTo( metricAggregationsValues.count() );
			assertThat( result.countDistinct ).isEqualTo( metricAggregationsValues.countDistinct() );
		}

		@Override
		public String toString() {
			return new StringJoiner( ", ", Result.class.getSimpleName() + "[", "]" )
					.add( "javaType=" + "javaType" )
					.add( "values=" + values() )
					.toString();
		}
	}

	private class InternalResult<V> {
		AggregationKey<V> sumKey = AggregationKey.of( "sum" );
		AggregationKey<V> minKey = AggregationKey.of( "min" );
		AggregationKey<V> maxKey = AggregationKey.of( "max" );
		AggregationKey<Long> countKey = AggregationKey.of( "count" );
		AggregationKey<Long> countDistinctKey = AggregationKey.of( "countDistinct" );
		AggregationKey<V> avgKey = AggregationKey.of( "avg" );

		V sum;
		V min;
		V max;
		Long count;
		Long countDistinct;
		V avg;

		void apply(SearchQuery<DocumentReference> query) {
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
