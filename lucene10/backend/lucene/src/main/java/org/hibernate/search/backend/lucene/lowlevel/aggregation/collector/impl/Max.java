/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

public class Max implements AggregationFunction<Max> {

	private Long max;

	@Override
	public void apply(long value) {
		if ( max == null ) {
			max = value;
			return;
		}

		max = Math.max( max, value );
	}

	@Override
	public void merge(AggregationFunction<Max> sibling) {
		Long other = sibling.implementation().max;
		apply( other );
	}

	@Override
	public Long result() {
		return max;
	}

	@Override
	public Max implementation() {
		return this;
	}
}
