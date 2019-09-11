/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
// tag::include[]
package org.hibernate.search.documentation.analysis;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

public class MyElasticsearchAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		context.analyzer( "english" ).custom() // <1>
				.tokenizer( "standard" ) // <2>
				.charFilters( "html_strip" ) // <3>
				.tokenFilters( "lowercase", "snowball_english", "asciifolding" ); // <4>

		context.tokenFilter( "snowball_english" ) // <5>
				.type( "snowball" )
				.param( "language", "English" ); // <6>

		context.normalizer( "lowercase" ).custom() // <7>
				.tokenFilters( "lowercase", "asciifolding" );

		context.analyzer( "french" ).custom() // <8>
				.tokenizer( "standard" )
				.tokenFilters( "lowercase", "snowball_french", "asciifolding" );

		context.tokenFilter( "snowball_french" )
				.type( "snowball" )
				.param( "language", "French" );
	}
}
// end::include[]
