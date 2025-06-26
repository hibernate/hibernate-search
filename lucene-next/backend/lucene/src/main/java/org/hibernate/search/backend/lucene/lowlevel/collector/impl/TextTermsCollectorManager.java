/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.util.Collection;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValuesSource;

import org.apache.lucene.search.CollectorManager;

public class TextTermsCollectorManager
		implements CollectorManager<TextTermsCollector, TextTermsCollector> {

	private final TextMultiValuesSource valuesSource;
	private final String field;

	public TextTermsCollectorManager(String field, TextMultiValuesSource valuesSource) {
		this.field = field;
		this.valuesSource = valuesSource;
	}

	@Override
	public TextTermsCollector newCollector() {
		return new TextTermsCollector( field, valuesSource );
	}

	@Override
	public TextTermsCollector reduce(Collection<TextTermsCollector> collection) {
		// TODO: actually reduce:
		return collection.iterator().next();
	}
}
