/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import org.hibernate.search.analyzer.impl.LuceneAnalyzerReference;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.analyzer.impl.ScopedAnalyzer;
import org.hibernate.search.analyzer.impl.ScopedRemoteAnalyzer;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A reference to a {@code ScopedAnalyzer} - either a Lucene one or a remote one.
 *
 * @author Davide D'Alto
 * @author Guillaume Smet
 */
public final class ScopedAnalyzerReference implements AnalyzerReference {

	private static final Log log = LoggerFactory.make();

	private AnalyzerReference globalAnalyzerReference;
	private final ScopedAnalyzer scopedAnalyzer;

	private ScopedAnalyzerReference(AnalyzerReference globalAnalyzerReference, ScopedAnalyzer scopedAnalyzer) {
		this.globalAnalyzerReference = globalAnalyzerReference;
		this.scopedAnalyzer = scopedAnalyzer;
	}

	@Override
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		if ( LuceneAnalyzerReference.class.isAssignableFrom( analyzerType ) ) {
			if ( scopedAnalyzer instanceof ScopedLuceneAnalyzer ) {
				return true;
			}
		}
		else if ( RemoteAnalyzerReference.class.isAssignableFrom( analyzerType ) ) {
			if ( scopedAnalyzer instanceof ScopedRemoteAnalyzer ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public <T extends AnalyzerReference> T unwrap(Class<T> analyzerType) {
		if ( LuceneAnalyzerReference.class.isAssignableFrom( analyzerType ) ) {
			if ( !( scopedAnalyzer instanceof ScopedLuceneAnalyzer ) ) {
				throw log.scopedAnalyzerIsNotLucene( scopedAnalyzer );
			}
			return analyzerType.cast( new LuceneAnalyzerReference( (ScopedLuceneAnalyzer) scopedAnalyzer ) );
		}
		else if ( RemoteAnalyzerReference.class.isAssignableFrom( analyzerType ) ) {
			if ( !( scopedAnalyzer instanceof ScopedRemoteAnalyzer ) ) {
				throw log.scopedAnalyzerIsNotRemote( scopedAnalyzer );
			}
			return analyzerType.cast( new RemoteAnalyzerReference( (ScopedRemoteAnalyzer) scopedAnalyzer ) );
		}
		return analyzerType.cast( this );
	}

	@Override
	public void close() {
		scopedAnalyzer.close();
	}

	/**
	 * Builds a new {@link ScopedAnalyzerReference}.
	 *
	 * @author Gunnar Morling
	 */
	public static class Builder {

		private AnalyzerReference globalAnalyzerReference;
		private final ScopedAnalyzer scopedAnalyzer;

		public Builder(AnalyzerReference globalAnalyzerReference) {
			if ( globalAnalyzerReference instanceof ScopedAnalyzerReference ) {
				ScopedAnalyzerReference original = (ScopedAnalyzerReference) globalAnalyzerReference;
				this.globalAnalyzerReference = original.globalAnalyzerReference;
				this.scopedAnalyzer = original.scopedAnalyzer.clone();
			}
			else {
				this.globalAnalyzerReference = globalAnalyzerReference;
				if ( globalAnalyzerReference instanceof RemoteAnalyzerReference ) {
					this.scopedAnalyzer = new ScopedRemoteAnalyzer( globalAnalyzerReference );
				}
				else {
					this.scopedAnalyzer = new ScopedLuceneAnalyzer( globalAnalyzerReference );
				}
			}
		}

		public Builder addAnalyzerReference(String scope, AnalyzerReference analyzerReference) {
			scopedAnalyzer.addScopedAnalyzerReference( scope, analyzerReference );
			return this;
		}

		public Builder setGlobalAnalyzerReference(AnalyzerReference globalAnalyzerReference) {
			this.globalAnalyzerReference = globalAnalyzerReference;
			scopedAnalyzer.setGlobalAnalyzerReference( globalAnalyzerReference );
			return this;
		}

		public AnalyzerReference getGlobalAnalyzerReference() {
			return globalAnalyzerReference;
		}

		public ScopedAnalyzerReference build() {
			return new ScopedAnalyzerReference( globalAnalyzerReference, scopedAnalyzer );
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( "scopedAnalyzer: " );
		sb.append( scopedAnalyzer );
		sb.append( ">" );
		return sb.toString();
	}
}
