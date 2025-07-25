/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationRangeMoreStep;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationRangeStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.impl.Contracts;

class RangeAggregationRangeStepImpl<SR, PDF extends TypedSearchPredicateFactory<SR>, F, A>
		implements RangeAggregationRangeStep<SR, RangeAggregationRangeStepImpl<SR, PDF, F, A>, PDF, F, A>,
		RangeAggregationRangeMoreStep<SR,
				RangeAggregationRangeStepImpl<SR, PDF, F, A>,
				RangeAggregationRangeStepImpl<SR, PDF, F, A>,
				PDF,
				F,
				A> {
	private final RangeAggregationBuilder<F, A> builder;
	private final SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext;

	RangeAggregationRangeStepImpl(RangeAggregationBuilder<F, A> builder,
			SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext) {
		this.builder = builder;
		this.dslContext = dslContext;
	}

	@Override
	public RangeAggregationRangeStepImpl<SR, PDF, F, A> range(Range<? extends F> range) {
		Contracts.assertNotNull( range, "range" );
		builder.range( range );
		return this;
	}

	@Override
	public RangeAggregationRangeStepImpl<SR, PDF, F, A> ranges(Collection<? extends Range<? extends F>> ranges) {
		Contracts.assertNotNull( ranges, "ranges" );
		for ( Range<? extends F> range : ranges ) {
			range( range );
		}
		return this;
	}

	@Override
	public RangeAggregationRangeStepImpl<SR, PDF, F, A> filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.predicateFactory() ).toPredicate();

		return filter( predicate );
	}

	@Override
	public RangeAggregationRangeStepImpl<SR, PDF, F, A> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}

	@Override
	public SearchAggregation<Map<Range<F>, A>> toAggregation() {
		return builder.build();
	}

	@Override
	public <T> RangeAggregationRangeStepImpl<SR, PDF, F, T> value(SearchAggregation<T> aggregation) {
		return new RangeAggregationRangeStepImpl<>( builder.withValue( aggregation ), dslContext );
	}
}
