/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.CharFilterDefinition;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class ElasticsearchCharFilterParametersStep
		extends AbstractElasticsearchAnalysisComponentParametersStep<CharFilterDefinition> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	ElasticsearchCharFilterParametersStep(String name) {
		super( name, new CharFilterDefinition() );
	}

	@Override
	public void contribute(ElasticsearchAnalysisDefinitionCollector collector) {
		if ( StringHelper.isEmpty( definition.getType() ) ) {
			throw log.invalidElasticsearchCharFilterDefinition( name );
		}
		collector.collect( name, definition );
	}

}
