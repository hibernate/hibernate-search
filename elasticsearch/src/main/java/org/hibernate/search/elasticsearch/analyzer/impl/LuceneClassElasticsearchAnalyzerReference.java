/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistry;
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

	private String name;

	public LuceneClassElasticsearchAnalyzerReference(Class<?> luceneClass) {
		this.luceneClass = luceneClass;
		this.name = null; // Not initialized yet
	}

	@Override
	public String getAnalyzerName(String fieldName) {
		if ( name == null ) {
			throw LOG.lazyRemoteAnalyzerReferenceNotInitialized( this );
		}
		return name;
	}

	@Override
	public boolean isNormalizer(String fieldName) {
		return false;
	}

	@Override
	public void registerDefinitions(String fieldName, ElasticsearchAnalysisDefinitionRegistry definitionRegistry) {
		// Nothing to do
	}

	@Override
	public boolean isInitialized() {
		return name != null;
	}

	@Override
	public void initialize(ElasticsearchAnalysisDefinitionRegistry definitionRegistry, ElasticsearchAnalyzerDefinitionTranslator translator) {
		if ( this.name != null ) {
			throw new AssertionFailure( "A Lucene class analyzer reference has been initialized more than once: " + this );
		}
		this.name = translator.translate( luceneClass );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( luceneClass );
		sb.append( "," );
		sb.append( name );
		sb.append( ">" );
		return sb.toString();
	}
}
