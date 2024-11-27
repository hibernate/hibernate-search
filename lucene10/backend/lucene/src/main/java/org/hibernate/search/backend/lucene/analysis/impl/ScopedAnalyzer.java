/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.util.common.impl.CollectionHelper;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;

public final class ScopedAnalyzer extends DelegatingAnalyzerWrapper {

	private final Map<String, Analyzer> scopedAnalyzers;

	private ScopedAnalyzer(Map<String, Analyzer> scopedAnalyzers) {
		super( PER_FIELD_REUSE_STRATEGY );
		this.scopedAnalyzers = CollectionHelper.toImmutableMap( scopedAnalyzers );
	}

	@Override
	protected Analyzer getWrappedAnalyzer(String absoluteFieldPath) {
		Analyzer analyzer = scopedAnalyzers.get( absoluteFieldPath );

		if ( analyzer == null ) {
			return AnalyzerConstants.KEYWORD_ANALYZER;
		}

		return analyzer;
	}

	public static class Builder {

		private final Map<String, Analyzer> scopedAnalyzers = new HashMap<>();

		public void setAnalyzer(String absoluteFieldPath, Analyzer analyzer) {
			this.scopedAnalyzers.put( absoluteFieldPath, analyzer );
		}

		public ScopedAnalyzer build() {
			return new ScopedAnalyzer( scopedAnalyzers );
		}
	}
}
