/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

public class Count implements AggregationFunction<Count> {

	private long count = 0L;

	@Override
	public void apply(long value) {
		count++;
	}

	@Override
	public void merge(AggregationFunction<Count> sibling) {
		count += sibling.implementation().count;
	}

	@Override
	public Long result() {
		return count;
	}

	@Override
	public Count implementation() {
		return this;
	}
}
