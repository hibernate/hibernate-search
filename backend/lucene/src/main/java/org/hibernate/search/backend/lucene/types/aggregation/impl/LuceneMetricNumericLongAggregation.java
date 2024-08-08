/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CountCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CountDistinctCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.search.aggregation.spi.FieldMetricAggregationBuilder;

public class LuceneMetricNumericLongAggregation extends AbstractLuceneNestableAggregation<Long> {

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final String operation;

	private CollectorKey<?, Long> collectorKey;

	LuceneMetricNumericLongAggregation(Builder builder) {
		super( builder );
		this.indexNames = builder.scope.hibernateSearchIndexNames();
		this.absoluteFieldPath = builder.field.absolutePath();
		this.operation = builder.operation;
	}

	@Override
	public Extractor<Long> request(AggregationRequestContext context) {
		JoiningLongMultiValuesSource source = JoiningLongMultiValuesSource.fromField(
				absoluteFieldPath, createNestedDocsProvider( context )
		);
		if ( "cardinality".equals( operation ) ) {
			CountDistinctCollectorFactory collectorFactory = new CountDistinctCollectorFactory( source );
			collectorKey = collectorFactory.getCollectorKey();
			context.requireCollector( collectorFactory );
		}
		else if ( "value_count".equals( operation ) ) {
			CountCollectorFactory collectorFactory = new CountCollectorFactory( source );
			collectorKey = collectorFactory.getCollectorKey();
			context.requireCollector( collectorFactory );
		}
		return new LuceneNumericMetricLongAggregationExtraction();
	}

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

	private static class Builder extends AbstractBuilder<Long> implements FieldMetricAggregationBuilder<Long> {
		private final String operation;

		public Builder(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<?> field,
				String operation) {
			super( scope, field );
			this.operation = operation;
		}

		@Override
		public LuceneMetricNumericLongAggregation build() {
			return new LuceneMetricNumericLongAggregation( this );
		}
	}
}
