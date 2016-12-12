/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.ScopedAnalyzer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A {@code ScopedRemoteAnalyzer} is a wrapper class containing all remote analyzers for a given class.
 *
 * {@code ScopedRemoteAnalyzer} behaves similar to {@code RemoteAnalyzer} but delegates requests for name
 * to the underlying {@code RemoteAnalyzer} depending on the requested field name.
 *
 * @author Guillaume Smet
 */
public class ScopedRemoteAnalyzer implements RemoteAnalyzer, ScopedAnalyzer {

	private static final Log log = LoggerFactory.make();

	private final RemoteAnalyzerReference globalAnalyzerReference;
	private final Map<String, RemoteAnalyzerReference> scopedAnalyzers = new HashMap<>();

	public ScopedRemoteAnalyzer(RemoteAnalyzerReference globalAnalyzerReference) {
		this.globalAnalyzerReference = globalAnalyzerReference;
	}

	public ScopedRemoteAnalyzer(Builder builder) {
		this.globalAnalyzerReference = builder.globalAnalyzerReference;
		this.scopedAnalyzers.putAll( builder.scopedAnalyzers );
	}

	@Override
	public String getName(String fieldName) {
		RemoteAnalyzerReference analyzerReference = scopedAnalyzers.get( fieldName );
		if ( analyzerReference == null ) {
			analyzerReference = globalAnalyzerReference;
		}
		return analyzerReference.getAnalyzer().getName( fieldName );
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

		private RemoteAnalyzerReference globalAnalyzerReference;
		private final Map<String, RemoteAnalyzerReference> scopedAnalyzers = new HashMap<>();

		public Builder(RemoteAnalyzerReference globalAnalyzerReference, Map<String, RemoteAnalyzerReference> scopedAnalyzers) {
			this.globalAnalyzerReference = globalAnalyzerReference;
			this.scopedAnalyzers.putAll( scopedAnalyzers );
		}

		@Override
		public RemoteAnalyzerReference getGlobalAnalyzerReference() {
			return globalAnalyzerReference;
		}

		@Override
		public void setGlobalAnalyzerReference(AnalyzerReference globalAnalyzerReference) {
			this.globalAnalyzerReference = getRemoteAnalyzerReference( globalAnalyzerReference );
		}

		@Override
		public void addAnalyzerReference(String scope, AnalyzerReference analyzerReference) {
			scopedAnalyzers.put( scope, getRemoteAnalyzerReference( analyzerReference ) );
		}

		@Override
		public ScopedRemoteAnalyzerReference build() {
			ScopedRemoteAnalyzer analyzer = new ScopedRemoteAnalyzer( this );
			return new ScopedRemoteAnalyzerReference( analyzer );
		}
	}

	private static RemoteAnalyzerReference getRemoteAnalyzerReference(AnalyzerReference analyzerReference) {
		if ( !analyzerReference.is( RemoteAnalyzerReference.class ) ) {
			throw log.analyzerReferenceIsNotRemote( analyzerReference );
		}

		return analyzerReference.unwrap( RemoteAnalyzerReference.class );
	}

}
