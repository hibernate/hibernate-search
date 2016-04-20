/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A Lucene {@code Analyzer} loaded lazily.
 *
 * @author Guillaume Smet
 */
public class LazyLuceneAnalyzer extends AnalyzerWrapper {

	private static final Log log = LoggerFactory.make();

	private final String name;
	private Analyzer delegate;

	public LazyLuceneAnalyzer(String name) {
		super( Analyzer.GLOBAL_REUSE_STRATEGY );
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public void close() {
		if ( delegate != null ) {
			delegate.close();
		}
	}

	public void setDelegate(Analyzer analyzer) {
		this.delegate = analyzer;
	}

	@Override
	protected Analyzer getWrappedAnalyzer(String fieldName) {
		validate();
		return delegate;
	}

	private void validate() {
		if ( delegate == null ) {
			throw log.lazyLuceneAnalyzerNotInitialized( this );
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( name );
		if ( delegate != null ) {
			sb.append( ", delegate: " );
			sb.append( delegate );
		}
		sb.append( ">" );
		return sb.toString();
	}

}
