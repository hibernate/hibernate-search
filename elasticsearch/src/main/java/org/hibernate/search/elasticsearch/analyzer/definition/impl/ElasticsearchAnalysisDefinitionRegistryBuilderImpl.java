/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.definition.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.elasticsearch.analyzer.definition.ElasticsearchAnalysisComponentDefinitionContext;
import org.hibernate.search.elasticsearch.analyzer.definition.ElasticsearchAnalysisDefinitionRegistryBuilder;
import org.hibernate.search.elasticsearch.analyzer.definition.ElasticsearchAnalyzerDefinitionContext;
import org.hibernate.search.elasticsearch.analyzer.definition.ElasticsearchNormalizerDefinitionContext;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchAnalysisDefinitionRegistryBuilderImpl implements ElasticsearchAnalysisDefinitionRegistryBuilder {

	private final List<ElasticsearchAnalysisDefinitionRegistryPopulator> populators = new ArrayList<>();

	@Override
	public ElasticsearchAnalyzerDefinitionContext analyzer(String name) {
		ElasticsearchAnalyzerDefinitionContextImpl context = new ElasticsearchAnalyzerDefinitionContextImpl( name );
		populators.add( context );
		return context;
	}

	@Override
	public ElasticsearchNormalizerDefinitionContext normalizer(String name) {
		ElasticsearchNormalizerDefinitionContextImpl context = new ElasticsearchNormalizerDefinitionContextImpl( name );
		populators.add( context );
		return context;
	}

	@Override
	public ElasticsearchAnalysisComponentDefinitionContext tokenizer(String name) {
		ElasticsearchTokenizerDefinitionContextImpl context = new ElasticsearchTokenizerDefinitionContextImpl( name );
		populators.add( context );
		return context;
	}

	@Override
	public ElasticsearchAnalysisComponentDefinitionContext charFilter(String name) {
		ElasticsearchCharFilterDefinitionContextImpl context = new ElasticsearchCharFilterDefinitionContextImpl( name );
		populators.add( context );
		return context;
	}

	@Override
	public ElasticsearchAnalysisComponentDefinitionContext tokenFilter(String name) {
		ElasticsearchTokenFilterDefinitionContextImpl context = new ElasticsearchTokenFilterDefinitionContextImpl( name );
		populators.add( context );
		return context;
	}

	public void build(ElasticsearchAnalysisDefinitionRegistry registry) {
		for ( ElasticsearchAnalysisDefinitionRegistryPopulator populator : populators ) {
			populator.populate( registry );
		}
	}

}
