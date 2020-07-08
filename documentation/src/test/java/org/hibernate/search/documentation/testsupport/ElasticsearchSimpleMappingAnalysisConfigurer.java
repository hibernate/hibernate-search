/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

class ElasticsearchSimpleMappingAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		context.analyzer( "english" ).custom()
				.tokenizer( "standard" )
				.tokenFilters( "lowercase", "snowball_english", "asciifolding" );

		context.tokenFilter( "snowball_english" )
				.type( "snowball" )
				.param( "language", "English" );

		context.analyzer( "name" ).custom()
				.tokenizer( "standard" )
				.tokenFilters( "lowercase", "asciifolding" );

		context.analyzer( "autocomplete_indexing" ).custom()
				.tokenizer( "standard" )
				.tokenFilters( "lowercase", "snowball_english", "asciifolding", "autocomplete_edge_ngram" );

		context.tokenFilter( "autocomplete_edge_ngram" )
				.type( "edge_ngram" )
				.param( "min_gram", 3 )
				.param( "max_gram", 7 );

		// Same as "autocomplete-indexing", but without the edge-ngram filter
		context.analyzer( "autocomplete_query" ).custom()
				.tokenizer( "standard" )
				.tokenFilters( "lowercase", "snowball_english", "asciifolding" );

		// Normalizers

		context.normalizer( "english" ).custom()
				.tokenFilters( "lowercase", "asciifolding" );

		context.normalizer( "name" ).custom()
				.tokenFilters( "lowercase", "asciifolding" );

		context.normalizer( "isbn" ).custom()
				.charFilters( "removeHyphens" );

		context.charFilter( "removeHyphens" ).type( "pattern_replace" )
				.param( "pattern", "-+" )
				.param( "replacement", "" );
	}
}
