/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A reference to an {@link Analyzer}.
 *
 * @author Davide D'Alto
 */
public class LuceneAnalyzerReference implements AnalyzerReference {

	private static final Log LOG = LoggerFactory.make();

	private String name;

	private Analyzer analyzer;

	public LuceneAnalyzerReference(String name) {
		this.name = name;
		this.analyzer = null; // Not initialized
	}

	public LuceneAnalyzerReference(Analyzer analyzer) {
		this.name = null;
		this.analyzer = analyzer;
	}

	@Override
	public String getAnalyzerName() {
		return name;
	}

	@Override
	public Analyzer getAnalyzer() {
		if ( analyzer == null ) {
			throw LOG.lazyLuceneAnalyzerReferenceNotInitialized( this );
		}
		return analyzer;
	}

	public boolean isInitialized() {
		return analyzer != null;
	}

	public void initialize(Analyzer analyzer) {
		if ( this.analyzer != null ) {
			throw new AssertionFailure( "An analyzer reference has been initialized more than once:" + this );
		}
		this.analyzer = analyzer;
	}

	@Override
	public void close() {
		if ( analyzer != null ) {
			analyzer.close();
		}
	}

	@Override
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		return analyzerType.isAssignableFrom( LuceneAnalyzerReference.class );
	}

	@Override
	public <T extends AnalyzerReference> T unwrap(Class<T> analyzerType) {
		return analyzerType.cast( this );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		if ( analyzer != null ) {
			sb.append( analyzer );
		}
		else {
			sb.append( name );
		}
		sb.append( ">" );
		return sb.toString();
	}
}
