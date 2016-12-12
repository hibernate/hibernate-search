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
import org.hibernate.search.analyzer.impl.LuceneAnalyzerReference;
import org.hibernate.search.analyzer.impl.ScopedAnalyzer;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
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

	private Analyzer globalAnalyzer;
	private final Map<String, Analyzer> scopedAnalyzers = new HashMap<String, Analyzer>();

	public ScopedLuceneAnalyzer(Analyzer globalAnalyzer) {
		this( globalAnalyzer, Collections.<String, Analyzer>emptyMap() );
	}

	public ScopedLuceneAnalyzer(AnalyzerReference globalAnalyzerReference) {
		this( getLuceneAnalyzer( globalAnalyzerReference ), Collections.<String, Analyzer>emptyMap() );
	}

	private ScopedLuceneAnalyzer(Analyzer globalAnalyzer, Map<String, Analyzer> scopedAnalyzers) {
		super( PER_FIELD_REUSE_STRATEGY );
		this.globalAnalyzer = globalAnalyzer;
		this.scopedAnalyzers.putAll( scopedAnalyzers );
	}

	@Override
	public void setGlobalAnalyzerReference(AnalyzerReference globalAnalyzerReference) {
		this.globalAnalyzer = getLuceneAnalyzer( globalAnalyzerReference );
	}

	public void addScopedAnalyzer(String scope, Analyzer scopedAnalyzer) {
		scopedAnalyzers.put( scope, scopedAnalyzer );
	}

	@Override
	public void addScopedAnalyzerReference(String scope, AnalyzerReference analyzerReference) {
		scopedAnalyzers.put( scope, getLuceneAnalyzer( analyzerReference ) );
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
		final Analyzer analyzer = scopedAnalyzers.get( fieldName );
		if ( analyzer == null ) {
			return globalAnalyzer;
		}
		else {
			return analyzer;
		}
	}

	@Override
	public ScopedLuceneAnalyzer clone() {
		ScopedLuceneAnalyzer clone = new ScopedLuceneAnalyzer( globalAnalyzer, scopedAnalyzers );
		return clone;
	}

	private static Analyzer getLuceneAnalyzer(AnalyzerReference analyzerReference) {
		if ( !( analyzerReference instanceof LuceneAnalyzerReference ) ) {
			throw log.analyzerReferenceIsNotLucene( analyzerReference );
		}

		return ( (LuceneAnalyzerReference) analyzerReference ).getAnalyzer();
	}

}
