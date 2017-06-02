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
 * to another instance.
 *
 * @author Yoann Rodiere
 */
public class ForwardingElasticsearchAnalysisDefinitionRegistry implements ElasticsearchAnalysisDefinitionRegistry {

	private final ElasticsearchAnalysisDefinitionRegistry delegate;

	public ForwardingElasticsearchAnalysisDefinitionRegistry(ElasticsearchAnalysisDefinitionRegistry delegate) {
		this.delegate = delegate;
	}

	@Override
	public void register(String name, AnalyzerDefinition definition) {
		delegate.register( name, definition );
	}

	@Override
	public void register(String name, NormalizerDefinition definition) {
		delegate.register( name, definition );
	}

	@Override
	public void register(String name, TokenizerDefinition definition) {
		delegate.register( name, definition );
	}

	@Override
	public void register(String name, TokenFilterDefinition definition) {
		delegate.register( name, definition );
	}

	@Override
	public void register(String name, CharFilterDefinition definition) {
		delegate.register( name, definition );
	}

	@Override
	public AnalyzerDefinition getAnalyzerDefinition(String name) {
		return delegate.getAnalyzerDefinition( name );
	}

	@Override
	public NormalizerDefinition getNormalizerDefinition(String name) {
		return delegate.getNormalizerDefinition( name );
	}

	@Override
	public TokenizerDefinition getTokenizerDefinition(String name) {
		return delegate.getTokenizerDefinition( name );
	}

	@Override
	public TokenFilterDefinition getTokenFilterDefinition(String name) {
		return delegate.getTokenFilterDefinition( name );
	}

	@Override
	public CharFilterDefinition getCharFilterDefinition(String name) {
		return delegate.getCharFilterDefinition( name );
	}

}
