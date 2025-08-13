/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.io.IOException;
import java.util.Collection;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningTextMultiValuesSource;

import com.carrotsearch.hppc.LongHashSet;

import org.apache.lucene.search.CollectorManager;

public class CountDistinctTextValuesCollectorManager implements CollectorManager<CountDistinctTextValuesCollector, Long> {

	private final JoiningTextMultiValuesSource source;
	private final String field;

	public CountDistinctTextValuesCollectorManager(JoiningTextMultiValuesSource source, String field) {
		this.source = source;
		this.field = field;
	}

	@Override
	public CountDistinctTextValuesCollector newCollector() throws IOException {
		return new CountDistinctTextValuesCollector( source, field );
	}

	@Override
	public Long reduce(Collection<CountDistinctTextValuesCollector> collectors) throws IOException {
		if ( collectors.isEmpty() ) {
			return 0L;
		}
		if ( collectors.size() == 1 ) {
			return (long) collectors.iterator().next().globalOrds().size();
		}
		LongHashSet ords = new LongHashSet();
		for ( CountDistinctTextValuesCollector collector : collectors ) {
			ords.addAll( collector.globalOrds() );
		}
		return (long) ords.size();
	}
}
