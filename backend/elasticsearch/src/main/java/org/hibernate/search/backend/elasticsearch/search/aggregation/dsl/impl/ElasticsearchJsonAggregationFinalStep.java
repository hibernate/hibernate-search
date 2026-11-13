package org.hibernate.search.backend.elasticsearch.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;


final class ElasticsearchJsonAggregationFinalStep<T>
		implements AggregationFinalStep<T> {
	private final SearchAggregationBuilder<T> builder;

	ElasticsearchJsonAggregationFinalStep(SearchAggregationBuilder<T> builder) {
		this.builder = builder;
	}

	@Override
	public SearchAggregation<T> toAggregation() {
		return builder.build();
	}
}
