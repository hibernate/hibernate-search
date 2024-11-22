/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalyzerOptionalComponentsStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalyzerTokenizerStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionContributor;
import org.hibernate.search.backend.elasticsearch.logging.impl.AnalysisLog;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinition;
import org.hibernate.search.util.common.impl.StringHelper;

class ElasticsearchAnalyzerComponentsStep
		implements ElasticsearchAnalyzerTokenizerStep,
		ElasticsearchAnalyzerOptionalComponentsStep,
		ElasticsearchAnalysisDefinitionContributor {

	private final String name;

	private final AnalyzerDefinition definition = new AnalyzerDefinition();

	ElasticsearchAnalyzerComponentsStep(String name) {
		this.name = name;
		this.definition.setType( "custom" );
	}

	@Override
	public ElasticsearchAnalyzerOptionalComponentsStep tokenizer(String tokenizerName) {
		definition.setTokenizer( tokenizerName );
		return this;
	}

	@Override
	public ElasticsearchAnalyzerOptionalComponentsStep charFilters(String... names) {
		definition.setCharFilters( null );
		for ( String charFilterName : names ) {
			definition.addCharFilter( charFilterName );
		}
		return this;
	}

	@Override
	public ElasticsearchAnalyzerOptionalComponentsStep tokenFilters(String... names) {
		definition.setTokenFilters( null );
		for ( String tokenFilterName : names ) {
			definition.addTokenFilter( tokenFilterName );
		}
		return this;
	}

	@Override
	public void contribute(ElasticsearchAnalysisDefinitionCollector collector) {
		if ( StringHelper.isEmpty( definition.getTokenizer() ) ) {
			throw AnalysisLog.INSTANCE.invalidElasticsearchAnalyzerDefinition( name );
		}
		collector.collect( name, definition );
	}

}
