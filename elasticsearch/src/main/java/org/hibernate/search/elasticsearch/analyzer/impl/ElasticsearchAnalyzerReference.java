/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchAnalyzerReference extends RemoteAnalyzerReference {

	private static final Log LOG = LoggerFactory.make();

	private final String name;

	private ElasticsearchAnalyzer analyzer;

	public ElasticsearchAnalyzerReference(String name) {
		this.name = name;
		this.analyzer = null; // Not initialized
	}

	public ElasticsearchAnalyzerReference(ElasticsearchAnalyzer analyzer) {
		this.name = null;
		this.analyzer = analyzer;
	}

	@Override
	public String getAnalyzerName() {
		return name;
	}

	@Override
	public ElasticsearchAnalyzer getAnalyzer() {
		if ( analyzer == null ) {
			throw LOG.lazyRemoteAnalyzerReferenceNotInitialized( this );
		}
		return analyzer;
	}

	public boolean isInitialized() {
		return analyzer != null;
	}

	public void initialize(ElasticsearchAnalyzer analyzer) {
		if ( this.analyzer != null ) {
			throw new AssertionFailure( "An analyzer reference has been initialized more than once: " + this );
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
		return analyzerType.isAssignableFrom( ElasticsearchAnalyzerReference.class );
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
