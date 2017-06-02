/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public class ScopedLuceneAnalyzerReference extends LuceneAnalyzerReference implements ScopedAnalyzerReference {

	private static final Log LOG = LoggerFactory.make();

	private final LuceneAnalyzerReference globalAnalyzerReference;
	private final Map<String, LuceneAnalyzerReference> scopedAnalyzerReferences;
	private final ScopedLuceneAnalyzer scopedAnalyzer;

	public ScopedLuceneAnalyzerReference(Builder builder) {
		this.globalAnalyzerReference = builder.globalAnalyzerReference;
		this.scopedAnalyzerReferences = Collections.unmodifiableMap( new HashMap<>( builder.scopedAnalyzerReferences ) );
		this.scopedAnalyzer = new ScopedLuceneAnalyzer( globalAnalyzerReference, scopedAnalyzerReferences );
	}

	@Override
	public ScopedLuceneAnalyzer getAnalyzer() {
		return scopedAnalyzer;
	}

	@Override
	public boolean isNormalizer(String fieldName) {
		return getDelegate( fieldName ).isNormalizer( fieldName );
	}

	@Override
	public void close() {
		getAnalyzer().close();
	}

	@Override
	public CopyBuilder startCopy() {
		return new Builder( globalAnalyzerReference, scopedAnalyzerReferences );
	}

	private LuceneAnalyzerReference getDelegate(String fieldName) {
		LuceneAnalyzerReference analyzerReference = scopedAnalyzerReferences.get( fieldName );
		if ( analyzerReference == null ) {
			analyzerReference = globalAnalyzerReference;
		}
		return analyzerReference;
	}

	public static class Builder implements ScopedAnalyzerReference.Builder, ScopedAnalyzerReference.CopyBuilder {

		private LuceneAnalyzerReference globalAnalyzerReference;
		private final Map<String, LuceneAnalyzerReference> scopedAnalyzerReferences = new HashMap<>();

		public Builder(LuceneAnalyzerReference globalAnalyzerReference,
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
	}

	private static LuceneAnalyzerReference getLuceneAnalyzerReference(AnalyzerReference analyzerReference) {
		if ( !analyzerReference.is( LuceneAnalyzerReference.class ) ) {
			throw LOG.analyzerReferenceIsNotLucene( analyzerReference );
		}

		return analyzerReference.unwrap( LuceneAnalyzerReference.class );
	}

}
