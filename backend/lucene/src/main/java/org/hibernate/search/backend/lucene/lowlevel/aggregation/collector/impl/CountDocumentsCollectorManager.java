/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.search.CollectorManager;

public class CountDocumentsCollectorManager implements CollectorManager<CountDocumentsCollector, Long> {

	@Override
	public CountDocumentsCollector newCollector() throws IOException {
		return new CountDocumentsCollector();
	}

	@Override
	public Long reduce(Collection<CountDocumentsCollector> collectors) throws IOException {
		long count = 0L;
		for ( CountDocumentsCollector collector : collectors ) {
			count += collector.count();
		}
		return count;
	}
}
