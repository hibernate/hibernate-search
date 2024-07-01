/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.MaxAggregationOptionsStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.FieldMetricAggregationBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

class MaxAggregationOptionsStepImpl<PDF extends SearchPredicateFactory, F>
		implements MaxAggregationOptionsStep<MaxAggregationOptionsStepImpl<PDF, F>, PDF, F> {
	private final FieldMetricAggregationBuilder<F> builder;
	private final SearchAggregationDslContext<?, ? extends PDF> dslContext;

	MaxAggregationOptionsStepImpl(FieldMetricAggregationBuilder<F> builder,
			SearchAggregationDslContext<?, ? extends PDF> dslContext) {
		this.builder = builder;
		this.dslContext = dslContext;
	}

	@Override
	public MaxAggregationOptionsStepImpl<PDF, F> filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.predicateFactory() ).toPredicate();

		return filter( predicate );
	}

	@Override
	public MaxAggregationOptionsStepImpl<PDF, F> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}

	@Override
	public SearchAggregation<F> toAggregation() {
		return builder.build();
	}
}
