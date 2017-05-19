/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import java.util.Map;

import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistryPopulator;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalyzerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.NormalizerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenizerDefinition;

/**
 * @author Yoann Rodiere
 */
public class NamedElasticsearch2NormalizerReference extends NamedElasticsearchAnalyzerReference {

	public NamedElasticsearch2NormalizerReference(String name) {
		super( name );
	}

	@Override
	public boolean isNormalizer(String fieldName) {
		return true;
	}

	@Override
	protected ElasticsearchAnalysisDefinitionRegistryPopulator createRegistryPopulator(ElasticsearchAnalysisDefinitionRegistry definitionRegistry) {
		NormalizerDefinition normalizerDefinition = definitionRegistry.getNormalizerDefinition( name );
		if ( normalizerDefinition == null ) {
			return (r) -> { }; // No-op
		}

		AnalyzerDefinition analyzerDefinition = normalizerToAnalyzer( normalizerDefinition );

		String tokenizerName = analyzerDefinition.getTokenizer();
		TokenizerDefinition tokenizerDefinition = definitionRegistry.getTokenizerDefinition( tokenizerName );

		Map<String, TokenFilterDefinition> tokenFilters =
				collectDefinitions( definitionRegistry::getTokenFilterDefinition, analyzerDefinition.getTokenFilters() );

		Map<String, CharFilterDefinition> charFilters =
				collectDefinitions( definitionRegistry::getCharFilterDefinition, analyzerDefinition.getCharFilters() );

		return new SimpleElasticsearchAnalysisDefinitionRegistryPopulator(
				name, analyzerDefinition, tokenizerName, tokenizerDefinition,
				charFilters, tokenFilters );
	}

	private AnalyzerDefinition normalizerToAnalyzer(NormalizerDefinition definition) {
		AnalyzerDefinition analyzerDefinition = new AnalyzerDefinition();
		analyzerDefinition.setTokenizer( "keyword" );
		analyzerDefinition.setCharFilters( definition.getCharFilters() );
		analyzerDefinition.setTokenFilters( definition.getTokenFilters() );
		return analyzerDefinition;
	}

}
