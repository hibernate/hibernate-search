/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

public class AggregationFunctionCollector<R extends AggregationFunction<?>> extends SimpleCollector {

	private final LongMultiValuesSource valueSource;
	private final AggregationFunction<R> aggregationFunction;

	private LongMultiValues values;

	public AggregationFunctionCollector(LongMultiValuesSource valueSource, AggregationFunction<R> aggregationFunction) {
		this.valueSource = valueSource;
		this.aggregationFunction = aggregationFunction;
	}

	public void merge(AggregationFunctionCollector<R> sibling) {
		aggregationFunction.merge( sibling.aggregationFunction );
	}

	public Long result() {
		return aggregationFunction.result();
	}

	@Override
	public void collect(int doc) throws IOException {
		if ( values.advanceExact( doc ) ) {
			while ( values.hasNextValue() ) {
				long value = values.nextValue();
				aggregationFunction.apply( value );
				if ( !aggregationFunction.acceptMultipleValues() ) {
					break;
				}
			}
		}
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		values = valueSource.getValues( context );
	}

	@Override
	public void finish() throws IOException {
		values = null;
	}
}
