/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import java.util.Collections;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;

/**
 * A {@code ScopedLuceneAnalyzer} is a wrapper class containing all analyzers for a given class.
 *
 * {@code ScopedLuceneAnalyzer} behaves similar to {@link org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper}
 * by delegating requests for {@code TokenStream}s to the underlying {@code Analyzer} depending on the requested field name.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public final class ScopedLuceneAnalyzer extends AnalyzerWrapper {

	private final LuceneAnalyzerReference globalAnalyzerReference;
	private final Map<String, LuceneAnalyzerReference> scopedAnalyzerReferences;

	public ScopedLuceneAnalyzer(Analyzer globalAnalyzer) {
		this( new SimpleLuceneAnalyzerReference( globalAnalyzer ), Collections.<String, LuceneAnalyzerReference>emptyMap() );
	}

	// For package use only; assumes the map is immutable
	ScopedLuceneAnalyzer(LuceneAnalyzerReference globalAnalyzerReference,
			Map<String, LuceneAnalyzerReference> scopedAnalyzerReferences) {
		super( PER_FIELD_REUSE_STRATEGY );
		this.globalAnalyzerReference = globalAnalyzerReference;
		this.scopedAnalyzerReferences = scopedAnalyzerReferences;
	}

	/**
	 * Compares the references of the global analyzer backing this ScopedAnalyzer
	 * and each scoped analyzer.
	 * @param other ScopedAnalyzer to compare to
	 * @return true if and only if the same instance of global analyzer is being used
	 * and all scoped analyzers also match, by reference.
	 */
	public boolean isCompositeOfSameInstances(ScopedLuceneAnalyzer other) {
		if ( toAnalyzer( this.globalAnalyzerReference ) != toAnalyzer( other.globalAnalyzerReference ) ) {
			return false;
		}
		if ( this.scopedAnalyzerReferences.size() != other.scopedAnalyzerReferences.size() ) {
			return false;
		}
		for ( String fieldname : scopedAnalyzerReferences.keySet() ) {
			if ( toAnalyzer( this.scopedAnalyzerReferences.get( fieldname ) ) != toAnalyzer( other.scopedAnalyzerReferences.get( fieldname ) ) ) {
				return false;
			}
		}
		return true;
	}

	private Analyzer toAnalyzer(LuceneAnalyzerReference reference) {
		return reference == null ? null : reference.getAnalyzer();
	}

	@Override
	protected Analyzer getWrappedAnalyzer(String fieldName) {
		LuceneAnalyzerReference analyzerReference = scopedAnalyzerReferences.get( fieldName );
		if ( analyzerReference == null ) {
			analyzerReference = globalAnalyzerReference;
		}
		return analyzerReference.getAnalyzer();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( globalAnalyzerReference );
		sb.append( "," );
		sb.append( scopedAnalyzerReferences );
		sb.append( ">" );
		return sb.toString();
	}

}
