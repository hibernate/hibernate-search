/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.java.modules.pojo.standalone.elasticsearch.config;

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
