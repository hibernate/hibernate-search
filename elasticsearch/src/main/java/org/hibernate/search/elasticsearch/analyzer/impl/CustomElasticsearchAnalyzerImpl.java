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
import org.hibernate.search.elasticsearch.settings.impl.model.AnalyzerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenizerDefinition;

/**
 * A description of an Elasticsearch analyzer built through an analyzer definition.
 * <p>
 * This implementation is used whenever {@code @Analyzer(definition = "foo")} is encountered
 * and an {@code @AnalyzerDefinition} exists with the given name ("foo" in this example).
 *
 * @author Guillaume Smet
 * @author Yoann Rodiere
 */
public class CustomElasticsearchAnalyzerImpl implements ElasticsearchAnalyzer {

	private final String analyzerName;
	private final AnalyzerDefinition analyzerDefinition;
	private final TokenizerDefinition tokenizerDefinition;
	private final Map<String, CharFilterDefinition> charFilters;
	private final Map<String, TokenFilterDefinition> tokenFilters;

	public CustomElasticsearchAnalyzerImpl(String analyzerName,
			AnalyzerDefinition analyzerDefinition,
			String tokenizerName,
			TokenizerDefinition tokenizerDefinition,
			Map<String, CharFilterDefinition> charFilters,
			Map<String, TokenFilterDefinition> tokenFilters) {
		super();
		this.analyzerName = analyzerName;
		this.analyzerDefinition = analyzerDefinition;
		this.tokenizerDefinition = tokenizerDefinition;
		this.charFilters = charFilters;
		this.tokenFilters = tokenFilters;
	}

	@Override
	public String getName(String fieldName) {
		return analyzerName;
	}

	@Override
	public String registerDefinitions(ElasticsearchAnalysisDefinitionRegistry registry, String fieldName) {
		registry.register( analyzerName, analyzerDefinition );
		if ( tokenizerDefinition != null ) {
			registry.register( analyzerDefinition.getTokenizer(), tokenizerDefinition );
		}
		for ( Map.Entry<String, CharFilterDefinition> entry : charFilters.entrySet() ) {
			registry.register( entry.getKey(), entry.getValue() );
		}
		for ( Map.Entry<String, TokenFilterDefinition> entry : tokenFilters.entrySet() ) {
			registry.register( entry.getKey(), entry.getValue() );
		}
		return analyzerName;
	}

	@Override
	public void close() {
		// nothing to close
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode( analyzerName );
		result = prime * result + Objects.hashCode( analyzerDefinition );
		result = prime * result + Objects.hashCode( tokenizerDefinition );
		result = prime * result + Objects.hashCode( charFilters );
		result = prime * result + Objects.hashCode( tokenFilters );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof CustomElasticsearchAnalyzerImpl ) {
			CustomElasticsearchAnalyzerImpl other = (CustomElasticsearchAnalyzerImpl) obj;
			return Objects.equals( analyzerName, other.analyzerName )
					&& Objects.equals( analyzerDefinition, other.analyzerDefinition )
					&& Objects.equals( tokenizerDefinition, other.tokenizerDefinition )
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
		sb.append( analyzerDefinition );
		sb.append( ">" );
		return sb.toString();
	}

}
