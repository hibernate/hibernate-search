/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.StringHelper;

public class ElasticsearchTypedAnalyzerDefinitionContextImpl
		extends ElasticsearchAnalysisComponentDefinitionContextImpl<AnalyzerDefinition> {

	private static final Log LOG = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ElasticsearchTypedAnalyzerDefinitionContextImpl(String name, String type) {
		super( name, new AnalyzerDefinition() );
		type( type );
	}

	@Override
	public void contribute(ElasticsearchAnalysisDefinitionCollector collector) {
		if ( StringHelper.isEmpty( definition.getType() ) ) {
			throw LOG.invalidElasticsearchTypedAnalyzerDefinition( name );
		}
		collector.collect( name, definition );
	}
}
