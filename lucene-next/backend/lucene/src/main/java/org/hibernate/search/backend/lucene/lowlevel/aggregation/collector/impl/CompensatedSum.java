/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

/**
 * <p>
 * The algorithm is inspired by {@code org.opensearch.search.aggregations.metrics.SumAggregator}
 * of <a href="https://opensearch.org/">OpenSearch</a>.
 */
public class CompensatedSum implements DoubleAggregationFunction<CompensatedSum> {

	private final KahanSummation kahanSummation = new KahanSummation( 0, 0 );

	@Override
	public void apply(double value) {
		kahanSummation.add( value );
	}

	@Override
	public void merge(DoubleAggregationFunction<CompensatedSum> sibling) {
		KahanSummation other = sibling.implementation().kahanSummation;
		kahanSummation.add( other.value(), other.delta() );
	}

	@Override
	public Double result() {
		return kahanSummation.value();
	}

	@Override
	public CompensatedSum implementation() {
		return this;
	}
}
