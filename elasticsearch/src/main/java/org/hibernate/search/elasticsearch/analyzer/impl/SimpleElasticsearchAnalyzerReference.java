/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.analyzer.spi.AnalyzerReference;

/**
 * A reference to an analyzer that is fully defined from the start.
 *
 * @author Yoann Rodiere
 */
public class SimpleElasticsearchAnalyzerReference extends ElasticsearchAnalyzerReference {

	private ElasticsearchAnalyzer analyzer;

	public SimpleElasticsearchAnalyzerReference(ElasticsearchAnalyzer analyzer) {
		this.analyzer = analyzer;
	}

	@Override
	public ElasticsearchAnalyzer getAnalyzer() {
		return analyzer;
	}

	@Override
	public void close() {
		if ( analyzer != null ) {
			analyzer.close();
		}
	}

	@Override
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		return analyzerType.isAssignableFrom( SimpleElasticsearchAnalyzerReference.class );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( analyzer );
		sb.append( ">" );
		return sb.toString();
	}
}
