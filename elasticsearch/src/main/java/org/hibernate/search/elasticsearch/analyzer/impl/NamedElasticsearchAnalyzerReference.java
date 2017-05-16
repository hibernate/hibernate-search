/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.elasticsearch.settings.impl.model.AnalyzerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenizerDefinition;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A reference to an Elasticsearch analyzer that also provides a name for this analyzer.
 * <p>
 * Such a reference may initially only contain the analyzer name, but in this case
 * it will be fully initialized with the actual analyzer later.
 *
 * @author Yoann Rodiere
 */
public class NamedElasticsearchAnalyzerReference extends ElasticsearchAnalyzerReference {

	private static final Log LOG = LoggerFactory.make();

	private final String name;

	private ElasticsearchAnalyzer analyzer;

	public NamedElasticsearchAnalyzerReference(String name) {
		this.name = name;
		this.analyzer = null; // Not initialized yet
	}

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

	public void initialize(ElasticsearchAnalysisDefinitionRegistry definitionRegistry) {
		if ( this.analyzer != null ) {
			throw new AssertionFailure( "A named analyzer reference has been initialized more than once: " + this );
		}
		this.analyzer = createAnalyzer( definitionRegistry );
	}

	private ElasticsearchAnalyzer createAnalyzer(ElasticsearchAnalysisDefinitionRegistry definitionRegistry) {
		AnalyzerDefinition analyzerDefinition = definitionRegistry.getAnalyzerDefinition( name );
		if ( analyzerDefinition == null ) {
			return new UndefinedElasticsearchAnalyzerImpl( name );
		}

		String tokenizerName = analyzerDefinition.getTokenizer();
		TokenizerDefinition tokenizerDefinition = definitionRegistry.getTokenizerDefinition( tokenizerName );

		Map<String, TokenFilterDefinition> tokenFilters = new TreeMap<>();
		if ( analyzerDefinition.getTokenFilters() != null ) {
			for ( String name : analyzerDefinition.getTokenFilters() ) {
				TokenFilterDefinition definition = definitionRegistry.getTokenFilterDefinition( name );
				if ( definition != null ) { // Ignore missing definitions: they may be already available on the server
					tokenFilters.put( name, definition );
				}
			}
		}

		Map<String, CharFilterDefinition> charFilters = new TreeMap<>();
		if ( analyzerDefinition.getCharFilters() != null ) {
			for ( String name : analyzerDefinition.getCharFilters() ) {
				CharFilterDefinition definition = definitionRegistry.getCharFilterDefinition( name );
				if ( definition != null ) { // Ignore missing definitions: they may be already available on the server
					charFilters.put( name, definition );
				}
			}
		}

		return new CustomElasticsearchAnalyzerImpl(
				name, analyzerDefinition,
				tokenizerName, tokenizerDefinition,
				charFilters, tokenFilters );
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
		sb.append( name );
		sb.append( "," );
		sb.append( analyzer );
		sb.append( ">" );
		return sb.toString();
	}
}
