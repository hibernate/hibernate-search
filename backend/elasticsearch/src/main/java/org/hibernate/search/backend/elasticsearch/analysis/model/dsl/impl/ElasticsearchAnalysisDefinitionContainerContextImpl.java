/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisComponentDefinitionContext;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalyzerDefinitionContext;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisDefinitionContainerContext;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchNormalizerDefinitionContext;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionContributor;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchAnalysisDefinitionContainerContextImpl
		implements ElasticsearchAnalysisDefinitionContainerContext, ElasticsearchAnalysisDefinitionContributor {

	private final List<ElasticsearchAnalysisDefinitionContributor> children = new ArrayList<>();

	@Override
	public ElasticsearchAnalyzerDefinitionContext analyzer(String name) {
		ElasticsearchAnalyzerDefinitionContextImpl context = new ElasticsearchAnalyzerDefinitionContextImpl( name );
		children.add( context );
		return context;
	}

	@Override
	public ElasticsearchNormalizerDefinitionContext normalizer(String name) {
		ElasticsearchNormalizerDefinitionContextImpl context = new ElasticsearchNormalizerDefinitionContextImpl( name );
		children.add( context );
		return context;
	}

	@Override
	public ElasticsearchAnalysisComponentDefinitionContext tokenizer(String name) {
		ElasticsearchTokenizerDefinitionContextImpl context = new ElasticsearchTokenizerDefinitionContextImpl( name );
		children.add( context );
		return context;
	}

	@Override
	public ElasticsearchAnalysisComponentDefinitionContext charFilter(String name) {
		ElasticsearchCharFilterDefinitionContextImpl context = new ElasticsearchCharFilterDefinitionContextImpl( name );
		children.add( context );
		return context;
	}

	@Override
	public ElasticsearchAnalysisComponentDefinitionContext tokenFilter(String name) {
		ElasticsearchTokenFilterDefinitionContextImpl context = new ElasticsearchTokenFilterDefinitionContextImpl( name );
		children.add( context );
		return context;
	}

	@Override
	public void contribute(ElasticsearchAnalysisDefinitionCollector collector) {
		for ( ElasticsearchAnalysisDefinitionContributor child : children ) {
			child.contribute( collector );
		}
	}

}
