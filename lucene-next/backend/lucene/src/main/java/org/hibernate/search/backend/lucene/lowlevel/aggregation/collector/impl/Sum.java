/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

public class Sum implements AggregationFunction<Sum> {

	private long sum = 0L;

	@Override
	public void apply(long value) {
		sum += value;
	}

	@Override
	public void merge(AggregationFunction<Sum> sibling) {
		apply( sibling.result() );
	}

	@Override
	public Long result() {
		return sum;
	}

	@Override
	public Sum implementation() {
		return this;
	}
}
