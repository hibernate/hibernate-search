/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisDefinitionContainerContext;
import org.hibernate.search.integrationtest.backend.tck.analysis.AnalysisCustomIT;

public class AnalysisCustomITAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
	@Override
	public void configure(ElasticsearchAnalysisDefinitionContainerContext context) {
		context.normalizer( AnalysisCustomIT.AnalysisDefinitions.NORMALIZER_NOOP.name ).custom();
		context.normalizer( AnalysisCustomIT.AnalysisDefinitions.NORMALIZER_LOWERCASE.name ).custom()
				.withTokenFilters( "lowercase" );

		context.analyzer( AnalysisCustomIT.AnalysisDefinitions.ANALYZER_NOOP.name ).custom()
				.withTokenizer( "keyword" );
		context.analyzer( AnalysisCustomIT.AnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ).custom()
				.withTokenizer( "whitespace" )
				.withTokenFilters( "lowercase" );

		String charFilterName = AnalysisCustomIT.AnalysisDefinitions.ANALYZER_PATTERNS_STOPWORD.name + "_charFilter";
		String tokenizerName = AnalysisCustomIT.AnalysisDefinitions.ANALYZER_PATTERNS_STOPWORD.name + "_tokenizer";
		String tokenFilterName = AnalysisCustomIT.AnalysisDefinitions.ANALYZER_PATTERNS_STOPWORD.name + "_tokenFilter";
		context.analyzer( AnalysisCustomIT.AnalysisDefinitions.ANALYZER_PATTERNS_STOPWORD.name ).custom()
				.withTokenizer( tokenizerName )
				.withCharFilters( charFilterName )
				.withTokenFilters( tokenFilterName );
		context.charFilter( charFilterName ).type( "pattern_replace" )
				.param( "pattern", "\\s+" )
				.param( "replacement", "," );
		context.tokenizer( tokenizerName ).type( "pattern" )
				.param( "pattern", "," );
		context.tokenFilter( tokenFilterName ).type( "stop" )
				.param( "stopwords", "stopword" );
	}
}
