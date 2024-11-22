/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.logging.impl.AnalysisLog;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinition;
import org.hibernate.search.util.common.impl.StringHelper;

class ElasticsearchAnalyzerParametersStep
		extends AbstractElasticsearchAnalysisComponentParametersStep<AnalyzerDefinition> {

	ElasticsearchAnalyzerParametersStep(String name, String type) {
		super( name, new AnalyzerDefinition() );
		type( type );
	}

	@Override
	public void contribute(ElasticsearchAnalysisDefinitionCollector collector) {
		if ( StringHelper.isEmpty( definition.getType() ) ) {
			throw AnalysisLog.INSTANCE.invalidElasticsearchTypedAnalyzerDefinition( name );
		}
		collector.collect( name, definition );
	}
}
