/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

public class ElasticsearchIndexSchemaManagerNormalizerITAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		context.normalizer( "custom-normalizer" ).custom()
				.charFilters( "custom-char-mapping" )
				.tokenFilters( "custom-elision" );
		context.charFilter( "custom-char-mapping" )
				.type( "mapping" )
				.param( "mappings", new String[] { "foo => bar" } ); // Explicit array to get a JSON array
		context.tokenFilter( "custom-elision" )
				.type( "elision" )
				.param( "articles", "l", "d" );
	}
}
