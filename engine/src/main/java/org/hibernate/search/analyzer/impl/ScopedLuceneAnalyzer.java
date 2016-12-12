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
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.ScopedAnalyzer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A {@code ScopedLuceneAnalyzer} is a wrapper class containing all analyzers for a given class.
 *
 * {@code ScopedLuceneAnalyzer} behaves similar to {@link org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper}
 * by delegating requests for {@code TokenStream}s to the underlying {@code Analyzer} depending on the requested field name.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public final class ScopedLuceneAnalyzer extends AnalyzerWrapper implements ScopedAnalyzer {

	private static final Log log = LoggerFactory.make();

	private final LuceneAnalyzerReference globalAnalyzerReference;
	private final Map<String, LuceneAnalyzerReference> scopedAnalyzers = new HashMap<>();

	public ScopedLuceneAnalyzer(Analyzer globalAnalyzer) {
		this( new LuceneAnalyzerReference( globalAnalyzer ) );
	}

	public ScopedLuceneAnalyzer(LuceneAnalyzerReference globalAnalyzerReference) {
		super( PER_FIELD_REUSE_STRATEGY );
		this.globalAnalyzerReference = globalAnalyzerReference;
	}

	public ScopedLuceneAnalyzer(Builder builder) {
		super( PER_FIELD_REUSE_STRATEGY );
		this.globalAnalyzerReference = builder.globalAnalyzerReference;
		this.scopedAnalyzers.putAll( builder.scopedAnalyzers );
	}

	/**
	 * Compares the references of the global analyzer backing this ScopedAnalyzer
	 * and each scoped analyzer.
	 * @param other ScopedAnalyzer to compare to
	 * @return true if and only if the same instance of global analyzer is being used
	 * and all scoped analyzers also match, by reference.
	 */
	public boolean isCompositeOfSameInstances(ScopedLuceneAnalyzer other) {
		if ( this.globalAnalyzerReference.getAnalyzer() != other.globalAnalyzerReference.getAnalyzer() ) {
			return false;
		}
		if ( this.scopedAnalyzers.size() != other.scopedAnalyzers.size() ) {
			return false;
		}
		for ( String fieldname : scopedAnalyzers.keySet() ) {
			if ( this.scopedAnalyzers.get( fieldname ) != other.scopedAnalyzers.get( fieldname ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected Analyzer getWrappedAnalyzer(String fieldName) {
		LuceneAnalyzerReference analyzerReference = scopedAnalyzers.get( fieldName );
		if ( analyzerReference == null ) {
			analyzerReference = globalAnalyzerReference;
		}
		return analyzerReference.getAnalyzer();
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

		private LuceneAnalyzerReference globalAnalyzerReference;
		private final Map<String, LuceneAnalyzerReference> scopedAnalyzers = new HashMap<>();

		public Builder(LuceneAnalyzerReference globalAnalyzerReference, Map<String, LuceneAnalyzerReference> scopedAnalyzers) {
			this.globalAnalyzerReference = globalAnalyzerReference;
			this.scopedAnalyzers.putAll( scopedAnalyzers );
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
			scopedAnalyzers.put( scope, getLuceneAnalyzerReference( analyzerReference ) );
		}

		@Override
		public ScopedLuceneAnalyzerReference build() {
			ScopedLuceneAnalyzer analyzer = new ScopedLuceneAnalyzer( this );
			return new ScopedLuceneAnalyzerReference( analyzer );
		}
	}

	private static LuceneAnalyzerReference getLuceneAnalyzerReference(AnalyzerReference analyzerReference) {
		if ( !analyzerReference.is( LuceneAnalyzerReference.class ) ) {
			throw log.analyzerReferenceIsNotLucene( analyzerReference );
		}

		return analyzerReference.unwrap( LuceneAnalyzerReference.class );
	}

}
