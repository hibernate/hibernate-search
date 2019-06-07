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
		context.analyzer( "english" ).type( "standard" );
		context.analyzer( "name" ).type( "standard" );
		context.normalizer( "english" ).custom()
				.withTokenFilters( "asciifolding", "lowercase" );
	}
}
