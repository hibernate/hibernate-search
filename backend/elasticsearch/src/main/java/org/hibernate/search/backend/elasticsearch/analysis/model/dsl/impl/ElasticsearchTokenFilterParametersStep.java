/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenFilterDefinition;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class ElasticsearchTokenFilterParametersStep
		extends AbstractElasticsearchAnalysisComponentParametersStep<TokenFilterDefinition> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	ElasticsearchTokenFilterParametersStep(String name) {
		super( name, new TokenFilterDefinition() );
	}

	@Override
	public void contribute(ElasticsearchAnalysisDefinitionCollector collector) {
		if ( StringHelper.isEmpty( definition.getType() ) ) {
			throw log.invalidElasticsearchTokenFilterDefinition( name );
		}
		collector.collect( name, definition );
	}

}
