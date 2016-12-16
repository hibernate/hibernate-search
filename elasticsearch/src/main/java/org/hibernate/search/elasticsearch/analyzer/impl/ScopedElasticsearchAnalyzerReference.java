/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public class ScopedElasticsearchAnalyzerReference extends ElasticsearchAnalyzerReference implements ScopedAnalyzerReference {

	private static final Log LOG = LoggerFactory.make();

	private volatile ScopedElasticsearchAnalyzer analyzer;

	/*
	 * We keep a reference to the builder for two reasons:
	 *  1. Lazy initialization of the analyzer
	 *  2. Copies of the analyzer; see startCopy()
	 */
	private final Builder builder;

	public ScopedElasticsearchAnalyzerReference(Builder builder) {
		this.builder = builder;
	}

	@Override
	public ScopedElasticsearchAnalyzer getAnalyzer() {
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
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		return analyzerType.isAssignableFrom( ScopedElasticsearchAnalyzerReference.class );
	}

	@Override
	public Builder startCopy() {
		return builder.clone();
	}

	public static class Builder implements ScopedAnalyzerReference.Builder, Cloneable {

		private ElasticsearchAnalyzerReference globalAnalyzerReference;
		private final Map<String, ElasticsearchAnalyzerReference> scopedAnalyzerReferences = new HashMap<>();

		public Builder(ElasticsearchAnalyzerReference globalAnalyzerReference, Map<String, ElasticsearchAnalyzerReference> scopedAnalyzers) {
			this.globalAnalyzerReference = globalAnalyzerReference;
			this.scopedAnalyzerReferences.putAll( scopedAnalyzers );
		}

		@Override
		public ElasticsearchAnalyzerReference getGlobalAnalyzerReference() {
			return globalAnalyzerReference;
		}

		@Override
		public void setGlobalAnalyzerReference(AnalyzerReference globalAnalyzerReference) {
			this.globalAnalyzerReference = getElasticsearchAnalyzerReference( globalAnalyzerReference );
		}

		@Override
		public void addAnalyzerReference(String scope, AnalyzerReference analyzerReference) {
			scopedAnalyzerReferences.put( scope, getElasticsearchAnalyzerReference( analyzerReference ) );
		}

		@Override
		public ScopedElasticsearchAnalyzerReference build() {
			/*
			 * Defer the actual analyzer creation to when it is first used, so that
			 * dangling references can get initialized first.
			 */
			return new ScopedElasticsearchAnalyzerReference( this );
		}

		/**
		 * Defers the actual analyzer creation to when it is first used, so that
		 * the builder accepts dangling references.
		 *
		 * @author Yoann Rodiere
		 */
		private ScopedElasticsearchAnalyzer buildAnalyzer() {
			ElasticsearchAnalyzer globalAnalyzer = globalAnalyzerReference.getAnalyzer();

			Map<String, ElasticsearchAnalyzer> scopedAnalyzers = new HashMap<>();

			for ( Map.Entry<String, ElasticsearchAnalyzerReference> entry : scopedAnalyzerReferences.entrySet() ) {
				scopedAnalyzers.put( entry.getKey(), entry.getValue().getAnalyzer() );
			}

			return new ScopedElasticsearchAnalyzer( globalAnalyzer, scopedAnalyzers );
		}

		@Override
		protected Builder clone() {
			return new Builder( globalAnalyzerReference, scopedAnalyzerReferences );
		}
	}

	private static ElasticsearchAnalyzerReference getElasticsearchAnalyzerReference(AnalyzerReference analyzerReference) {
		if ( !analyzerReference.is( ElasticsearchAnalyzerReference.class ) ) {
			throw LOG.analyzerReferenceIsNotRemote( analyzerReference );
		}

		return analyzerReference.unwrap( ElasticsearchAnalyzerReference.class );
	}

}
