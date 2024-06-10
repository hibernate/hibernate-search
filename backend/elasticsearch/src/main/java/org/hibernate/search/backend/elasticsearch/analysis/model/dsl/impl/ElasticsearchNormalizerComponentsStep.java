/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchNormalizerOptionalComponentsStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionContributor;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinition;

class ElasticsearchNormalizerComponentsStep
		implements ElasticsearchNormalizerOptionalComponentsStep,
		ElasticsearchAnalysisDefinitionContributor {

	private final String name;

	private final NormalizerDefinition definition = new NormalizerDefinition();

	ElasticsearchNormalizerComponentsStep(String name) {
		this.name = name;
		this.definition.setType( "custom" );
	}

	@Override
	public ElasticsearchNormalizerOptionalComponentsStep charFilters(String... names) {
		definition.setCharFilters( null );
		for ( String charFilterName : names ) {
			definition.addCharFilter( charFilterName );
		}
		return this;
	}

	@Override
	public ElasticsearchNormalizerOptionalComponentsStep tokenFilters(String... names) {
		definition.setTokenFilters( null );
		for ( String tokenFilterName : names ) {
			definition.addTokenFilter( tokenFilterName );
		}
		return this;
	}

	@Override
	public void contribute(ElasticsearchAnalysisDefinitionCollector collector) {
		collector.collect( name, definition );
	}

}
