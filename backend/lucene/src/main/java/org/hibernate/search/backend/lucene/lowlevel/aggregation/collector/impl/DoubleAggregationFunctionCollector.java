/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.io.IOException;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

public class DoubleAggregationFunctionCollector<AF extends DoubleAggregationFunction<?>> extends SimpleCollector {

	private final LongMultiValuesSource valueSource;
	private final DoubleAggregationFunction<AF> aggregationFunction;
	private final Function<Long, Double> longToDouble;

	private LongMultiValues values;

	public DoubleAggregationFunctionCollector(LongMultiValuesSource valueSource,
			DoubleAggregationFunction<AF> aggregationFunction, Function<Long, Double> longToDouble) {
		this.valueSource = valueSource;
		this.aggregationFunction = aggregationFunction;
		this.longToDouble = longToDouble;
	}

	public void merge(DoubleAggregationFunctionCollector<AF> sibling) {
		aggregationFunction.merge( sibling.aggregationFunction );
	}

	public Double result() {
		return aggregationFunction.result();
	}

	@Override
	public void collect(int doc) throws IOException {
		if ( values.advanceExact( doc ) ) {
			while ( values.hasNextValue() ) {
				long value = values.nextValue();
				aggregationFunction.apply( longToDouble.apply( value ) );
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
