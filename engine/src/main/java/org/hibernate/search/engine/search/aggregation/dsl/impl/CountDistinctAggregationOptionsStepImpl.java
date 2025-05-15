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
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;

class CountDistinctAggregationOptionsStepImpl<SR, PDF extends TypedSearchPredicateFactory<SR>>
		implements CountDistinctAggregationOptionsStep<SR, CountDistinctAggregationOptionsStepImpl<SR, PDF>, PDF> {
	private final SearchFilterableAggregationBuilder<Long> builder;
	private final SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext;

	CountDistinctAggregationOptionsStepImpl(SearchFilterableAggregationBuilder<Long> builder,
			SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext) {
		this.builder = builder;
		this.dslContext = dslContext;
	}

	@Override
	public CountDistinctAggregationOptionsStepImpl<SR, PDF> filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.predicateFactory() ).toPredicate();
		return filter( predicate );
	}

	@Override
	public CountDistinctAggregationOptionsStepImpl<SR, PDF> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}

	@Override
	public SearchAggregation<Long> toAggregation() {
		return builder.build();
	}
}
