/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.util.Collection;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.LongMultiValuesSource;

import org.apache.lucene.search.CollectorManager;

public class NumericTermsCollectorManager
		implements CollectorManager<NumericTermsCollector, NumericTermsCollector> {

	private final LongMultiValuesSource valuesSource;

	public NumericTermsCollectorManager(LongMultiValuesSource valuesSource) {
		this.valuesSource = valuesSource;
	}

	@Override
	public NumericTermsCollector newCollector() {
		return new NumericTermsCollector( valuesSource );
	}

	@Override
	public NumericTermsCollector reduce(Collection<NumericTermsCollector> collection) {
		// TODO: actually reduce:
		return collection.iterator().next();
	}
}
