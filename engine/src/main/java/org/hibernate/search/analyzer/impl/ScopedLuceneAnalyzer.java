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

	private final Analyzer globalAnalyzer;
	private final Map<String, Analyzer> scopedAnalyzers;

	public ScopedLuceneAnalyzer(Analyzer globalAnalyzer) {
		this( globalAnalyzer, Collections.<String, Analyzer>emptyMap() );
	}

	public ScopedLuceneAnalyzer(Analyzer globalAnalyzer, Map<String, Analyzer> scopedAnalyzers) {
		super( PER_FIELD_REUSE_STRATEGY );
		this.globalAnalyzer = globalAnalyzer;
		this.scopedAnalyzers = Collections.unmodifiableMap( new HashMap<>( scopedAnalyzers ) );
	}

	Analyzer getGlobalAnalyzer() {
		return globalAnalyzer;
	}

	Map<String, Analyzer> getScopedAnalyzers() {
		return scopedAnalyzers;
	}

	/**
	 * Compares the references of the global analyzer backing this ScopedAnalyzer
	 * and each scoped analyzer.
	 * @param other ScopedAnalyzer to compare to
	 * @return true if and only if the same instance of global analyzer is being used
	 * and all scoped analyzers also match, by reference.
	 */
	public boolean isCompositeOfSameInstances(ScopedLuceneAnalyzer other) {
		if ( this.globalAnalyzer != other.globalAnalyzer ) {
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
		Analyzer analyzer = scopedAnalyzers.get( fieldName );
		if ( analyzer == null ) {
			analyzer = globalAnalyzer;
		}
		return analyzer;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( globalAnalyzer );
		sb.append( "," );
		sb.append( scopedAnalyzers );
		sb.append( ">" );
		return sb.toString();
	}

}
