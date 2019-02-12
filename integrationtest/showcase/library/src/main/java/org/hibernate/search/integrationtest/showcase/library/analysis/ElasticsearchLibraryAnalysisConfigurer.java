/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.analysis;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisDefinitionContainerContext;

import org.springframework.stereotype.Component;

@Component("elasticsearchAnalysisConfigurer")
public class ElasticsearchLibraryAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {

	@Override
	public void configure(ElasticsearchAnalysisDefinitionContainerContext context) {
		context.normalizer( LibraryAnalyzers.NORMALIZER_SORT ).custom()
				.withTokenFilters( "asciifolding", "lowercase" );
		context.normalizer( LibraryAnalyzers.NORMALIZER_ISBN ).custom()
				.withCharFilters( "removeHyphens" );
		context.charFilter( "removeHyphens" ).type( "pattern_replace" )
				.param( "pattern", "-+" )
				.param( "replacement", "" );
	}
}
