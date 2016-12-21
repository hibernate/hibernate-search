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
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public class ScopedLuceneAnalyzerReference extends LuceneAnalyzerReference implements ScopedAnalyzerReference {

	private static final Log LOG = LoggerFactory.make();

	private ScopedLuceneAnalyzer analyzer;

	private DeferredInitializationBuilder builder;

	public ScopedLuceneAnalyzerReference(ScopedLuceneAnalyzer analyzer) {
		this.builder = null;
		this.analyzer = analyzer; // Already initialized
	}

	private ScopedLuceneAnalyzerReference(DeferredInitializationBuilder builder) {
		this.builder = builder;
		this.analyzer = null; // Not initialized yet
	}

	@Override
	public ScopedLuceneAnalyzer getAnalyzer() {
		if ( analyzer == null ) {
			throw LOG.lazyLuceneAnalyzerReferenceNotInitialized( this );
		}
		return analyzer;
	}

	public boolean isInitialized() {
		return analyzer != null;
	}

	public void initialize() {
		if ( this.analyzer != null ) {
			throw new AssertionFailure( "A lucene analyzer reference has been initialized more than once: " + this );
		}
		this.analyzer = builder.buildAnalyzer();
		this.builder = null;
	}

	@Override
	public void close() {
		if ( isInitialized() ) {
			getAnalyzer().close();
		}
	}

	@Override
	public CopyBuilder startCopy() {
		return new CopyBuilder( getAnalyzer() );
	}

	/**
	 * A builder that defers the actual analyzer creation to later during the search
	 * factory initialization, so that the builder accepts dangling references.
	 *
	 * @author Yoann Rodiere
	 */
	public static class DeferredInitializationBuilder implements ScopedAnalyzerReference.Builder {

		private LuceneAnalyzerReference globalAnalyzerReference;
		private final Map<String, LuceneAnalyzerReference> scopedAnalyzerReferences = new HashMap<>();

		public DeferredInitializationBuilder(LuceneAnalyzerReference globalAnalyzerReference,
				Map<String, LuceneAnalyzerReference> scopedAnalyzers) {
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
			return new ScopedLuceneAnalyzerReference( this );
		}

		protected final ScopedLuceneAnalyzer buildAnalyzer() {
			Analyzer globalAnalyzer = globalAnalyzerReference.getAnalyzer();

			Map<String, Analyzer> scopedAnalyzers = new HashMap<>();
			for ( Map.Entry<String, LuceneAnalyzerReference> entry : scopedAnalyzerReferences.entrySet() ) {
				scopedAnalyzers.put( entry.getKey(), entry.getValue().getAnalyzer() );
			}

			return new ScopedLuceneAnalyzer( globalAnalyzer, scopedAnalyzers );
		}
	}

	public static class CopyBuilder implements ScopedAnalyzerReference.CopyBuilder {

		private final ScopedLuceneAnalyzer baseAnalyzer;
		private final Map<String, Analyzer> scopedAnalyzersOverrides = new HashMap<>();

		protected CopyBuilder(ScopedLuceneAnalyzer baseAnalyzer) {
			this.baseAnalyzer = baseAnalyzer;
		}

		@Override
		public void addAnalyzerReference(String scope, AnalyzerReference analyzerReference) {
			scopedAnalyzersOverrides.put( scope, getLuceneAnalyzerReference( analyzerReference ).getAnalyzer() );
		}

		@Override
		public ScopedLuceneAnalyzerReference build() {
			Analyzer globalAnalyzer = baseAnalyzer.getGlobalAnalyzer();

			Map<String, Analyzer> scopedAnalyzers = new HashMap<>( baseAnalyzer.getScopedAnalyzers() );
			scopedAnalyzers.putAll( scopedAnalyzersOverrides );

			ScopedLuceneAnalyzer scopedAnalyzer = new ScopedLuceneAnalyzer( globalAnalyzer, scopedAnalyzers );

			return new ScopedLuceneAnalyzerReference( scopedAnalyzer );
		}
	}

	private static LuceneAnalyzerReference getLuceneAnalyzerReference(AnalyzerReference analyzerReference) {
		if ( !analyzerReference.is( LuceneAnalyzerReference.class ) ) {
			throw LOG.analyzerReferenceIsNotLucene( analyzerReference );
		}

		return analyzerReference.unwrap( LuceneAnalyzerReference.class );
	}

}
