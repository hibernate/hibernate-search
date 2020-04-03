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
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationRangeMoreStep;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationRangeStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.impl.DefaultSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.impl.Contracts;

class RangeAggregationRangeStepImpl<F>
		implements RangeAggregationRangeStep<RangeAggregationRangeStepImpl<F>, F>,
				RangeAggregationRangeMoreStep<RangeAggregationRangeStepImpl<F>, RangeAggregationRangeStepImpl<F>, F> {
	private final RangeAggregationBuilder<F> builder;
	private final SearchAggregationDslContext<?> dslContext;

	RangeAggregationRangeStepImpl(RangeAggregationBuilder<F> builder, SearchAggregationDslContext<?> dslContext) {
		this.builder = builder;
		this.dslContext = dslContext;
	}

	@Override
	public RangeAggregationRangeStepImpl<F> range(Range<? extends F> range) {
		Contracts.assertNotNull( range, "range" );
		builder.range( range );
		return this;
	}

	@Override
	public RangeAggregationRangeStepImpl<F> ranges(Collection<? extends Range<? extends F>> ranges) {
		Contracts.assertNotNull( ranges, "ranges" );
		for ( Range<? extends F> range : ranges ) {
			range( range );
		}
		return this;
	}

	@Override
	public RangeAggregationRangeStepImpl<F> filter(
		Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {

		SearchAggregationDslContext<?> ctx = dslContext;
		SearchPredicateBuilderFactory predicateBuilderFactory = ctx.getPredicateBuilderFactory();
		SearchPredicateFactory factory = new DefaultSearchPredicateFactory<>( predicateBuilderFactory );
		SearchPredicate predicate = clauseContributor.apply( extendPredicateFactory( factory ) ).toPredicate();

		filter( predicate );
		return this;
	}

	@Override
	public RangeAggregationRangeStepImpl<F> filter(SearchPredicate searchPredicate) {
		SearchAggregationDslContext<?> ctx = dslContext;
		SearchPredicateBuilderFactory predicateBuilderFactory = ctx.getPredicateBuilderFactory();
		searchPredicate = (SearchPredicate) predicateBuilderFactory.toImplementation( searchPredicate );

		builder.filter( searchPredicate );
		return this;
	}

	protected SearchPredicateFactory extendPredicateFactory(SearchPredicateFactory predicateFactory) {
		return predicateFactory;
	}

	@Override
	public SearchAggregation<Map<Range<F>, Long>> toAggregation() {
		return builder.build();
	}
}
