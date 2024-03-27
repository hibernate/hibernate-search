/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;

import com.google.gson.JsonObject;

final class ElasticsearchJsonAggregationFinalStep
		implements AggregationFinalStep<JsonObject> {
	private final SearchAggregationBuilder<JsonObject> builder;

	ElasticsearchJsonAggregationFinalStep(SearchAggregationBuilder<JsonObject> builder) {
		this.builder = builder;
	}

	@Override
	public SearchAggregation<JsonObject> toAggregation() {
		return builder.build();
	}
}
