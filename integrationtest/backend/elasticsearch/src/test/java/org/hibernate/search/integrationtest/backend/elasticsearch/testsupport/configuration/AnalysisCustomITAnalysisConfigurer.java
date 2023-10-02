/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.analysis.AnalysisCustomIT;

public class AnalysisCustomITAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		String charFilterName = AnalysisCustomIT.AnalysisDefinitions.ANALYZER_PATTERNS_STOPWORD.name + "_charFilter";
		String tokenizerName = AnalysisCustomIT.AnalysisDefinitions.ANALYZER_PATTERNS_STOPWORD.name + "_tokenizer";
		String tokenFilterName = AnalysisCustomIT.AnalysisDefinitions.ANALYZER_PATTERNS_STOPWORD.name + "_tokenFilter";

		context.normalizer( AnalysisCustomIT.AnalysisDefinitions.NORMALIZER_NOOP.name ).custom();
		context.normalizer( AnalysisCustomIT.AnalysisDefinitions.NORMALIZER_LOWERCASE.name ).custom()
				.tokenFilters( "lowercase" );
		context.normalizer( AnalysisCustomIT.AnalysisDefinitions.NORMALIZER_PATTERN_REPLACING.name ).custom()
				.charFilters( charFilterName );

		context.analyzer( AnalysisCustomIT.AnalysisDefinitions.ANALYZER_NOOP.name ).custom()
				.tokenizer( "keyword" );
		context.analyzer( AnalysisCustomIT.AnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ).custom()
				.tokenizer( "whitespace" )
				.tokenFilters( "lowercase" );

		context.analyzer( AnalysisCustomIT.AnalysisDefinitions.ANALYZER_PATTERNS_STOPWORD.name ).custom()
				.tokenizer( tokenizerName )
				.charFilters( charFilterName )
				.tokenFilters( tokenFilterName );
		context.charFilter( charFilterName ).type( "pattern_replace" )
				.param( "pattern", "\\s+" )
				.param( "replacement", "," );
		context.tokenizer( tokenizerName ).type( "pattern" )
				.param( "pattern", "," );
		context.tokenFilter( tokenFilterName ).type( "stop" )
				.param( "stopwords", "stopword" );
	}
}
