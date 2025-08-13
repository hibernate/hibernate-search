/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.io.IOException;
import java.util.Collection;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningTextMultiValuesSource;

import org.apache.lucene.search.CollectorManager;

public class CountTextValuesCollectorManager implements CollectorManager<CountTextValuesCollector, Long> {

	private final JoiningTextMultiValuesSource source;

	public CountTextValuesCollectorManager(JoiningTextMultiValuesSource source) {
		this.source = source;
	}

	@Override
	public CountTextValuesCollector newCollector() throws IOException {
		return new CountTextValuesCollector( source );
	}

	@Override
	public Long reduce(Collection<CountTextValuesCollector> collectors) throws IOException {
		long count = 0;
		for ( CountTextValuesCollector collector : collectors ) {
			count += collector.count();
		}
		return count;
	}
}
