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
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.MetricAggregationsValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldLocation;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldValueCardinality;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

/**
 * Denotes a metric aggregations test case for a particular {@link FieldTypeDescriptor}.
 */
public class MetricAggregationsTestCase<F> {

	private final FieldTypeDescriptor<F, ?> typeDescriptor;
	private final boolean supported;
	private final MetricAggregationsValues<F> metricAggregationsValues;

	public MetricAggregationsTestCase(FieldTypeDescriptor<F, ?> typeDescriptor) {
		this.typeDescriptor = typeDescriptor;
		metricAggregationsValues = typeDescriptor.metricAggregationsValues();
		this.supported = metricAggregationsValues != null;
	}

	public boolean supported() {
		return supported;
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

		SearchQuery<DocumentReference> query = scope.query().where( SearchPredicateFactory::matchAll )
				.aggregation( result.sumKey(), f -> f.sum().field( fieldPath, typeDescriptor.getJavaType() ) )
				.toQuery();
		result.apply( query );
		return new Result<>( typeDescriptor.getJavaType(), metricAggregationsValues, result );
	}

	@Override
	public String toString() {
		return "Case{" + typeDescriptor + '}';
	}

	public static class Result<F> {
		public final Class<F> javaType;
		public final List<F> values;
		public final F expectedSum;
		public final F sumResult;

		public Result(Class<F> javaType, MetricAggregationsValues<F> metricAggregationsValues,
				InternalResult<F> result) {
			this.javaType = javaType;
			values = metricAggregationsValues.values();
			expectedSum = metricAggregationsValues.expectedSum();
			sumResult = result.sumResult;
		}

		@Override
		public String toString() {
			return new StringJoiner( ", ", Result.class.getSimpleName() + "[", "]" )
					.add( "javaType=" + javaType )
					.add( "values=" + values )
					.add( "expectedSum=" + expectedSum )
					.add( "sumResult=" + sumResult )
					.toString();
		}
	}

	private static class InternalResult<F> {
		AggregationKey<F> sumKey = AggregationKey.of( "sum" );
		F sumResult = null;

		AggregationKey<F> sumKey() {
			return sumKey;
		}

		F sum() {
			return sumResult;
		}

		void apply(SearchQuery<DocumentReference> query) {
			SearchResult<DocumentReference> result = query.fetch( 0 );
			sumResult = result.aggregation( sumKey );
		}
	}
}
