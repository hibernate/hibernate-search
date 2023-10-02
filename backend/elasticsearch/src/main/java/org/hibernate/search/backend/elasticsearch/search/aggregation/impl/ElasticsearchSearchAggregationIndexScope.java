/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationIndexScope;

public interface ElasticsearchSearchAggregationIndexScope<S extends ElasticsearchSearchAggregationIndexScope<?>>
		extends SearchAggregationIndexScope<S> {

	@Override
	ElasticsearchSearchAggregationBuilderFactory aggregationBuilders();

}
