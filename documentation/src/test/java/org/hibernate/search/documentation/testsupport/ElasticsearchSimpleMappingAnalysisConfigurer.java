/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.testsupport;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

public class ElasticsearchSimpleMappingAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
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

		context.normalizer( "lowercase" ).custom()
				.tokenFilters( "lowercase" );

		// For AlternativeBinderIT

		context.analyzer( "text_en" ).custom()
				.tokenizer( "standard" )
				.tokenFilters( "lowercase", "snowball_english", "asciifolding" );
		context.analyzer( "text_fr" ).custom()
				.tokenizer( "standard" )
				.tokenFilters( "lowercase", "snowball_french", "asciifolding" );
		context.analyzer( "text_de" ).custom()
				.tokenizer( "standard" )
				.tokenFilters( "lowercase", "decompounder_german", "snowball_german", "asciifolding" );

		context.tokenFilter( "snowball_french" )
				.type( "snowball" )
				.param( "language", "French" );

		context.tokenFilter( "decompounder_german" )
				.type( "dictionary_decompounder" )
				.param( "word_list", "wieder", "vereinigung" );

		context.tokenFilter( "snowball_german" )
				.type( "snowball" )
				.param( "language", "German" );
	}
}
