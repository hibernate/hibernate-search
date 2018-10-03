/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionContributor;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchCustomNormalizerDefinitionContext;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchCustomNormalizerDefinitionContextImpl
		implements ElasticsearchCustomNormalizerDefinitionContext,
		ElasticsearchAnalysisDefinitionContributor {

	private final String name;

	private final NormalizerDefinition definition = new NormalizerDefinition();

	ElasticsearchCustomNormalizerDefinitionContextImpl(String name) {
		this.name = name;
		this.definition.setType( "custom" );
	}

	@Override
	public ElasticsearchCustomNormalizerDefinitionContext withCharFilters(String... names) {
		definition.setCharFilters( null );
		for ( String name : names ) {
			definition.addCharFilter( name );
		}
		return this;
	}

	@Override
	public ElasticsearchCustomNormalizerDefinitionContext withTokenFilters(String... names) {
		definition.setTokenFilters( null );
		for ( String name : names ) {
			definition.addTokenFilter( name );
		}
		return this;
	}

	@Override
	public void contribute(ElasticsearchAnalysisDefinitionCollector collector) {
		collector.collect( name, definition );
	}

}
