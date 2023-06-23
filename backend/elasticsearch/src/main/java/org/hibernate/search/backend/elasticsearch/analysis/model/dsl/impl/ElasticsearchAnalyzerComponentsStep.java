/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalyzerOptionalComponentsStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalyzerTokenizerStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionCollector;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionContributor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinition;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class ElasticsearchAnalyzerComponentsStep
		implements ElasticsearchAnalyzerTokenizerStep,
		ElasticsearchAnalyzerOptionalComponentsStep,
		ElasticsearchAnalysisDefinitionContributor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private final AnalyzerDefinition definition = new AnalyzerDefinition();

	ElasticsearchAnalyzerComponentsStep(String name) {
		this.name = name;
		this.definition.setType( "custom" );
	}

	@Override
	public ElasticsearchAnalyzerOptionalComponentsStep tokenizer(String tokenizerName) {
		definition.setTokenizer( tokenizerName );
		return this;
	}

	@Override
	public ElasticsearchAnalyzerOptionalComponentsStep charFilters(String... names) {
		definition.setCharFilters( null );
		for ( String charFilterName : names ) {
			definition.addCharFilter( charFilterName );
		}
		return this;
	}

	@Override
	public ElasticsearchAnalyzerOptionalComponentsStep tokenFilters(String... names) {
		definition.setTokenFilters( null );
		for ( String tokenFilterName : names ) {
			definition.addTokenFilter( tokenFilterName );
		}
		return this;
	}

	@Override
	public void contribute(ElasticsearchAnalysisDefinitionCollector collector) {
		if ( StringHelper.isEmpty( definition.getTokenizer() ) ) {
			throw log.invalidElasticsearchAnalyzerDefinition( name );
		}
		collector.collect( name, definition );
	}

}
