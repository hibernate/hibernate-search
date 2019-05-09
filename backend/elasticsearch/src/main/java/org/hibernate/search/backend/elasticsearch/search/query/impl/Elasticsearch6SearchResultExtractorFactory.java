/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;

public class Elasticsearch6SearchResultExtractorFactory implements ElasticsearchSearchResultExtractorFactory {
	@Override
	public <H> ElasticsearchSearchResultExtractor<H> createResultExtractor(LoadingContext<?, ?> loadingContext,
			ElasticsearchSearchProjection<?, H> rootProjection,
			SearchProjectionExtractContext searchProjectionExecutionContext) {
		return new Elasticsearch6SearchResultExtractor<>( loadingContext, rootProjection,
				searchProjectionExecutionContext );
	}
}
