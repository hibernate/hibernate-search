/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchNormalizerDefinitionContext;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchNormalizerDefinitionContextImpl
		implements ElasticsearchNormalizerDefinitionContext,
		ElasticsearchAnalysisDefinitionRegistryPopulator {

	private final String name;

	private final NormalizerDefinition definition = new NormalizerDefinition();

	ElasticsearchNormalizerDefinitionContextImpl(String name) {
		this.name = name;
		this.definition.setType( "custom" );
	}

	@Override
	public ElasticsearchNormalizerDefinitionContext withCharFilters(String... names) {
		definition.setCharFilters( null );
		for ( String name : names ) {
			definition.addCharFilter( name );
		}
		return this;
	}

	@Override
	public ElasticsearchNormalizerDefinitionContext withTokenFilters(String... names) {
		definition.setTokenFilters( null );
		for ( String name : names ) {
			definition.addTokenFilter( name );
		}
		return this;
	}

	@Override
	public void populate(ElasticsearchAnalysisDefinitionRegistry registry) {
		registry.register( name, definition );
	}

}
