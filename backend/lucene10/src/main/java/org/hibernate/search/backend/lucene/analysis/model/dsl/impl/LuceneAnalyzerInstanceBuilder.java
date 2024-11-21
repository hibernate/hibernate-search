/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.util.common.impl.Contracts;

import org.apache.lucene.analysis.Analyzer;

class LuceneAnalyzerInstanceBuilder implements LuceneAnalyzerBuilder {
	private final Analyzer instance;

	LuceneAnalyzerInstanceBuilder(Analyzer instance) {
		Contracts.assertNotNull( instance, "instance" );
		this.instance = instance;
	}

	@Override
	public Analyzer build(LuceneAnalysisComponentFactory factory) {
		return instance;
	}
}
