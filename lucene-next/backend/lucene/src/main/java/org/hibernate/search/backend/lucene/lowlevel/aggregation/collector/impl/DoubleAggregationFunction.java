package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

public interface DoubleAggregationFunction<R extends DoubleAggregationFunction<?>> {

	void apply(double value);

	void merge(DoubleAggregationFunction<R> sibling);

	Double result();

	R implementation();

}
