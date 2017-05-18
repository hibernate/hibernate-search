/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.definition.impl;

import org.hibernate.search.elasticsearch.settings.impl.model.AnalyzerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
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
public final class ChainingElasticsearchAnalysisDefinitionRegistry implements ElasticsearchAnalysisDefinitionRegistry {

	private final ElasticsearchAnalysisDefinitionRegistry parent;
	private final ElasticsearchAnalysisDefinitionRegistry self = new SimpleElasticsearchAnalysisDefinitionRegistry();

	public ChainingElasticsearchAnalysisDefinitionRegistry(ElasticsearchAnalysisDefinitionRegistry parent) {
		this.parent = parent;
	}

	@Override
	public void register(String name, AnalyzerDefinition definition) {
		self.register( name, definition );
	}

	@Override
	public void register(String name, TokenizerDefinition definition) {
		self.register( name, definition );
	}

	@Override
	public void register(String name, TokenFilterDefinition definition) {
		self.register( name, definition );
	}

	@Override
	public void register(String name, CharFilterDefinition definition) {
		self.register( name, definition );
	}

	@Override
	public AnalyzerDefinition getAnalyzerDefinition(String name) {
		AnalyzerDefinition result = self.getAnalyzerDefinition( name );
		if ( result == null ) {
			result = parent.getAnalyzerDefinition( name );
		}
		return result;
	}

	@Override
	public TokenizerDefinition getTokenizerDefinition(String name) {
		TokenizerDefinition result = self.getTokenizerDefinition( name );
		if ( result == null ) {
			result = parent.getTokenizerDefinition( name );
		}
		return result;
	}

	@Override
	public TokenFilterDefinition getTokenFilterDefinition(String name) {
		TokenFilterDefinition result = self.getTokenFilterDefinition( name );
		if ( result == null ) {
			result = parent.getTokenFilterDefinition( name );
		}
		return result;
	}

	@Override
	public CharFilterDefinition getCharFilterDefinition(String name) {
		CharFilterDefinition result = self.getCharFilterDefinition( name );
		if ( result == null ) {
			result = parent.getCharFilterDefinition( name );
		}
		return result;
	}

}
