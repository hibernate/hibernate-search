/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregation;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.engine.search.aggregation.AggregationKey;

public class Elasticsearch6SearchResultExtractorFactory implements ElasticsearchSearchResultExtractorFactory {
	@Override
	public <H> ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> createResultExtractor(
			ElasticsearchSearchQueryRequestContext requestContext,
			ElasticsearchSearchProjection<?, H> rootProjection,
			Map<AggregationKey<?>, ElasticsearchSearchAggregation<?>> aggregations) {
		return new Elasticsearch6SearchResultExtractor<>(
				requestContext,
				rootProjection, aggregations
		);
	}
}
