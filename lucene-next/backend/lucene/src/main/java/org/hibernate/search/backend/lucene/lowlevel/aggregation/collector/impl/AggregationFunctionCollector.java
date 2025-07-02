/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.search.ScoreMode;

public class AggregationFunctionCollector<R extends AggregationFunction<?>> implements Collector {

	private final LongMultiValuesSource valueSource;
	private final AggregationFunction<R> aggregationFunction;

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
	public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
		return new AggregationFunctionLeafCollector( valueSource.getValues( context ) );
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	public class AggregationFunctionLeafCollector implements LeafCollector {
		private final LongMultiValues values;

		public AggregationFunctionLeafCollector(LongMultiValues values) {
			this.values = values;
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
		public void setScorer(Scorable scorer) {
			// no-op by default
		}
	}
}
