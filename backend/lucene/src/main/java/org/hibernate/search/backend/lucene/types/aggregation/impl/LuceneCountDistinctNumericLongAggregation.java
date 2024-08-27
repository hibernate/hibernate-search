/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CountDistinctCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;

public class LuceneCountDistinctNumericLongAggregation extends AbstractLuceneMetricNumericLongAggregation {

	public static <F> Factory<F> factory(AbstractLuceneNumericFieldCodec<F, ?> codec) {
		return new Factory<>( codec, "cardinality" );
	}

	LuceneCountDistinctNumericLongAggregation(Builder builder) {
		super( builder );
	}

	@Override
	void fillCollectors(JoiningLongMultiValuesSource source, AggregationRequestContext context) {
		CountDistinctCollectorFactory collectorFactory = new CountDistinctCollectorFactory( source );
		collectorKey = collectorFactory.getCollectorKey();
		context.requireCollector( collectorFactory );
	}
}
