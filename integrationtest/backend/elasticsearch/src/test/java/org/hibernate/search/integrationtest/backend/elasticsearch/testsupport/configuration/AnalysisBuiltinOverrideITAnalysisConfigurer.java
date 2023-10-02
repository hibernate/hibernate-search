/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;

public class AnalysisBuiltinOverrideITAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		overrideAnalyzer( context, AnalyzerNames.DEFAULT );
		overrideAnalyzer( context, AnalyzerNames.STANDARD );
		overrideAnalyzer( context, AnalyzerNames.SIMPLE );
		overrideAnalyzer( context, AnalyzerNames.WHITESPACE );
		overrideAnalyzer( context, AnalyzerNames.STOP );
		overrideAnalyzer( context, AnalyzerNames.KEYWORD );
	}

	private void overrideAnalyzer(ElasticsearchAnalysisConfigurationContext context, String name) {
		String filterName = name + "_filter";
		context.analyzer( name ).custom()
				.tokenizer( "keyword" )
				.tokenFilters( filterName );
		context.tokenFilter( filterName ).type( "pattern_replace" )
				.param( "pattern", ".*" )
				.param( "replacement", name );
	}
}
