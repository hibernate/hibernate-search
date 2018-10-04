/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.analysis;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisDefinitionContainerContext;

public class LibraryAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
	public static final String ANALYZER_DEFAULT = "default"; // No definition, just use the default from Elasticsearch
	public static final String NORMALIZER_SORT = "asciifolding_lowercase";

	@Override
	public void configure(ElasticsearchAnalysisDefinitionContainerContext context) {
		context.normalizer( NORMALIZER_SORT ).custom()
				.withTokenFilters( "asciifolding", "lowercase" );
	}
}
