/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public class ScopedLuceneAnalyzerReference extends LuceneAnalyzerReference implements ScopedAnalyzerReference {

	private static final Log LOG = LoggerFactory.make();

	private volatile ScopedLuceneAnalyzer analyzer;

	/*
	 * We keep a reference to the builder for two reasons:
	 *  1. Lazy initialization of the analyzer
	 *  2. Copies of the analyzer; see startCopy()
	 */
	private final Builder builder;

	public ScopedLuceneAnalyzerReference(Builder builder) {
		this.builder = builder;
	}

	@Override
	public ScopedLuceneAnalyzer getAnalyzer() {
		if ( analyzer == null ) {
			// Lazy initialization, done only once
			synchronized ( this ) {
				if ( analyzer == null ) {
					analyzer = builder.buildAnalyzer();
				}
			}
		}
		return analyzer;
	}

	@Override
	public void close() {
		getAnalyzer().close();
	}

	@Override
	public Builder startCopy() {
		return builder.clone();
	}

	public static class Builder implements ScopedAnalyzerReference.Builder, Cloneable {

		private LuceneAnalyzerReference globalAnalyzerReference;
		private final Map<String, LuceneAnalyzerReference> scopedAnalyzerReferences = new HashMap<>();

		public Builder(LuceneAnalyzerReference globalAnalyzerReference, Map<String, LuceneAnalyzerReference> scopedAnalyzers) {
			this.globalAnalyzerReference = globalAnalyzerReference;
			this.scopedAnalyzerReferences.putAll( scopedAnalyzers );
		}

		@Override
		public LuceneAnalyzerReference getGlobalAnalyzerReference() {
			return globalAnalyzerReference;
		}

		@Override
		public void setGlobalAnalyzerReference(AnalyzerReference globalAnalyzerReference) {
			this.globalAnalyzerReference = getLuceneAnalyzerReference( globalAnalyzerReference );
		}

		@Override
		public void addAnalyzerReference(String scope, AnalyzerReference analyzerReference) {
			scopedAnalyzerReferences.put( scope, getLuceneAnalyzerReference( analyzerReference ) );
		}

		@Override
		public ScopedLuceneAnalyzerReference build() {
			/*
			 * Defer the actual analyzer creation to when it is first used, so that
			 * dangling references can get initialized first.
			 */
			return new ScopedLuceneAnalyzerReference( this );
		}

		/**
		 * Defers the actual analyzer creation to when it is first used, so that
		 * the builder accepts dangling references.
		 *
		 * @author Yoann Rodiere
		 */
		private ScopedLuceneAnalyzer buildAnalyzer() {
			Analyzer globalAnalyzer = globalAnalyzerReference.getAnalyzer();

			Map<String, Analyzer> scopedAnalyzers = new HashMap<>();

			for ( Map.Entry<String, LuceneAnalyzerReference> entry : scopedAnalyzerReferences.entrySet() ) {
				scopedAnalyzers.put( entry.getKey(), entry.getValue().getAnalyzer() );
			}

			return new ScopedLuceneAnalyzer( globalAnalyzer, scopedAnalyzers );
		}

		@Override
		protected Builder clone() {
			return new Builder( globalAnalyzerReference, scopedAnalyzerReferences );
		}
	}

	private static LuceneAnalyzerReference getLuceneAnalyzerReference(AnalyzerReference analyzerReference) {
		if ( !analyzerReference.is( LuceneAnalyzerReference.class ) ) {
			throw LOG.analyzerReferenceIsNotLucene( analyzerReference );
		}

		return analyzerReference.unwrap( LuceneAnalyzerReference.class );
	}

}
