/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValuesSource;

public class TextTermsCollectorFactory
		implements CollectorFactory<TextTermsCollector, TextTermsCollector, TextTermsCollectorManager> {

	public static CollectorFactory<TextTermsCollector, TextTermsCollector, TextTermsCollectorManager> instance(
			String field, TextMultiValuesSource valuesSource) {
		return new TextTermsCollectorFactory( field, valuesSource );
	}


	public final CollectorKey<TextTermsCollector, TextTermsCollector> key = CollectorKey.create();
	private final TextMultiValuesSource valuesSource;
	private final String field;

	public TextTermsCollectorFactory(String field, TextMultiValuesSource valuesSource) {
		this.field = field;
		this.valuesSource = valuesSource;
	}

	@Override
	public TextTermsCollectorManager createCollectorManager(CollectorExecutionContext context) {
		return new TextTermsCollectorManager( field, valuesSource );
	}

	@Override
	public CollectorKey<TextTermsCollector, TextTermsCollector> getCollectorKey() {
		return key;
	}
}
