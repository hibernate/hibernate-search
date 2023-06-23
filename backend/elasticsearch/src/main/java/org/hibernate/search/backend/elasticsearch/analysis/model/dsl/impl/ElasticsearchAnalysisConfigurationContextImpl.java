/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisComponentParametersStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisComponentTypeStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalyzerTokenizerStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalyzerTypeStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchNormalizerTypeStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionContributor;

public class ElasticsearchAnalysisConfigurationContextImpl
		implements ElasticsearchAnalysisConfigurationContext, ElasticsearchAnalysisDefinitionContributor {

	private final List<ElasticsearchAnalysisDefinitionContributor> children = new ArrayList<>();

	@Override
	public ElasticsearchAnalyzerTypeStep analyzer(String name) {
		return new ElasticsearchAnalyzerTypeStep() {
			@Override
			public ElasticsearchAnalyzerTokenizerStep custom() {
				ElasticsearchAnalyzerComponentsStep context =
						new ElasticsearchAnalyzerComponentsStep( name );
				children.add( context );
				return context;
			}

			@Override
			public ElasticsearchAnalysisComponentParametersStep type(String type) {
				ElasticsearchAnalyzerParametersStep context =
						new ElasticsearchAnalyzerParametersStep( name, type );
				children.add( context );
				return context;
			}
		};
	}

	@Override
	public ElasticsearchNormalizerTypeStep normalizer(String name) {
		return () -> {
			ElasticsearchNormalizerComponentsStep context =
					new ElasticsearchNormalizerComponentsStep( name );
			children.add( context );
			return context;
		};
	}

	@Override
	public ElasticsearchAnalysisComponentTypeStep tokenizer(String name) {
		ElasticsearchTokenizerParametersStep context = new ElasticsearchTokenizerParametersStep( name );
		children.add( context );
		return context;
	}

	@Override
	public ElasticsearchAnalysisComponentTypeStep charFilter(String name) {
		ElasticsearchCharFilterParametersStep context = new ElasticsearchCharFilterParametersStep( name );
		children.add( context );
		return context;
	}

	@Override
	public ElasticsearchAnalysisComponentTypeStep tokenFilter(String name) {
		ElasticsearchTokenFilterParametersStep context = new ElasticsearchTokenFilterParametersStep( name );
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
