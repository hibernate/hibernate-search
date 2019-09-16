/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.impl.StringHelper;


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
