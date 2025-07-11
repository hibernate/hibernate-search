/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationOptionsStep;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationValueStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.util.common.impl.Contracts;

class TermsAggregationOptionsStepImpl<SR, PDF extends TypedSearchPredicateFactory<SR>, F, V>
		implements TermsAggregationValueStep<SR, TermsAggregationOptionsStepImpl<SR, PDF, F, V>, PDF, F, Map<F, V>> {
	private final TermsAggregationBuilder<F, V> builder;
	private final SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext;

	TermsAggregationOptionsStepImpl(TermsAggregationBuilder<F, V> builder,
			SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext) {
		this.builder = builder;
		this.dslContext = dslContext;
	}

	@Override
	public TermsAggregationOptionsStepImpl<SR, PDF, F, V> orderByCountDescending() {
		builder.orderByCountDescending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<SR, PDF, F, V> orderByCountAscending() {
		builder.orderByCountAscending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<SR, PDF, F, V> orderByTermAscending() {
		builder.orderByTermAscending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<SR, PDF, F, V> orderByTermDescending() {
		builder.orderByTermDescending();
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<SR, PDF, F, V> minDocumentCount(int minDocumentCount) {
		Contracts.assertPositiveOrZero( minDocumentCount, "minDocumentCount" );
		builder.minDocumentCount( minDocumentCount );
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<SR, PDF, F, V> maxTermCount(int maxTermCount) {
		Contracts.assertStrictlyPositive( maxTermCount, "maxTermCount" );
		builder.maxTermCount( maxTermCount );
		return this;
	}

	@Override
	public TermsAggregationOptionsStepImpl<SR, PDF, F, V> filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.predicateFactory() ).toPredicate();

		return filter( predicate );
	}

	@Override
	public TermsAggregationOptionsStepImpl<SR, PDF, F, V> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}

	@Override
	public SearchAggregation<Map<F, V>> toAggregation() {
		return builder.build();
	}

	@Override
	public <T> TermsAggregationOptionsStep<SR, ?, PDF, F, Map<F, T>> value(SearchAggregation<T> aggregation) {
		return new TermsAggregationOptionsStepImpl<>( builder.withValue( aggregation ), dslContext );
	}
}
