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
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public class ScopedElasticsearchAnalyzerReference extends ElasticsearchAnalyzerReference implements ScopedAnalyzerReference {

	private static final Log LOG = LoggerFactory.make();

	private ScopedElasticsearchAnalyzer analyzer;

	/*
	 * We keep a reference to the builder for two reasons:
	 *  1. Deferred initialization of the analyzer
	 *  2. Copies of the analyzer; see startCopy()
	 */
	private final AbstractBuilder builder;

	public ScopedElasticsearchAnalyzerReference(AbstractBuilder builder, ScopedElasticsearchAnalyzer analyzer) {
		this.builder = builder;
		this.analyzer = analyzer; // Already initialized
	}

	public ScopedElasticsearchAnalyzerReference(AbstractBuilder builder) {
		this.builder = builder;
		this.analyzer = null; // Not initialized yet
	}

	@Override
	public ScopedElasticsearchAnalyzer getAnalyzer() {
		if ( analyzer == null ) {
			throw LOG.lazyRemoteAnalyzerReferenceNotInitialized( this );
		}
		return analyzer;
	}

	public boolean isInitialized() {
		return analyzer != null;
	}

	public void initialize() {
		if ( this.analyzer != null ) {
			throw new AssertionFailure( "An Elasticsearch analyzer reference has been initialized more than once: " + this );
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
	public Builder startCopy() {
		return new CopyBuilder( builder );
	}

	public abstract static class AbstractBuilder implements ScopedAnalyzerReference.Builder {

		private ElasticsearchAnalyzerReference globalAnalyzerReference;
		private final Map<String, ElasticsearchAnalyzerReference> scopedAnalyzerReferences = new HashMap<>();

		protected AbstractBuilder(AbstractBuilder copied) {
			this( copied.globalAnalyzerReference, copied.scopedAnalyzerReferences );
		}

		public AbstractBuilder(ElasticsearchAnalyzerReference globalAnalyzerReference,
				Map<String, ElasticsearchAnalyzerReference> scopedAnalyzers) {
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
		public abstract ScopedElasticsearchAnalyzerReference build();

		protected final ScopedElasticsearchAnalyzer buildAnalyzer() {
			ElasticsearchAnalyzer globalAnalyzer = globalAnalyzerReference.getAnalyzer();

			Map<String, ElasticsearchAnalyzer> scopedAnalyzers = new HashMap<>();

			for ( Map.Entry<String, ElasticsearchAnalyzerReference> entry : scopedAnalyzerReferences.entrySet() ) {
				scopedAnalyzers.put( entry.getKey(), entry.getValue().getAnalyzer() );
			}

			return new ScopedElasticsearchAnalyzer( globalAnalyzer, scopedAnalyzers );
		}
	}

	/**
	 * A builder that defers the actual analyzer creation to later during the search
	 * factory initialization, so that the builder accepts dangling references.
	 *
	 * @author Yoann Rodiere
	 */
	public static class DeferredInitializationBuilder extends AbstractBuilder {

		public DeferredInitializationBuilder(ElasticsearchAnalyzerReference globalAnalyzerReference,
				Map<String, ElasticsearchAnalyzerReference> scopedAnalyzers) {
			super( globalAnalyzerReference, scopedAnalyzers );
		}

		@Override
		public ScopedElasticsearchAnalyzerReference build() {
			return new ScopedElasticsearchAnalyzerReference( this );
		}
	}

	public static class CopyBuilder extends AbstractBuilder {

		private CopyBuilder(AbstractBuilder copied) {
			super( copied );
		}

		@Override
		public ScopedElasticsearchAnalyzerReference build() {
			ScopedElasticsearchAnalyzer analyzer = buildAnalyzer();
			return new ScopedElasticsearchAnalyzerReference( this, analyzer );
		}

	}

	private static ElasticsearchAnalyzerReference getElasticsearchAnalyzerReference(AnalyzerReference analyzerReference) {
		if ( !analyzerReference.is( ElasticsearchAnalyzerReference.class ) ) {
			throw LOG.analyzerReferenceIsNotRemote( analyzerReference );
		}

		return analyzerReference.unwrap( ElasticsearchAnalyzerReference.class );
	}

}
