/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.dsl.predicate.RangeBoundInclusion;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFieldSetContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFromContext;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;


class RangePredicateFieldSetContextImpl<N, CTX, C>
		implements RangePredicateFieldSetContext<N>, MultiFieldPredicateCommonState.FieldSetContext<CTX, C> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState<N, CTX, C> commonState;

	private final List<String> absoluteFieldPaths;
	private final List<RangePredicateBuilder<CTX, C>> queryBuilders = new ArrayList<>();

	RangePredicateFieldSetContextImpl(CommonState<N, CTX, C> commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = absoluteFieldPaths;
		SearchPredicateFactory<CTX, C> predicateFactory = commonState.getFactory();
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			queryBuilders.add( predicateFactory.range( absoluteFieldPath ) );
		}
	}

	@Override
	public RangePredicateFieldSetContext<N> orFields(String... absoluteFieldPaths) {
		return new RangePredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
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
	public void contributePredicateBuilders(Consumer<SearchPredicateBuilder<CTX, ? super C>> collector) {
		queryBuilders.forEach( collector );
	}

	static class CommonState<N, CTX, C> extends MultiFieldPredicateCommonState<N, CTX, C, RangePredicateFieldSetContextImpl<N, CTX, C>> {

		private boolean hasNonNullBound = false;

		CommonState(SearchPredicateFactory<CTX, C> factory, Supplier<N> nextContextProvider) {
			super( factory, nextContextProvider );
		}

		@Override
		public void contribute(CTX context, C collector) {
			// Just in case from() was called, but not to()
			checkHasNonNullBound();
			super.contribute( context, collector );
		}

		RangePredicateFromContext<N> from(Object value, RangeBoundInclusion inclusion) {
			if ( value != null ) {
				hasNonNullBound = true;
				getQueryBuilders().forEach( q -> q.lowerLimit( value ) );
			}
			switch ( inclusion ) {
				case EXCLUDED:
					getQueryBuilders().forEach( RangePredicateBuilder::excludeLowerLimit );
					break;
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

		N above(Object value, RangeBoundInclusion inclusion) {
			if ( value != null ) {
				hasNonNullBound = true;
				getQueryBuilders().forEach( q -> q.lowerLimit( value ) );
			}
			switch ( inclusion ) {
				case EXCLUDED:
					getQueryBuilders().forEach( RangePredicateBuilder::excludeLowerLimit );
					break;
				case INCLUDED:
					break;
			}
			checkHasNonNullBound();
			return getNextContextProvider().get();
		}

		N below(Object value, RangeBoundInclusion inclusion) {
			if ( value != null ) {
				hasNonNullBound = true;
				getQueryBuilders().forEach( q -> q.upperLimit( value ) );
			}
			switch ( inclusion ) {
				case EXCLUDED:
					getQueryBuilders().forEach( RangePredicateBuilder::excludeUpperLimit );
					break;
				case INCLUDED:
					break;
			}
			checkHasNonNullBound();
			return getNextContextProvider().get();
		}

		private List<String> collectAbsoluteFieldPaths() {
			return getFieldSetContexts().stream().flatMap( f -> f.absoluteFieldPaths.stream() )
					.collect( Collectors.toList() );
		}

		private void checkHasNonNullBound() {
			if ( !hasNonNullBound ) {
				throw log.rangePredicateCannotMatchNullValue( collectAbsoluteFieldPaths() );
			}
		}

		private Stream<RangePredicateBuilder<CTX, C>> getQueryBuilders() {
			return getFieldSetContexts().stream().flatMap( f -> f.queryBuilders.stream() );
		}

	}

}
