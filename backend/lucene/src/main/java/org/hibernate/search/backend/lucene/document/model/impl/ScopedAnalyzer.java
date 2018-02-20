/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.hibernate.search.util.impl.CollectionHelper;

/**
 * @author Guillaume Smet
 */
public class ScopedAnalyzer extends AnalyzerWrapper {

	private final Analyzer globalAnalyzer;

	private final Map<String, Analyzer> scopedAnalyzers;

	private ScopedAnalyzer(Analyzer globalAnalyzer, Map<String, Analyzer> scopedAnalyzers) {
		super( PER_FIELD_REUSE_STRATEGY );
		this.globalAnalyzer = globalAnalyzer;
		this.scopedAnalyzers = CollectionHelper.toImmutableMap( scopedAnalyzers );
	}

	@Override
	protected Analyzer getWrappedAnalyzer(String fieldName) {
		Analyzer analyzer = scopedAnalyzers.get( fieldName );

		if ( analyzer == null ) {
			return globalAnalyzer;
		}

		return analyzer;
	}

	static class Builder {

		private final Analyzer globalAnalyzer;

		private final Map<String, Analyzer> scopedAnalyzers = new HashMap<>();

		Builder(Analyzer globalAnalyzer) {
			this.globalAnalyzer = globalAnalyzer;
		}

		void setAnalyzer(String fieldName, Analyzer analyzer) {
			this.scopedAnalyzers.put( fieldName, analyzer );
		}

		ScopedAnalyzer build() {
			return new ScopedAnalyzer( globalAnalyzer, scopedAnalyzers );
		}
	}
}
