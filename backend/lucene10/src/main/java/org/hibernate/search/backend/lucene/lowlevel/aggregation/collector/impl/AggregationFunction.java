/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

public interface AggregationFunction<R extends AggregationFunction<?>> {

	void apply(long value);

	void merge(AggregationFunction<R> sibling);

	Long result();

	R implementation();

}
