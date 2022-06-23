/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
// tag::include[]
package org.hibernate.search.documentation.mapper.orm.gettingstarted.withhsearch.customanalysis;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

public class MyElasticsearchAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		context.analyzer( "english" ).custom() // <1>
				.tokenizer( "standard" ) // <2>
				.tokenFilters( "lowercase", "snowball_english", "asciifolding" ); // <3>

		context.tokenFilter( "snowball_english" ) // <4>
				.type( "snowball" )
				.param( "language", "English" ); // <5>

		context.analyzer( "name" ).custom() // <6>
				.tokenizer( "standard" )
				.tokenFilters( "lowercase", "asciifolding" );
	}
}
// end::include[]
