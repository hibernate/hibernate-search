/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.util.Locale;

public class Sum implements AggregationFunction<Sum> {

	private Long sum = null;

	@Override
	public void apply(long value) {
		if ( sum == null ) {
			sum = value;
		}
		else {
			sum += value;
		}
	}

	@Override
	public void merge(AggregationFunction<Sum> sibling) {
		Long result = sibling.result();
		if ( result != null ) {
			apply( result );
		}
	}

	@Override
	public Long result() {
		return sum;
	}

	@Override
	public Sum implementation() {
		return this;
	}

	@Override
	public String toString() {
		return String.format( Locale.ROOT, "Sum{sum=%d}", sum );
	}
}
