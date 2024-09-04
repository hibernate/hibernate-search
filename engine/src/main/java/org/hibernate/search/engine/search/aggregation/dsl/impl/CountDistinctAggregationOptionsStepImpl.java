/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.CountDistinctAggregationOptionsStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchFilterableAggregationBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

class CountDistinctAggregationOptionsStepImpl<PDF extends SearchPredicateFactory>
		implements CountDistinctAggregationOptionsStep<CountDistinctAggregationOptionsStepImpl<PDF>, PDF> {
	private final SearchFilterableAggregationBuilder<Long> builder;
	private final SearchAggregationDslContext<?, ? extends PDF> dslContext;

	CountDistinctAggregationOptionsStepImpl(SearchFilterableAggregationBuilder<Long> builder,
			SearchAggregationDslContext<?, ? extends PDF> dslContext) {
		this.builder = builder;
		this.dslContext = dslContext;
	}

	@Override
	public CountDistinctAggregationOptionsStepImpl<PDF> filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.predicateFactory() ).toPredicate();
		return filter( predicate );
	}

	@Override
	public CountDistinctAggregationOptionsStepImpl<PDF> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}

	@Override
	public SearchAggregation<Long> toAggregation() {
		return builder.build();
	}
}
