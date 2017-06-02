/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A reference to an {@link Analyzer}.
 *
 * @author Davide D'Alto
 */
public class SimpleLuceneAnalyzerReference extends LuceneAnalyzerReference {

	private static final Log LOG = LoggerFactory.make();

	private Analyzer analyzer;

	public SimpleLuceneAnalyzerReference(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	@Override
	public Analyzer getAnalyzer() {
		if ( analyzer == null ) {
			throw LOG.lazyLuceneAnalyzerReferenceNotInitialized( this );
		}
		return analyzer;
	}

	@Override
	public boolean isNormalizer(String fieldName) {
		return false;
	}

	public boolean isInitialized() {
		return true;
	}

	@Override
	public void close() {
		analyzer.close();
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
