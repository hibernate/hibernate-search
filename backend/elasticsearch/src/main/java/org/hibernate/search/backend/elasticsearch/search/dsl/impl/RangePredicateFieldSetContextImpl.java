/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.dsl.predicate.RangeBoundInclusion;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFieldSetContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFromContext;


/**
 * @author Yoann Rodiere
 */
class RangePredicateFieldSetContextImpl<N, C>
		implements RangePredicateFieldSetContext<N>, MultiFieldPredicateCommonState.FieldSetContext<C> {

	private final CommonState<N, C> commonState;

	private final List<RangePredicateBuilder<C>> queryBuilders = new ArrayList<>();

	public RangePredicateFieldSetContextImpl(CommonState<N, C> commonState, List<String> fieldNames) {
		this.commonState = commonState;
		this.commonState.add( this );
		SearchPredicateFactory<C> clauseFactory =
				commonState.getTargetContext().getSearchPredicateFactory();
		for ( String fieldName : fieldNames ) {
			queryBuilders.add( clauseFactory.range( fieldName ) );
		}
	}

	@Override
	public RangePredicateFieldSetContext<N> orFields(String... fields) {
		return new RangePredicateFieldSetContextImpl<>( commonState, Arrays.asList( fields ) );
	}

	@Override
	public RangePredicateFieldSetContext<N> boostedTo(float boost) {
		queryBuilders.forEach( b -> b.boost( boost ) );
		return this;
	}

	@Override
	public RangePredicateFromContext<N> from(Object value, RangeBoundInclusion inclusion) {
		return commonState.from( value, inclusion );
	}

	@Override
	public N above(Object value, RangeBoundInclusion inclusion) {
		return commonState.above( value, inclusion );
	}

	@Override
	public N below(Object value, RangeBoundInclusion inclusion) {
		return commonState.below( value, inclusion );
	}

	@Override
	public void contributePredicateBuilders(Consumer<SearchPredicateBuilder<? super C>> collector) {
		queryBuilders.forEach( collector );
	}

	public static class CommonState<N, C> extends MultiFieldPredicateCommonState<N, C, RangePredicateFieldSetContextImpl<N, C>> {

		public CommonState(SearchTargetContext<C> targetContext, Supplier<N> nextContextProvider) {
			super( targetContext, nextContextProvider );
		}

		public RangePredicateFromContext<N> from(Object value, RangeBoundInclusion inclusion) {
			getQueryBuilders().forEach( q -> q.lowerLimit( value ) );
			switch ( inclusion ) {
				case EXCLUDED:
					getQueryBuilders().forEach( RangePredicateBuilder::excludeLowerLimit );
				case INCLUDED:
					break;
			}
			return new RangePredicateFromContext<N>() {
				@Override
				public N to(Object value, RangeBoundInclusion inclusion) {
					return below( value, inclusion );
				}
			};
		}

		public N above(Object value, RangeBoundInclusion inclusion) {
			getQueryBuilders().forEach( q -> q.lowerLimit( value ) );
			switch ( inclusion ) {
				case EXCLUDED:
					getQueryBuilders().forEach( RangePredicateBuilder::excludeLowerLimit );
				case INCLUDED:
					break;
			}
			return getNextContextProvider().get();
		}

		public N below(Object value, RangeBoundInclusion inclusion) {
			getQueryBuilders().forEach( q -> q.upperLimit( value ) );
			switch ( inclusion ) {
				case EXCLUDED:
					getQueryBuilders().forEach( RangePredicateBuilder::excludeUpperLimit );
				case INCLUDED:
					break;
			}
			return getNextContextProvider().get();
		}

		private Stream<RangePredicateBuilder<C>> getQueryBuilders() {
			return getFieldSetContexts().stream().flatMap( f -> f.queryBuilders.stream() );
		}

	}

}
