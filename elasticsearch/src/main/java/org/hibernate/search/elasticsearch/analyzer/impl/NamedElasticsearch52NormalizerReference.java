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
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.NormalizerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;

/**
 * @author Yoann Rodiere
 */
public class NamedElasticsearch52NormalizerReference extends NamedElasticsearchAnalyzerReference {

	public NamedElasticsearch52NormalizerReference(String name) {
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

		Map<String, TokenFilterDefinition> tokenFilters =
				collectDefinitions( definitionRegistry::getTokenFilterDefinition, normalizerDefinition.getTokenFilters() );

		Map<String, CharFilterDefinition> charFilters =
				collectDefinitions( definitionRegistry::getCharFilterDefinition, normalizerDefinition.getCharFilters() );

		return new SimpleElasticsearchAnalysisDefinitionRegistryPopulator(
				name, normalizerDefinition,
				charFilters, tokenFilters );
	}

}