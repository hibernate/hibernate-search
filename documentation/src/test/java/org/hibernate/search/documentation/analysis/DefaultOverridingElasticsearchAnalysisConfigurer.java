/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
// tag::include[]
package org.hibernate.search.documentation.analysis;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;

public class DefaultOverridingElasticsearchAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		context.analyzer( AnalyzerNames.DEFAULT ) // <1>
				.custom() // <2>
				.tokenizer( "standard" )
				.tokenFilters( "lowercase", "snowball_french", "asciifolding" );

		context.tokenFilter( "snowball_french" )
				.type( "snowball" )
				.param( "language", "French" );
	}
}
// end::include[]
