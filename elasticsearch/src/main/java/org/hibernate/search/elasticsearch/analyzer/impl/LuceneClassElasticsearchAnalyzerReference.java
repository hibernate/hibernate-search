/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.elasticsearch.settings.impl.translation.ElasticsearchAnalyzerDefinitionTranslator;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A reference to an analyzer that should be translated from a Lucene class.
 *
 * @author Yoann Rodiere
 */
public class LuceneClassElasticsearchAnalyzerReference extends ElasticsearchAnalyzerReference {

	private static final Log LOG = LoggerFactory.make();

	private final Class<?> luceneClass;

	private ElasticsearchAnalyzer analyzer;

	public LuceneClassElasticsearchAnalyzerReference(Class<?> luceneClass) {
		this.luceneClass = luceneClass;
		this.analyzer = null; // Not initialized yet
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

	public void initialize(ElasticsearchAnalyzerDefinitionTranslator translator) {
		if ( this.analyzer != null ) {
			throw new AssertionFailure( "A Lucene class analyzer reference has been initialized more than once: " + this );
		}
		String name = translator.translate( luceneClass );
		this.analyzer = new UndefinedElasticsearchAnalyzerImpl( name );
	}

	@Override
	public void close() {
		if ( analyzer != null ) {
			analyzer.close();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( luceneClass );
		sb.append( "," );
		sb.append( analyzer );
		sb.append( ">" );
		return sb.toString();
	}
}
