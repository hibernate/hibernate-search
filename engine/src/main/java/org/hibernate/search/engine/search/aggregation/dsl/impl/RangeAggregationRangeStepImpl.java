/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.impl.Contracts;

class RangeAggregationRangeStepImpl<E, PDF extends SearchPredicateFactory<E>, F>
		implements RangeAggregationRangeStep<E, RangeAggregationRangeStepImpl<E, PDF, F>, PDF, F>,
		RangeAggregationRangeMoreStep<E,
				RangeAggregationRangeStepImpl<E, PDF, F>,
				RangeAggregationRangeStepImpl<E, PDF, F>,
				PDF,
				F> {
	private final RangeAggregationBuilder<F> builder;
	private final SearchAggregationDslContext<E, ?, ? extends PDF> dslContext;

	RangeAggregationRangeStepImpl(RangeAggregationBuilder<F> builder,
			SearchAggregationDslContext<E, ?, ? extends PDF> dslContext) {
		this.builder = builder;
		this.dslContext = dslContext;
	}

	@Override
	public RangeAggregationRangeStepImpl<E, PDF, F> range(Range<? extends F> range) {
		Contracts.assertNotNull( range, "range" );
		builder.range( range );
		return this;
	}

	@Override
	public RangeAggregationRangeStepImpl<E, PDF, F> ranges(Collection<? extends Range<? extends F>> ranges) {
		Contracts.assertNotNull( ranges, "ranges" );
		for ( Range<? extends F> range : ranges ) {
			range( range );
		}
		return this;
	}

	@Override
	public RangeAggregationRangeStepImpl<E, PDF, F> filter(
			Function<? super PDF, ? extends PredicateFinalStep> clauseContributor) {
		SearchPredicate predicate = clauseContributor.apply( dslContext.predicateFactory() ).toPredicate();

		return filter( predicate );
	}

	@Override
	public RangeAggregationRangeStepImpl<E, PDF, F> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}

	@Override
	public SearchAggregation<Map<Range<F>, Long>> toAggregation() {
		return builder.build();
	}
}
