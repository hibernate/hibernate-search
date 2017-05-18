/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.definition.impl;

import org.hibernate.search.elasticsearch.analyzer.definition.ElasticsearchAnalyzerDefinitionContext;
import org.hibernate.search.elasticsearch.analyzer.definition.ElasticsearchAnalyzerDefinitionWithTokenizerContext;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalyzerDefinition;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchAnalyzerDefinitionContextImpl
		implements ElasticsearchAnalyzerDefinitionContext,
				ElasticsearchAnalyzerDefinitionWithTokenizerContext,
				ElasticsearchAnalysisDefinitionRegistryPopulator {

	protected static final Log LOG = LoggerFactory.make( Log.class );

	private final String name;

	private final AnalyzerDefinition definition = new AnalyzerDefinition();

	public ElasticsearchAnalyzerDefinitionContextImpl(String name) {
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
	public void populate(ElasticsearchAnalysisDefinitionRegistry registry) {
		if ( StringHelper.isEmpty( definition.getTokenizer() ) ) {
			throw LOG.invalidElasticsearchAnalyzerDefinition( name );
		}
		registry.register( name, definition );
	}

}
