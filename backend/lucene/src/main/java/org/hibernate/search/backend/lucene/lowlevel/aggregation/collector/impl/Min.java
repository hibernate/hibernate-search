/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

public class Min implements AggregationFunction<Min> {

	private Long min;

	@Override
	public void apply(long value) {
		if ( min == null ) {
			min = value;
			return;
		}

		min = Math.min( min, value );
	}

	@Override
	public void merge(AggregationFunction<Min> sibling) {
		Long other = sibling.implementation().min;
		apply( other );
	}

	@Override
	public Long result() {
		return min;
	}

	@Override
	public Min implementation() {
		return this;
	}
}
