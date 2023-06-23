/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

public class ElasticsearchIndexSchemaManagerAnalyzerITAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		context.analyzer( "custom-analyzer" ).custom()
				.tokenizer( "custom-edgeNGram" )
				.charFilters( "custom-pattern-replace" )
				.tokenFilters( "custom-keep-types", "custom-word-delimiter" );
		context.charFilter( "custom-pattern-replace" )
				.type( "pattern_replace" )
				.param( "pattern", "[^0-9]" )
				.param( "replacement", "0" )
				.param( "tags", "CASE_INSENSITIVE|COMMENTS" );
		context.tokenizer( "custom-edgeNGram" )
				.type( "edge_ngram" )
				.param( "min_gram", 1 )
				.param( "max_gram", 10 );
		context.tokenFilter( "custom-keep-types" )
				.type( "keep_types" )
				.param( "types", "<NUM>", "<DOUBLE>" );
		context.tokenFilter( "custom-word-delimiter" )
				.type( "word_delimiter" )
				.param( "generate_word_parts", false );
	}
}
