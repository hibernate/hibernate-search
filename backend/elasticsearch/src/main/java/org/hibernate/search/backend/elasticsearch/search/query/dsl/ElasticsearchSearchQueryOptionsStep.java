/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.query.dsl;

import org.hibernate.search.backend.elasticsearch.search.aggregation.dsl.ElasticsearchSearchAggregationFactory;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchFetchable;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchRequestTransformer;
import org.hibernate.search.backend.elasticsearch.search.sort.dsl.ElasticsearchSearchSortFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.util.common.annotation.Incubating;

public interface ElasticsearchSearchQueryOptionsStep<H, LOS>
		extends SearchQueryOptionsStep<
				ElasticsearchSearchQueryOptionsStep<H, LOS>,
				H,
				LOS,
				ElasticsearchSearchSortFactory,
				ElasticsearchSearchAggregationFactory>,
		ElasticsearchSearchFetchable<H> {

	/**
	 * Set the {@link ElasticsearchSearchRequestTransformer} for this search query.
	 * <p>
	 * <strong>WARNING:</strong> Direct changes to the request may conflict with Hibernate Search features
	 * and be supported differently by different versions of Elasticsearch.
	 * Thus they cannot be guaranteed to continue to work when upgrading Hibernate Search,
	 * even for micro upgrades ({@code x.y.z} to {@code x.y.(z+1)}).
	 * Use this at your own risk.
	 *
	 * @param transformer The search request transformer.
	 * @return {@code this}, for method chaining.
	 */
	@Incubating
	ElasticsearchSearchQueryOptionsStep<H, LOS> requestTransformer(ElasticsearchSearchRequestTransformer transformer);

	@Override
	ElasticsearchSearchQuery<H> toQuery();
}
