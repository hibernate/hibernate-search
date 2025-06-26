/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;

public class NumericTermsCollectorFactory
		implements CollectorFactory<NumericTermsCollector, NumericTermsCollector, NumericTermsCollectorManager> {

	public static CollectorFactory<NumericTermsCollector, NumericTermsCollector, NumericTermsCollectorManager> instance(
			LongMultiValuesSource valuesSource) {
		return new NumericTermsCollectorFactory( valuesSource );
	}

	public final CollectorKey<NumericTermsCollector, NumericTermsCollector> key = CollectorKey.create();
	private final LongMultiValuesSource valuesSource;

	public NumericTermsCollectorFactory(LongMultiValuesSource valuesSource) {
		this.valuesSource = valuesSource;
	}

	@Override
	public NumericTermsCollectorManager createCollectorManager(CollectorExecutionContext context) {
		return new NumericTermsCollectorManager( valuesSource );
	}

	@Override
	public CollectorKey<NumericTermsCollector, NumericTermsCollector> getCollectorKey() {
		return key;
	}
}
