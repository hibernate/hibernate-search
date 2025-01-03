/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.CountAggregationOptionsStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchFilterableAggregationBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

class CountAggregationOptionsStepImpl<SR, PDF extends SearchPredicateFactory<SR>>
		implements CountAggregationOptionsStep<SR, CountAggregationOptionsStepImpl<SR, PDF>, PDF> {
	private final SearchFilterableAggregationBuilder<Long> builder;
	private final SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext;

	CountAggregationOptionsStepImpl(SearchFilterableAggregationBuilder<Long> builder,
			SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext) {
		this.builder = builder;
		this.dslContext = dslContext;
	}

	@Override
	public CountAggregationOptionsStepImpl<SR, PDF> filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.predicateFactory() ).toPredicate();
		return filter( predicate );
	}

	@Override
	public CountAggregationOptionsStepImpl<SR, PDF> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}

	@Override
	public SearchAggregation<Long> toAggregation() {
		return builder.build();
	}
}
