/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;
import java.util.List;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValuesSource;

import org.apache.lucene.search.CollectorManager;

public class TextTermsCollectorFactory
		implements CollectorFactory<TextTermsCollector, TextTermsCollector, TextTermsCollectorManager> {

	public static CollectorFactory<TextTermsCollector, TextTermsCollector, TextTermsCollectorManager> instance(
			String field, TextMultiValuesSource valuesSource, List<CollectorFactory<?, ?, ?>> collectorFactories) {
		return new TextTermsCollectorFactory( field, valuesSource, collectorFactories );
	}

	public final CollectorKey<TextTermsCollector, TextTermsCollector> key = CollectorKey.create();
	private final TextMultiValuesSource valuesSource;
	private final String field;
	private final List<CollectorFactory<?, ?, ?>> collectorFactories;

	public TextTermsCollectorFactory(String field, TextMultiValuesSource valuesSource,
			List<CollectorFactory<?, ?, ?>> collectorFactories) {
		this.field = field;
		this.valuesSource = valuesSource;
		this.collectorFactories = collectorFactories;
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public TextTermsCollectorManager createCollectorManager(CollectorExecutionContext context) throws IOException {
		CollectorKey<?, ?>[] keys = new CollectorKey<?, ?>[collectorFactories.size()];
		var managers = new CollectorManager[collectorFactories.size()];
		int index = 0;
		for ( CollectorFactory<?, ?, ?> factory : collectorFactories ) {
			keys[index] = factory.getCollectorKey();
			CollectorManager<?, ?> collectorManager = factory.createCollectorManager( context );
			managers[index] = collectorManager;
			index++;
		}
		return new TextTermsCollectorManager( field, valuesSource, keys, managers );
	}

	@Override
	public CollectorKey<TextTermsCollector, TextTermsCollector> getCollectorKey() {
		return key;
	}
}
