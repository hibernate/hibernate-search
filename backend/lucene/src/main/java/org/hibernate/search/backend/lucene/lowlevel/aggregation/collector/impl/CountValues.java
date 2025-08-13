/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.util.Locale;

public class CountValues implements AggregationFunction<CountValues> {

	private long count = 0L;

	@Override
	public void apply(long value) {
		count++;
	}

	@Override
	public void merge(AggregationFunction<CountValues> sibling) {
		count += sibling.implementation().count;
	}

	@Override
	public Long result() {
		return count;
	}

	@Override
	public CountValues implementation() {
		return this;
	}

	@Override
	public String toString() {
		return String.format( Locale.ROOT, "CountValues{count=%d}", count );
	}
}
