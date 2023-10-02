/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.config;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

public class MyElasticsearchAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {

	public static final String MY_ANALYZER = "myAnalyzer";

	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		context.analyzer( MY_ANALYZER ).custom()
				.tokenizer( "standard" )
				.tokenFilters( "lowercase", "mySnowballFilter", "asciifolding" );

		context.tokenFilter( "mySnowballFilter" )
				.type( "snowball" )
				.param( "language", "English" );
	}
}
