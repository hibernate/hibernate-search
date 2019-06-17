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
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchCustomAnalyzerDefinitionContext;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisDefinitionContainerContext;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchCustomNormalizerDefinitionContext;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchNormalizerDefinitionContext;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchTypedAnalysisComponentDefinitionContext;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionContributor;



public class ElasticsearchAnalysisDefinitionContainerContextImpl
		implements ElasticsearchAnalysisDefinitionContainerContext, ElasticsearchAnalysisDefinitionContributor {

	private final List<ElasticsearchAnalysisDefinitionContributor> children = new ArrayList<>();

	@Override
	public ElasticsearchAnalyzerDefinitionContext analyzer(String name) {
		return new ElasticsearchAnalyzerDefinitionContext() {
			@Override
			public ElasticsearchCustomAnalyzerDefinitionContext custom() {
				ElasticsearchCustomAnalyzerDefinitionContextImpl context =
						new ElasticsearchCustomAnalyzerDefinitionContextImpl( name );
				children.add( context );
				return context;
			}

			@Override
			public ElasticsearchTypedAnalysisComponentDefinitionContext type(String type) {
				ElasticsearchTypedAnalyzerDefinitionContext context =
						new ElasticsearchTypedAnalyzerDefinitionContext( name, type );
				children.add( context );
				return context;
			}
		};
	}

	@Override
	public ElasticsearchNormalizerDefinitionContext normalizer(String name) {
		return new ElasticsearchNormalizerDefinitionContext() {
			@Override
			public ElasticsearchCustomNormalizerDefinitionContext custom() {
				ElasticsearchCustomNormalizerDefinitionContextImpl context =
						new ElasticsearchCustomNormalizerDefinitionContextImpl( name );
				children.add( context );
				return context;
			}
		};
	}

	@Override
	public ElasticsearchAnalysisComponentDefinitionContext tokenizer(String name) {
		ElasticsearchTokenizerDefinitionContext context = new ElasticsearchTokenizerDefinitionContext( name );
		children.add( context );
		return context;
	}

	@Override
	public ElasticsearchAnalysisComponentDefinitionContext charFilter(String name) {
		ElasticsearchCharFilterDefinitionContext context = new ElasticsearchCharFilterDefinitionContext( name );
		children.add( context );
		return context;
	}

	@Override
	public ElasticsearchAnalysisComponentDefinitionContext tokenFilter(String name) {
		ElasticsearchTokenFilterDefinitionContext context = new ElasticsearchTokenFilterDefinitionContext( name );
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
