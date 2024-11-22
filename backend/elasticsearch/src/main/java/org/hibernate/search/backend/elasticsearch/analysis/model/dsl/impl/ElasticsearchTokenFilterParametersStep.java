/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.logging.impl.AnalysisLog;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenFilterDefinition;
import org.hibernate.search.util.common.impl.StringHelper;

class ElasticsearchTokenFilterParametersStep
		extends AbstractElasticsearchAnalysisComponentParametersStep<TokenFilterDefinition> {

	ElasticsearchTokenFilterParametersStep(String name) {
		super( name, new TokenFilterDefinition() );
	}

	@Override
	public void contribute(ElasticsearchAnalysisDefinitionCollector collector) {
		if ( StringHelper.isEmpty( definition.getType() ) ) {
			throw AnalysisLog.INSTANCE.invalidElasticsearchTokenFilterDefinition( name );
		}
		collector.collect( name, definition );
	}

}
