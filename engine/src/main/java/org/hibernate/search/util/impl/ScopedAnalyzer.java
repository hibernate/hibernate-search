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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.hibernate.search.analyzer.impl.AnalyzerReference;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A {@code ScopedAnalyzer} is a wrapper class containing all analyzers for a given class.
 *
 * {@code ScopedAnalyzer} behaves similar to {@link org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper}
 * by delegating requests for {@code TokenStream}s to the underlying {@code Analyzer} depending on the requested field name.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public final class ScopedAnalyzer extends AnalyzerWrapper {

	private static final Log log = LoggerFactory.make();

	private AnalyzerReference globalAnalyzer;
	private final Map<String, AnalyzerReference> scopedAnalyzers = new HashMap<String, AnalyzerReference>();

	public ScopedAnalyzer(AnalyzerReference globalAnalyzer) {
		this( globalAnalyzer, Collections.<String, AnalyzerReference>emptyMap() );
	}

	private ScopedAnalyzer(AnalyzerReference globalAnalyzer, Map<String, AnalyzerReference> scopedAnalyzers) {
		super( PER_FIELD_REUSE_STRATEGY );
		this.globalAnalyzer = globalAnalyzer;
		for ( Map.Entry<String, AnalyzerReference> entry : scopedAnalyzers.entrySet() ) {
			addScopedAnalyzer( entry.getKey(), entry.getValue() );
		}
	}

	public void setGlobalAnalyzer(AnalyzerReference globalAnalyzer) {
		this.globalAnalyzer = globalAnalyzer;
	}

	public void addScopedAnalyzer(String scope, AnalyzerReference scopedAnalyzer) {
		scopedAnalyzers.put( scope, scopedAnalyzer );
	}

	/**
	 * Compares the references of the global analyzer backing this ScopedAnalyzer
	 * and each scoped analyzer.
	 * @param other ScopedAnalyzer to compare to
	 * @return true if and only if the same instance of global analyzer is being used
	 * and all scoped analyzers also match, by reference.
	 */
	public boolean isCompositeOfSameInstances(ScopedAnalyzer other) {
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
		final AnalyzerReference analyzer = scopedAnalyzers.get( fieldName );
		if ( analyzer == null ) {
			return globalAnalyzer.getAnalyzer();
		}
		else if ( analyzer.getAnalyzer() != null ) {
			return analyzer.getAnalyzer();
		}
		else if ( analyzer.getRemote() != null ) {
			// Remote analyzer don't have an implementation
			return PassThroughAnalyzer.INSTANCE;
		}
		else {
			throw log.analyzerReferenceNotInitialized( fieldName, analyzer.getName() );
		}
	}

	@Override
	public ScopedAnalyzer clone() {
		ScopedAnalyzer clone = new ScopedAnalyzer( globalAnalyzer, scopedAnalyzers );
		return clone;
	}

}
