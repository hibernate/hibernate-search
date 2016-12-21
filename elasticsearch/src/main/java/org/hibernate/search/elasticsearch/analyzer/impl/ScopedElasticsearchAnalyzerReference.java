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

	private DeferredInitializationBuilder builder;

	public ScopedElasticsearchAnalyzerReference(ScopedElasticsearchAnalyzer analyzer) {
		this.builder = null;
		this.analyzer = analyzer; // Already initialized
	}

	private ScopedElasticsearchAnalyzerReference(DeferredInitializationBuilder builder) {
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

		private ElasticsearchAnalyzerReference globalAnalyzerReference;
		private final Map<String, ElasticsearchAnalyzerReference> scopedAnalyzerReferences = new HashMap<>();

		public DeferredInitializationBuilder(ElasticsearchAnalyzerReference globalAnalyzerReference,
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
		public ScopedElasticsearchAnalyzerReference build() {
			return new ScopedElasticsearchAnalyzerReference( this );
		}

		protected final ScopedElasticsearchAnalyzer buildAnalyzer() {
			ElasticsearchAnalyzer globalAnalyzer = globalAnalyzerReference.getAnalyzer();

			Map<String, ElasticsearchAnalyzer> scopedAnalyzers = new HashMap<>();
			for ( Map.Entry<String, ElasticsearchAnalyzerReference> entry : scopedAnalyzerReferences.entrySet() ) {
				scopedAnalyzers.put( entry.getKey(), entry.getValue().getAnalyzer() );
			}

			return new ScopedElasticsearchAnalyzer( globalAnalyzer, scopedAnalyzers );
		}
	}

	public static class CopyBuilder implements ScopedAnalyzerReference.CopyBuilder {

		private final ScopedElasticsearchAnalyzer baseAnalyzer;
		private final Map<String, ElasticsearchAnalyzer> scopedAnalyzersOverrides = new HashMap<>();

		protected CopyBuilder(ScopedElasticsearchAnalyzer baseAnalyzer) {
			this.baseAnalyzer = baseAnalyzer;
		}

		@Override
		public void addAnalyzerReference(String scope, AnalyzerReference analyzerReference) {
			scopedAnalyzersOverrides.put( scope, getElasticsearchAnalyzerReference( analyzerReference ).getAnalyzer() );
		}

		@Override
		public ScopedElasticsearchAnalyzerReference build() {
			ElasticsearchAnalyzer globalAnalyzer = baseAnalyzer.getGlobalAnalyzer();

			Map<String, ElasticsearchAnalyzer> scopedAnalyzers = new HashMap<>( baseAnalyzer.getScopedAnalyzers() );
			scopedAnalyzers.putAll( scopedAnalyzersOverrides );

			ScopedElasticsearchAnalyzer scopedAnalyzer = new ScopedElasticsearchAnalyzer( globalAnalyzer, scopedAnalyzers );

			return new ScopedElasticsearchAnalyzerReference( scopedAnalyzer );
		}
	}

	private static ElasticsearchAnalyzerReference getElasticsearchAnalyzerReference(AnalyzerReference analyzerReference) {
		if ( !analyzerReference.is( ElasticsearchAnalyzerReference.class ) ) {
			throw LOG.analyzerReferenceIsNotRemote( analyzerReference );
		}

		return analyzerReference.unwrap( ElasticsearchAnalyzerReference.class );
	}

}
