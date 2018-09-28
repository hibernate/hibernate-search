/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalyzerDefinitionContext;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionContributor;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalyzerDefinitionWithTokenizerContext;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.StringHelper;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchAnalyzerDefinitionContextImpl
		implements ElasticsearchAnalyzerDefinitionContext,
		ElasticsearchAnalyzerDefinitionWithTokenizerContext,
		ElasticsearchAnalysisDefinitionContributor {

	private static final Log LOG = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private final AnalyzerDefinition definition = new AnalyzerDefinition();

	ElasticsearchAnalyzerDefinitionContextImpl(String name) {
		this.name = name;
		this.definition.setType( "custom" );
	}

	@Override
	public ElasticsearchAnalyzerDefinitionWithTokenizerContext withTokenizer(String name) {
		definition.setTokenizer( name );
		return this;
	}

	@Override
	public ElasticsearchAnalyzerDefinitionWithTokenizerContext withCharFilters(String... names) {
		definition.setCharFilters( null );
		for ( String name : names ) {
			definition.addCharFilter( name );
		}
		return this;
	}

	@Override
	public ElasticsearchAnalyzerDefinitionWithTokenizerContext withTokenFilters(String... names) {
		definition.setTokenFilters( null );
		for ( String name : names ) {
			definition.addTokenFilter( name );
		}
		return this;
	}

	@Override
	public void contribute(ElasticsearchAnalysisDefinitionCollector collector) {
		if ( StringHelper.isEmpty( definition.getTokenizer() ) ) {
			throw LOG.invalidElasticsearchAnalyzerDefinition( name );
		}
		collector.collect( name, definition );
	}

}
