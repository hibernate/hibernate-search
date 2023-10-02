/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
// tag::include[]
package org.hibernate.search.documentation.analysis;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

public class AdvancedElasticsearchAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		// tag::type[]
		context.analyzer( "english_stopwords" ).type( "standard" ) // <1>
				.param( "stopwords", "_english_" ); // <2>
		// end::type[]
	}
}
