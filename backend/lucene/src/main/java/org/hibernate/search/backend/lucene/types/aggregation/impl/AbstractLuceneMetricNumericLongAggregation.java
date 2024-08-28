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

public abstract class AbstractLuceneMetricNumericLongAggregation extends AbstractLuceneNestableAggregation<Long> {

	private final Set<String> indexNames;
	private final String absoluteFieldPath;

	protected CollectorKey<?, Long> collectorKey;

	AbstractLuceneMetricNumericLongAggregation(AbstractBuilder<Long> builder) {
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
}
