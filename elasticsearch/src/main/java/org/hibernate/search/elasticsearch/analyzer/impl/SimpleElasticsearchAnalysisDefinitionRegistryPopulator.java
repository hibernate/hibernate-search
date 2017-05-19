/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import java.util.Map;
import java.util.Objects;

import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistryPopulator;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalyzerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.NormalizerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenizerDefinition;

/**
 * A populator of an Elasticsearch analysis definitions provided explicitly.
 *
 * @author Guillaume Smet
 * @author Yoann Rodiere
 */
public class SimpleElasticsearchAnalysisDefinitionRegistryPopulator implements ElasticsearchAnalysisDefinitionRegistryPopulator {

	private final String analyzerName;
	private final AnalyzerDefinition analyzerDefinition;
	private final TokenizerDefinition tokenizerDefinition;
	private final NormalizerDefinition normalizerDefinition;
	private final Map<String, CharFilterDefinition> charFilters;
	private final Map<String, TokenFilterDefinition> tokenFilters;

	public SimpleElasticsearchAnalysisDefinitionRegistryPopulator(String analyzerName,
			AnalyzerDefinition analyzerDefinition,
			String tokenizerName,
			TokenizerDefinition tokenizerDefinition,
			Map<String, CharFilterDefinition> charFilters,
			Map<String, TokenFilterDefinition> tokenFilters) {
		this( analyzerName,
				analyzerDefinition, tokenizerName, tokenizerDefinition,
				null,
				charFilters, tokenFilters );
	}

	public SimpleElasticsearchAnalysisDefinitionRegistryPopulator(String analyzerName,
			NormalizerDefinition normalizerDefinition,
			Map<String, CharFilterDefinition> charFilters,
			Map<String, TokenFilterDefinition> tokenFilters) {
		this( analyzerName,
				null, null, null,
				normalizerDefinition,
				charFilters, tokenFilters );
	}

	private SimpleElasticsearchAnalysisDefinitionRegistryPopulator(String analyzerName,
			AnalyzerDefinition analyzerDefinition,
			String tokenizerName,
			TokenizerDefinition tokenizerDefinition,
			NormalizerDefinition normalizerDefinition,
			Map<String, CharFilterDefinition> charFilters,
			Map<String, TokenFilterDefinition> tokenFilters) {
		super();
		this.analyzerName = analyzerName;
		this.analyzerDefinition = analyzerDefinition;
		this.tokenizerDefinition = tokenizerDefinition;
		this.normalizerDefinition = normalizerDefinition;
		this.charFilters = charFilters;
		this.tokenFilters = tokenFilters;
	}

	@Override
	public void populate(ElasticsearchAnalysisDefinitionRegistry registry) {
		if ( analyzerDefinition != null ) {
			registry.register( analyzerName, analyzerDefinition );
		}
		if ( tokenizerDefinition != null ) {
			registry.register( analyzerDefinition.getTokenizer(), tokenizerDefinition );
		}
		if ( normalizerDefinition != null ) {
			registry.register( analyzerName, normalizerDefinition );
		}
		for ( Map.Entry<String, CharFilterDefinition> entry : charFilters.entrySet() ) {
			registry.register( entry.getKey(), entry.getValue() );
		}
		for ( Map.Entry<String, TokenFilterDefinition> entry : tokenFilters.entrySet() ) {
			registry.register( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode( analyzerName );
		result = prime * result + Objects.hashCode( analyzerDefinition );
		result = prime * result + Objects.hashCode( tokenizerDefinition );
		result = prime * result + Objects.hashCode( normalizerDefinition );
		result = prime * result + Objects.hashCode( charFilters );
		result = prime * result + Objects.hashCode( tokenFilters );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof SimpleElasticsearchAnalysisDefinitionRegistryPopulator ) {
			SimpleElasticsearchAnalysisDefinitionRegistryPopulator other = (SimpleElasticsearchAnalysisDefinitionRegistryPopulator) obj;
			return Objects.equals( analyzerName, other.analyzerName )
					&& Objects.equals( analyzerDefinition, other.analyzerDefinition )
					&& Objects.equals( tokenizerDefinition, other.tokenizerDefinition )
					&& Objects.equals( normalizerDefinition, other.normalizerDefinition )
					&& Objects.equals( charFilters, other.charFilters )
					&& Objects.equals( tokenFilters, other.tokenFilters );
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( analyzerName );
		sb.append( ", " );
		sb.append( analyzerDefinition != null ? analyzerDefinition : normalizerDefinition );
		sb.append( ">" );
		return sb.toString();
	}

}
