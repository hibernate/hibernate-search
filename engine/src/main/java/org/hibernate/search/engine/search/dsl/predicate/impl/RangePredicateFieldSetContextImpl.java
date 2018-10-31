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
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.RangeBoundInclusion;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFieldSetContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFromContext;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;


class RangePredicateFieldSetContextImpl<N, B>
		implements RangePredicateFieldSetContext<N>, MultiFieldPredicateCommonState.FieldSetContext<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState<N, B> commonState;

	private final List<String> absoluteFieldPaths;
	private final List<RangePredicateBuilder<B>> predicateBuilders = new ArrayList<>();

	RangePredicateFieldSetContextImpl(CommonState<N, B> commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = absoluteFieldPaths;
		SearchPredicateFactory<?, B> predicateFactory = commonState.getFactory();
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			predicateBuilders.add( predicateFactory.range( absoluteFieldPath ) );
		}
	}

	@Override
	public RangePredicateFieldSetContext<N> orFields(String... absoluteFieldPaths) {
		return new RangePredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public RangePredicateFieldSetContext<N> boostedTo(float boost) {
		predicateBuilders.forEach( b -> b.boost( boost ) );
		return this;
	}

	@Override
	public RangePredicateFromContext<N> from(Object value, RangeBoundInclusion inclusion) {
		return commonState.from( value, inclusion );
	}

	@Override
	public SearchPredicateTerminalContext<N> above(Object value, RangeBoundInclusion inclusion) {
		return commonState.above( value, inclusion );
	}

	@Override
	public SearchPredicateTerminalContext<N> below(Object value, RangeBoundInclusion inclusion) {
		return commonState.below( value, inclusion );
	}

	@Override
	public void contributePredicateBuilders(Consumer<B> collector) {
		for ( RangePredicateBuilder<B> predicateBuilder : predicateBuilders ) {
			collector.accept( predicateBuilder.toImplementation() );
		}
	}

	static class CommonState<N, B> extends MultiFieldPredicateCommonState<N, B, RangePredicateFieldSetContextImpl<N, B>>
			implements RangePredicateFromContext<N>, SearchPredicateTerminalContext<N> {

		private boolean hasNonNullBound = false;

		CommonState(SearchPredicateFactory<?, B> factory, Supplier<N> nextContextProvider) {
			super( factory, nextContextProvider );
		}

		@Override
		protected B doContribute() {
			// Just in case from() was called, but not to()
			checkHasNonNullBound();
			return super.doContribute();
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
			return this;
		}

		@Override
		public SearchPredicateTerminalContext<N> to(Object value, RangeBoundInclusion inclusion) {
			return below( value, inclusion );
		}

		SearchPredicateTerminalContext<N> above(Object value, RangeBoundInclusion inclusion) {
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
			return this;
		}

		SearchPredicateTerminalContext<N> below(Object value, RangeBoundInclusion inclusion) {
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
			return this;
		}

		@Override
		public N end() {
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

		private Stream<RangePredicateBuilder<B>> getQueryBuilders() {
			return getFieldSetContexts().stream().flatMap( f -> f.predicateBuilders.stream() );
		}

	}

}
