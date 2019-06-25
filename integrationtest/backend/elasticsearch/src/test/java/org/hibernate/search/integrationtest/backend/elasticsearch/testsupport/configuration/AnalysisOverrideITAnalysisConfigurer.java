/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.OverrideAnalysisDefinitions;

public class AnalysisOverrideITAnalysisConfigurer extends DefaultITAnalysisConfigurer {

	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		super.configure( context );

		context.analyzer( OverrideAnalysisDefinitions.ANALYZER_WHITESPACE.name ).custom()
				.withTokenizer( "whitespace" );

		context.analyzer( OverrideAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ).custom()
				.withTokenizer( "whitespace" )
				.withTokenFilters( "lowercase" );

		String tokenizerName = OverrideAnalysisDefinitions.ANALYZER_NGRAM.name + "_tokenizer";
		context.analyzer( OverrideAnalysisDefinitions.ANALYZER_NGRAM.name ).custom()
				.withTokenizer( tokenizerName );

		context.tokenizer( tokenizerName )
				.type( "ngram" )
				.param( "min_gram", 5 )
				.param( "max_gram", 6 );
	}
}
