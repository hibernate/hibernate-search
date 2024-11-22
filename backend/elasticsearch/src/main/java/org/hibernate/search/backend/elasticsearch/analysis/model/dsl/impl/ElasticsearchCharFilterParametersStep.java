/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.logging.impl.AnalysisLog;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.CharFilterDefinition;
import org.hibernate.search.util.common.impl.StringHelper;

class ElasticsearchCharFilterParametersStep
		extends AbstractElasticsearchAnalysisComponentParametersStep<CharFilterDefinition> {

	ElasticsearchCharFilterParametersStep(String name) {
		super( name, new CharFilterDefinition() );
	}

	@Override
	public void contribute(ElasticsearchAnalysisDefinitionCollector collector) {
		if ( StringHelper.isEmpty( definition.getType() ) ) {
			throw AnalysisLog.INSTANCE.invalidElasticsearchCharFilterDefinition( name );
		}
		collector.collect( name, definition );
	}

}
