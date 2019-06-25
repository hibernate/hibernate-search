/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;

public class ElasticsearchNormalizerManagementITAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
	@Override
	public void configure(ElasticsearchAnalysisConfigurationContext context) {
		context.normalizer( "custom-normalizer" ).custom()
				.withCharFilters( "custom-char-mapping" )
				.withTokenFilters( "custom-elision" );
		context.charFilter( "custom-char-mapping" )
				.type( "mapping" )
				.param( "mappings", new String[] { "foo => bar" } ); // Explicit array to get a JSON array
		context.tokenFilter( "custom-elision" )
				.type( "elision" )
				.param( "articles", "l", "d" );
	}
}
