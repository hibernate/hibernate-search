/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AvgAggregationOptionsStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchFilterableAggregationBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

class AvgAggregationOptionsStepImpl<PDF extends SearchPredicateFactory>
		implements AvgAggregationOptionsStep<AvgAggregationOptionsStepImpl<PDF>, PDF> {
	private final SearchFilterableAggregationBuilder<Double> builder;
	private final SearchAggregationDslContext<?, ? extends PDF> dslContext;

	AvgAggregationOptionsStepImpl(SearchFilterableAggregationBuilder<Double> builder,
			SearchAggregationDslContext<?, ? extends PDF> dslContext) {
		this.builder = builder;
		this.dslContext = dslContext;
	}

	@Override
	public AvgAggregationOptionsStepImpl<PDF> filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.predicateFactory() ).toPredicate();
		return filter( predicate );
	}

	@Override
	public AvgAggregationOptionsStepImpl<PDF> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}

	@Override
	public SearchAggregation<Double> toAggregation() {
		return builder.build();
	}
}
