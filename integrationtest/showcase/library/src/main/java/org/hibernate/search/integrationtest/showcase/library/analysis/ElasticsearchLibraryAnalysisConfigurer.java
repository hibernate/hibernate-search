/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.analysis;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

import org.springframework.stereotype.Component;

@Component("elasticsearchAnalysisConfigurer")
public class ElasticsearchLibraryAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {

	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		context.normalizer( LibraryAnalyzers.NORMALIZER_SORT ).custom()
				.tokenFilters( "lowercase", "asciifolding" );
		context.normalizer( LibraryAnalyzers.NORMALIZER_ISBN ).custom()
				.charFilters( "removeHyphens" );
		context.charFilter( "removeHyphens" ).type( "pattern_replace" )
				.param( "pattern", "-+" )
				.param( "replacement", "" );
	}
}
