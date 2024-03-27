/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregation;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.engine.search.aggregation.AggregationKey;

/**
 * @see Elasticsearch7SearchResultExtractor
 */
public class Elasticsearch7SearchResultExtractorFactory implements ElasticsearchSearchResultExtractorFactory {
	@Override
	public <H> ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> createResultExtractor(
			ElasticsearchSearchQueryRequestContext requestContext,
			ElasticsearchSearchProjection.Extractor<?, H> rootExtractor,
			Map<AggregationKey<?>, ElasticsearchSearchAggregation<?>> aggregations) {
		return new Elasticsearch7SearchResultExtractor<>(
				requestContext,
				rootExtractor, aggregations
		);
	}
}
