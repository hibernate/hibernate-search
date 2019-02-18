/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.hibernate.search.util.common.impl.CollectionHelper;

/**
 * @author Guillaume Smet
 */
public final class ScopedAnalyzer extends AnalyzerWrapper {

	private final Analyzer globalAnalyzer;

	private final Map<String, Analyzer> scopedAnalyzers;

	private ScopedAnalyzer(Analyzer globalAnalyzer, Map<String, Analyzer> scopedAnalyzers) {
		super( PER_FIELD_REUSE_STRATEGY );
		this.globalAnalyzer = globalAnalyzer;
		this.scopedAnalyzers = CollectionHelper.toImmutableMap( scopedAnalyzers );
	}

	@Override
	protected Analyzer getWrappedAnalyzer(String absoluteFieldPath) {
		Analyzer analyzer = scopedAnalyzers.get( absoluteFieldPath );

		if ( analyzer == null ) {
			return globalAnalyzer;
		}

		return analyzer;
	}

	public static class Builder {

		private final Analyzer globalAnalyzer;

		private final Map<String, Analyzer> scopedAnalyzers = new HashMap<>();

		public Builder(Analyzer globalAnalyzer) {
			this.globalAnalyzer = globalAnalyzer;
		}

		public void setAnalyzer(String absoluteFieldPath, Analyzer analyzer) {
			this.scopedAnalyzers.put( absoluteFieldPath, analyzer );
		}

		public ScopedAnalyzer build() {
			return new ScopedAnalyzer( globalAnalyzer, scopedAnalyzers );
		}
	}
}
