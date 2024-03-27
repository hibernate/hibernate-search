/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.elasticsearch.testsupport;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.analysis.Analyzers;

public class ElasticsearchPerformanceAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {

	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		context.analyzer( Analyzers.ANALYZER_ENGLISH ).custom()
				.tokenizer( "standard" )
				.tokenFilters( "lowercase", "snowball_english", "asciifolding" );

		context.tokenFilter( "snowball_english" )
				.type( "snowball" )
				.param( "language", "English" );

		context.normalizer( Analyzers.NORMALIZER_ENGLISH ).custom()
				.tokenFilters( "lowercase", "asciifolding" );
	}

}
