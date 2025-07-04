/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;
import java.util.List;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;

import org.apache.lucene.search.CollectorManager;

public class NumericTermsCollectorFactory
		implements CollectorFactory<NumericTermsCollector, NumericTermsCollector, NumericTermsCollectorManager> {

	public static CollectorFactory<NumericTermsCollector, NumericTermsCollector, NumericTermsCollectorManager> instance(
			LongMultiValuesSource valuesSource, List<CollectorFactory<?, ?, ?>> collectorFactories) {
		return new NumericTermsCollectorFactory( valuesSource, collectorFactories );
	}

	private final CollectorKey<NumericTermsCollector, NumericTermsCollector> key = CollectorKey.create();
	private final LongMultiValuesSource valuesSource;
	private final List<CollectorFactory<?, ?, ?>> collectorFactories;

	public NumericTermsCollectorFactory(LongMultiValuesSource valuesSource,
			List<CollectorFactory<?, ?, ?>> collectorFactories) {
		this.valuesSource = valuesSource;
		this.collectorFactories = collectorFactories;
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public NumericTermsCollectorManager createCollectorManager(CollectorExecutionContext context) throws IOException {
		CollectorKey<?, ?>[] keys = new CollectorKey<?, ?>[collectorFactories.size()];
		var managers = new CollectorManager[collectorFactories.size()];
		int index = 0;
		for ( CollectorFactory<?, ?, ?> factory : collectorFactories ) {
			keys[index] = factory.getCollectorKey();
			CollectorManager<?, ?> collectorManager = factory.createCollectorManager( context );
			managers[index] = collectorManager;
			index++;
		}
		return new NumericTermsCollectorManager( valuesSource, keys, managers );
	}

	@Override
	public CollectorKey<NumericTermsCollector, NumericTermsCollector> getCollectorKey() {
		return key;
	}
}
