/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.definition.impl;

import org.hibernate.search.elasticsearch.settings.impl.model.AnalyzerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.NormalizerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenizerDefinition;

/**
 * An {@link ElasticsearchAnalysisDefinitionRegistry} that delegate calls
 * to a "parent" registry when no definition is found.
 * <p>
 * Mutating calls ({@code register} methods) are never delegated to the parent.
 *
 * @author Yoann Rodiere
 */
public final class ChainingElasticsearchAnalysisDefinitionRegistry extends ForwardingElasticsearchAnalysisDefinitionRegistry {

	private final ElasticsearchAnalysisDefinitionRegistry parent;

	public ChainingElasticsearchAnalysisDefinitionRegistry(ElasticsearchAnalysisDefinitionRegistry self,
			ElasticsearchAnalysisDefinitionRegistry parent) {
		super( self );
		this.parent = parent;
	}

	@Override
	public AnalyzerDefinition getAnalyzerDefinition(String name) {
		AnalyzerDefinition result = super.getAnalyzerDefinition( name );
		if ( result == null ) {
			result = parent.getAnalyzerDefinition( name );
		}
		return result;
	}

	@Override
	public NormalizerDefinition getNormalizerDefinition(String name) {
		NormalizerDefinition result = super.getNormalizerDefinition( name );
		if ( result == null ) {
			result = parent.getNormalizerDefinition( name );
		}
		return result;
	}

	@Override
	public TokenizerDefinition getTokenizerDefinition(String name) {
		TokenizerDefinition result = super.getTokenizerDefinition( name );
		if ( result == null ) {
			result = parent.getTokenizerDefinition( name );
		}
		return result;
	}

	@Override
	public TokenFilterDefinition getTokenFilterDefinition(String name) {
		TokenFilterDefinition result = super.getTokenFilterDefinition( name );
		if ( result == null ) {
			result = parent.getTokenFilterDefinition( name );
		}
		return result;
	}

	@Override
	public CharFilterDefinition getCharFilterDefinition(String name) {
		CharFilterDefinition result = super.getCharFilterDefinition( name );
		if ( result == null ) {
			result = parent.getCharFilterDefinition( name );
		}
		return result;
	}

}
