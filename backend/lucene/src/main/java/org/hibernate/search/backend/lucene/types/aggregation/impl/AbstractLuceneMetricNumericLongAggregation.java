/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.search.aggregation.spi.FieldMetricAggregationBuilder;
import org.hibernate.search.util.common.AssertionFailure;

public abstract class AbstractLuceneMetricNumericLongAggregation extends AbstractLuceneNestableAggregation<Long> {

	private final Set<String> indexNames;
	private final String absoluteFieldPath;

	protected CollectorKey<?, Long> collectorKey;

	AbstractLuceneMetricNumericLongAggregation(Builder builder) {
		super( builder );
		this.indexNames = builder.scope.hibernateSearchIndexNames();
		this.absoluteFieldPath = builder.field.absolutePath();
	}

	@Override
	public Extractor<Long> request(AggregationRequestContext context) {
		JoiningLongMultiValuesSource source = JoiningLongMultiValuesSource.fromField(
				absoluteFieldPath, createNestedDocsProvider( context )
		);
		fillCollectors( source, context );

		return new LuceneNumericMetricLongAggregationExtraction();
	}

	abstract void fillCollectors(JoiningLongMultiValuesSource source, AggregationRequestContext context);

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	private class LuceneNumericMetricLongAggregationExtraction implements Extractor<Long> {
		@Override
		public Long extract(AggregationExtractContext context) {
			return context.getFacets( collectorKey );
		}
	}

	public static class Factory<F>
			extends AbstractLuceneCodecAwareSearchQueryElementFactory<FieldMetricAggregationBuilder<Long>,
					F,
					AbstractLuceneNumericFieldCodec<F, ?>> {

		private final String operation;

		public Factory(AbstractLuceneNumericFieldCodec<F, ?> codec, String operation) {
			super( codec );
			this.operation = operation;
		}

		@Override
		public FieldMetricAggregationBuilder<Long> create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			return new Builder( scope, field, operation );
		}
	}

	protected static class Builder extends AbstractBuilder<Long> implements FieldMetricAggregationBuilder<Long> {
		private final String operation;

		public Builder(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<?> field,
				String operation) {
			super( scope, field );
			this.operation = operation;
		}

		@Override
		public AbstractLuceneMetricNumericLongAggregation build() {
			if ( "value_count".equals( operation ) ) {
				return new LuceneCountNumericLongAggregation( this );
			}
			else if ( "cardinality".equals( operation ) ) {
				return new LuceneCountDistinctNumericLongAggregation( this );
			}
			else {
				throw new AssertionFailure( "Aggregation operation not supported: " + operation );
			}
		}
	}
}
