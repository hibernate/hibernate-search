/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Supplier;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;

import org.apache.lucene.search.CollectorManager;

public class AggregationFunctionCollectorManager<T extends AggregationFunction<?>>
		implements CollectorManager<AggregationFunctionCollector<T>, Long> {

	private final JoiningLongMultiValuesSource source;
	private final Supplier<AggregationFunction<T>> functionSupplier;

	public AggregationFunctionCollectorManager(JoiningLongMultiValuesSource source,
			Supplier<AggregationFunction<T>> functionSupplier) {
		this.source = source;
		this.functionSupplier = functionSupplier;
	}

	@Override
	public AggregationFunctionCollector<T> newCollector() {
		return new AggregationFunctionCollector<>( source, functionSupplier.get() );
	}

	@Override
	public Long reduce(Collection<AggregationFunctionCollector<T>> collectors) throws IOException {
		if ( collectors.isEmpty() ) {
			return null;
		}

		var iterator = collectors.iterator();
		var identity = iterator.next();
		while ( iterator.hasNext() ) {
			identity.merge( iterator.next() );
		}
		return identity.result();
	}
}
