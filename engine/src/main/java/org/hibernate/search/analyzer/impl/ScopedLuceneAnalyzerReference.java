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

	/*
	 * We keep a reference to the builder for two reasons:
	 *  1. Deferred initialization of the analyzer
	 *  2. Copies of the analyzer; see startCopy()
	 */
	private final AbstractBuilder builder;

	public ScopedLuceneAnalyzerReference(AbstractBuilder builder, ScopedLuceneAnalyzer analyzer) {
		this.builder = builder;
		this.analyzer = analyzer; // Already initialized
	}

	public ScopedLuceneAnalyzerReference(AbstractBuilder builder) {
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
	}

	@Override
	public void close() {
		if ( isInitialized() ) {
			getAnalyzer().close();
		}
	}

	@Override
	public CopyBuilder startCopy() {
		return new CopyBuilder( builder );
	}

	public abstract static class AbstractBuilder implements ScopedAnalyzerReference.Builder {

		private LuceneAnalyzerReference globalAnalyzerReference;
		private final Map<String, LuceneAnalyzerReference> scopedAnalyzerReferences = new HashMap<>();

		protected AbstractBuilder(AbstractBuilder copied) {
			this( copied.globalAnalyzerReference, copied.scopedAnalyzerReferences );
		}

		public AbstractBuilder(LuceneAnalyzerReference globalAnalyzerReference,
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
		public abstract ScopedLuceneAnalyzerReference build();

		protected final ScopedLuceneAnalyzer buildAnalyzer() {
			Analyzer globalAnalyzer = globalAnalyzerReference.getAnalyzer();

			Map<String, Analyzer> scopedAnalyzers = new HashMap<>();

			for ( Map.Entry<String, LuceneAnalyzerReference> entry : scopedAnalyzerReferences.entrySet() ) {
				scopedAnalyzers.put( entry.getKey(), entry.getValue().getAnalyzer() );
			}

			return new ScopedLuceneAnalyzer( globalAnalyzer, scopedAnalyzers );
		}
	}

	/**
	 * A builder that defers the actual analyzer creation to later during the search
	 * factory initialization, so that the builder accepts dangling references.
	 *
	 * @author Yoann Rodiere
	 */
	public static class DeferredInitializationBuilder extends AbstractBuilder {

		public DeferredInitializationBuilder(LuceneAnalyzerReference globalAnalyzerReference,
				Map<String, LuceneAnalyzerReference> scopedAnalyzers) {
			super( globalAnalyzerReference, scopedAnalyzers );
		}

		@Override
		public ScopedLuceneAnalyzerReference build() {
			return new ScopedLuceneAnalyzerReference( this );
		}
	}

	public static class CopyBuilder extends AbstractBuilder {

		private CopyBuilder(AbstractBuilder copied) {
			super( copied );
		}

		@Override
		public ScopedLuceneAnalyzerReference build() {
			ScopedLuceneAnalyzer analyzer = buildAnalyzer();
			return new ScopedLuceneAnalyzerReference( this, analyzer );
		}

	}

	private static LuceneAnalyzerReference getLuceneAnalyzerReference(AnalyzerReference analyzerReference) {
		if ( !analyzerReference.is( LuceneAnalyzerReference.class ) ) {
			throw LOG.analyzerReferenceIsNotLucene( analyzerReference );
		}

		return analyzerReference.unwrap( LuceneAnalyzerReference.class );
	}

}
