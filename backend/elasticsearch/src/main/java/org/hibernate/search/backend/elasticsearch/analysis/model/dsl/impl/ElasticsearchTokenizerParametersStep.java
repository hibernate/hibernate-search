/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.logging.impl.AnalyzerLog;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenizerDefinition;
import org.hibernate.search.util.common.impl.StringHelper;

class ElasticsearchTokenizerParametersStep
		extends AbstractElasticsearchAnalysisComponentParametersStep<TokenizerDefinition> {

	ElasticsearchTokenizerParametersStep(String name) {
		super( name, new TokenizerDefinition() );
	}

	@Override
	public void contribute(ElasticsearchAnalysisDefinitionCollector collector) {
		if ( StringHelper.isEmpty( definition.getType() ) ) {
			throw AnalyzerLog.INSTANCE.invalidElasticsearchTokenizerDefinition( name );
		}
		collector.collect( name, definition );
	}

}
