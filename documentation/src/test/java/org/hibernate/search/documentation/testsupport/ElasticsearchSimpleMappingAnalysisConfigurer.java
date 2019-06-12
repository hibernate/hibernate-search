/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisDefinitionContainerContext;

class ElasticsearchSimpleMappingAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
	@Override
	public void configure(ElasticsearchAnalysisDefinitionContainerContext context) {
		context.analyzer( "english" ).custom()
				.withTokenizer( "standard" )
				.withTokenFilters( "asciifolding", "lowercase", "snowball_english" );

		context.tokenFilter( "snowball_english" )
				.type( "snowball" )
				.param( "language", "English" );

		context.analyzer( "name" ).custom()
				.withTokenizer( "standard" )
				.withTokenFilters( "asciifolding", "lowercase" );

		context.analyzer( "autocomplete_indexing" ).custom()
				.withTokenizer( "standard" )
				.withTokenFilters( "asciifolding", "lowercase", "snowball_english", "autocomplete_edge_ngram" );

		context.tokenFilter( "autocomplete_edge_ngram" )
				.type( "edge_ngram" )
				.param( "min_gram", 3 )
				.param( "max_gram", 7 );

		// Same as "autocomplete-indexing", but without the edge-ngram filter
		context.analyzer( "autocomplete_query" ).custom()
				.withTokenizer( "standard" )
				.withTokenFilters( "asciifolding", "lowercase", "snowball_english" );

		// Normalizers

		context.normalizer( "english" ).custom()
				.withTokenFilters( "asciifolding", "lowercase" );

		context.normalizer( "name" ).custom()
				.withTokenFilters( "asciifolding", "lowercase" );
	}
}
