/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningTextMultiValuesSource;

public class CountTextValuesCollectorFactory
		implements
		CollectorFactory<CountTextValuesCollector, Long, CountTextValuesCollectorManager> {

	private final JoiningTextMultiValuesSource source;
	private final CollectorKey<CountTextValuesCollector, Long> key = CollectorKey.create();

	public CountTextValuesCollectorFactory(JoiningTextMultiValuesSource source, String field) {
		this.source = source;
	}

	@Override
	public CountTextValuesCollectorManager createCollectorManager(CollectorExecutionContext context) {
		return new CountTextValuesCollectorManager( source );
	}

	@Override
	public CollectorKey<CountTextValuesCollector, Long> getCollectorKey() {
		return key;
	}
}
