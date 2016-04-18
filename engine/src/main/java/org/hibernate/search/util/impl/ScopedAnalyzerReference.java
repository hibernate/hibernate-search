/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.analyzer.impl.AnalyzerReference;
import org.hibernate.search.analyzer.impl.LuceneAnalyzerReference;

/**
 * A wrapper containing all the analyzers for a given class.
 *
 * @author Davide D'Alto
 */
public final class ScopedAnalyzerReference implements AnalyzerReference {

	private final Map<String, AnalyzerReference> analyzerReferences;
	private final AnalyzerReference globalAnalyzerReference;

	public ScopedAnalyzerReference(AnalyzerReference globalAnalyzerReference) {
		this( globalAnalyzerReference, Collections.<String, AnalyzerReference>emptyMap() );
	}

	private ScopedAnalyzerReference(AnalyzerReference globalAnalyzerReference, Map<String, AnalyzerReference> analyzerReferences) {
		this.globalAnalyzerReference = globalAnalyzerReference;
		this.analyzerReferences = Collections.unmodifiableMap( analyzerReferences );
	}

	private static Analyzer luceneAnalyzer(AnalyzerReference scopedAnalyzer) {
		Analyzer analyzer = scopedAnalyzer.unwrap( LuceneAnalyzerReference.class ).getAnalyzer();
		return analyzer;
	}

	/**
	 * Compares the references of the global analyzer backing this ScopedAnalyzer and each scoped analyzer.
	 *
	 * @param other ScopedAnalyzer to compare to
	 * @return true if and only if the same instance of global analyzer is being used and all scoped analyzers also
	 * match, by reference.
	 */
	public boolean isCompositeOfSameInstances(ScopedAnalyzerReference other) {
		if ( this.globalAnalyzerReference != other.globalAnalyzerReference ) {
			return false;
		}
		if ( this.analyzerReferences.size() != other.analyzerReferences.size() ) {
			return false;
		}
		for ( String fieldname : analyzerReferences.keySet() ) {
			if ( this.analyzerReferences.get( fieldname ) != other.analyzerReferences.get( fieldname ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		return ScopedAnalyzerReference.class.isAssignableFrom( analyzerType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends AnalyzerReference> T unwrap(Class<T> analyzerType) {
		if ( LuceneAnalyzerReference.class.isAssignableFrom( analyzerType ) ) {
			// There are places where we still have to pass a ScopedAnalyzer
			return (T) new LuceneAnalyzerReference( createScopedAnalyzer( globalAnalyzerReference, analyzerReferences ) );
		}
		return (T) this;
	}

	private static ScopedAnalyzer createScopedAnalyzer(AnalyzerReference globalAnalyzer, Map<String, AnalyzerReference> analyzers) {
		final ScopedAnalyzer analyzer = createScopedAnalyzer( globalAnalyzer );
		for ( Entry<String, AnalyzerReference> entry : analyzers.entrySet() ) {
			if ( entry.getValue().is( LuceneAnalyzerReference.class ) ) {
				analyzer.addScopedAnalyzer( entry.getKey(), luceneAnalyzer( entry.getValue() ) );
			}
		}
		return analyzer;
	}

	private static ScopedAnalyzer createScopedAnalyzer(AnalyzerReference globalAnalyzer) {
		// XXX GSM: weird
		final Analyzer analyzer = globalAnalyzer.is( LuceneAnalyzerReference.class )
				? luceneAnalyzer( globalAnalyzer )
				: null;
		return new ScopedAnalyzer( analyzer );
	}

	@Override
	public void close() {
		// we don't close the underlying {@code AnalyzerReference}s as they might be used by other
		// {@code ScopedAnalyzerReference}. It is especially true for the reference to the
		// PassThroughAnalyzer which is shared statically.
	}

	/**
	 * Builds a new {@link ScopedAnalyzerReference}.
	 *
	 * @author Gunnar Morling
	 */
	public static class Builder {

		private final Map<String, AnalyzerReference> analyzerReferences;
		private AnalyzerReference globalAnalyzerReference;

		public Builder(ScopedAnalyzerReference original) {
			this.analyzerReferences = new HashMap<>( original.analyzerReferences );
			this.globalAnalyzerReference = original.globalAnalyzerReference;
		}

		public Builder(AnalyzerReference globalAnalyzerReference) {
			analyzerReferences = new HashMap<>();
			this.globalAnalyzerReference = globalAnalyzerReference;
		}

		public Builder addAnalyzerReference(String scope, AnalyzerReference analyzerReference) {
			analyzerReferences.put( scope, analyzerReference );
			return this;
		}

		public Builder setGlobalAnalyzerReference(AnalyzerReference globalAnalyzerReference) {
			this.globalAnalyzerReference = globalAnalyzerReference;
			return this;
		}

		public ScopedAnalyzerReference build() {
			return new ScopedAnalyzerReference( globalAnalyzerReference, analyzerReferences );
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( "globalAnalyzer: " );
		sb.append( globalAnalyzerReference );
		sb.append( ">" );
		return sb.toString();
	}
}
