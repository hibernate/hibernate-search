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
import org.hibernate.search.analyzer.spi.ScopedAnalyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A {@code ScopedElasticsearchAnalyzer} is a wrapper class containing all remote analyzers for a given class.
 *
 * {@code ScopedElasticsearchAnalyzer} behaves similar to {@code ElasticsearchAnalyzerImpl} but delegates requests for name
 * to the underlying {@code ElasticsearchAnalyzerImpl} depending on the requested field name.
 *
 * @author Guillaume Smet
 */
public class ScopedElasticsearchAnalyzer implements ElasticsearchAnalyzer, ScopedAnalyzer {

	private static final Log log = LoggerFactory.make();

	private final ElasticsearchAnalyzerReference globalAnalyzerReference;
	private final Map<String, ElasticsearchAnalyzerReference> scopedAnalyzers = new HashMap<>();

	public ScopedElasticsearchAnalyzer(ElasticsearchAnalyzerReference globalAnalyzerReference) {
		this.globalAnalyzerReference = globalAnalyzerReference;
	}

	public ScopedElasticsearchAnalyzer(Builder builder) {
		this.globalAnalyzerReference = builder.globalAnalyzerReference;
		this.scopedAnalyzers.putAll( builder.scopedAnalyzers );
	}

	private ElasticsearchAnalyzerReference getDelegate(String fieldName) {
		ElasticsearchAnalyzerReference analyzerReference = scopedAnalyzers.get( fieldName );
		if ( analyzerReference == null ) {
			analyzerReference = globalAnalyzerReference;
		}
		return analyzerReference;
	}

	@Override
	public String getName(String fieldName) {
		return getDelegate( fieldName ).getAnalyzer().getName( fieldName );
	}

	@Override
	public AnalyzerDef getDefinition(String fieldName) {
		return getDelegate( fieldName ).getAnalyzer().getDefinition( fieldName );
	}

	@Override
	public void close() {
		// nothing to close
	}

	@Override
	public Builder startCopy() {
		return new Builder( globalAnalyzerReference, scopedAnalyzers );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( globalAnalyzerReference );
		sb.append( "," );
		sb.append( scopedAnalyzers );
		sb.append( ">" );
		return sb.toString();
	}

	public static class Builder implements ScopedAnalyzer.Builder {

		private ElasticsearchAnalyzerReference globalAnalyzerReference;
		private final Map<String, ElasticsearchAnalyzerReference> scopedAnalyzers = new HashMap<>();

		public Builder(ElasticsearchAnalyzerReference globalAnalyzerReference, Map<String, ElasticsearchAnalyzerReference> scopedAnalyzers) {
			this.globalAnalyzerReference = globalAnalyzerReference;
			this.scopedAnalyzers.putAll( scopedAnalyzers );
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
			scopedAnalyzers.put( scope, getElasticsearchAnalyzerReference( analyzerReference ) );
		}

		@Override
		public ScopedElasticsearchAnalyzerReference build() {
			ScopedElasticsearchAnalyzer analyzer = new ScopedElasticsearchAnalyzer( this );
			return new ScopedElasticsearchAnalyzerReference( analyzer );
		}
	}

	private static ElasticsearchAnalyzerReference getElasticsearchAnalyzerReference(AnalyzerReference analyzerReference) {
		if ( !analyzerReference.is( ElasticsearchAnalyzerReference.class ) ) {
			throw log.analyzerReferenceIsNotRemote( analyzerReference );
		}

		return analyzerReference.unwrap( ElasticsearchAnalyzerReference.class );
	}

}
