/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.util.Locale;

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
		if ( other.initialized() ) {
			kahanSummation.add( other.value(), other.delta() );
		}
	}

	@Override
	public Double result() {
		if ( kahanSummation.initialized() ) {
			return kahanSummation.value();
		}
		else {
			return null;
		}
	}

	@Override
	public CompensatedSum implementation() {
		return this;
	}

	@Override
	public String toString() {
		return String.format( Locale.ROOT, "CompensatedSum{kahanSummation=%s}", kahanSummation );
	}
}
