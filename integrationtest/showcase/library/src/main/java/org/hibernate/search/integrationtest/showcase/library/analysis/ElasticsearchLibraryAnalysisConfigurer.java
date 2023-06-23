/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.analysis;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

import org.springframework.stereotype.Component;

@Component("elasticsearchAnalysisConfigurer")
public class ElasticsearchLibraryAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {

	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		context.normalizer( LibraryAnalyzers.NORMALIZER_SORT ).custom()
				.tokenFilters( "lowercase", "asciifolding" );
		context.normalizer( LibraryAnalyzers.NORMALIZER_ISBN ).custom()
				.charFilters( "removeHyphens" );
		context.charFilter( "removeHyphens" ).type( "pattern_replace" )
				.param( "pattern", "-+" )
				.param( "replacement", "" );
	}
}
