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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFieldSetContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFromContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.logging.impl.LoggerFactory;


class RangePredicateFieldSetContextImpl<B>
		implements RangePredicateFieldSetContext, AbstractMultiFieldPredicateCommonState.FieldSetContext<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState<B> commonState;
	private final RangePredicateFromContextImpl<B> fromContext;

	private final List<String> absoluteFieldPaths;
	private final List<RangePredicateBuilder<B>> predicateBuilders = new ArrayList<>();

	RangePredicateFieldSetContextImpl(CommonState<B> commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = absoluteFieldPaths;
		SearchPredicateBuilderFactory<?, B> predicateFactory = commonState.getFactory();
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			predicateBuilders.add( predicateFactory.range( absoluteFieldPath ) );
		}
		this.fromContext = new RangePredicateFromContextImpl<>( commonState );
	}

	@Override
	public RangePredicateFieldSetContext orFields(String... absoluteFieldPaths) {
		return new RangePredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public RangePredicateFieldSetContext boostedTo(float boost) {
		predicateBuilders.forEach( b -> b.boost( boost ) );
		return this;
	}

	@Override
	public RangePredicateFromContext from(Object value) {
		return fromContext.from( value );
	}

	@Override
	public RangePredicateTerminalContext above(Object value) {
		return commonState.above( value );
	}

	@Override
	public RangePredicateTerminalContext below(Object value) {
		return commonState.below( value );
	}

	@Override
	public void contributePredicateBuilders(Consumer<B> collector) {
		for ( RangePredicateBuilder<B> predicateBuilder : predicateBuilders ) {
			collector.accept( predicateBuilder.toImplementation() );
		}
	}

	static class CommonState<B> extends AbstractMultiFieldPredicateCommonState<B, RangePredicateFieldSetContextImpl<B>> implements RangePredicateTerminalContext {

		private boolean hasNonNullBound = false;

		// excludeLimit in from/above means excluding the lower limit
		// excludeLimit in to/below means excluding the upper one
		protected boolean excludeUpperLimit = false;

		CommonState(SearchPredicateBuilderFactory<?, B> factory) {
			super( factory );
		}

		@Override
		public SearchPredicateTerminalContext excludeLimit() {
			getQueryBuilders().forEach( ( excludeUpperLimit ) ? RangePredicateBuilder::excludeUpperLimit : RangePredicateBuilder::excludeLowerLimit );
			return this;
		}

		@Override
		protected B toImplementation() {
			// Just in case from() was called, but not to()
			checkHasNonNullBound();
			return super.toImplementation();
		}

		RangePredicateTerminalContext above(Object value) {
			excludeUpperLimit = false;
			if ( value != null ) {
				hasNonNullBound = true;
				getQueryBuilders().forEach( q -> q.lowerLimit( value ) );
			}
			checkHasNonNullBound();
			return this;
		}

		RangePredicateTerminalContext below(Object value) {
			excludeUpperLimit = true;
			if ( value != null ) {
				hasNonNullBound = true;
				getQueryBuilders().forEach( q -> q.upperLimit( value ) );
			}
			checkHasNonNullBound();
			return this;
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

	static class RangePredicateFromContextImpl<B> implements RangePredicateFromContext {

		private final CommonState<B> delegate;

		RangePredicateFromContextImpl(CommonState<B> delegate) {
			this.delegate = delegate;
		}

		@Override
		public RangePredicateTerminalContext to(Object value) {
			return delegate.below( value );
		}

		@Override
		public RangePredicateFromContext excludeLimit() {
			delegate.getQueryBuilders().forEach( RangePredicateBuilder::excludeLowerLimit );
			return this;
		}

		RangePredicateFromContext from(Object value) {
			if ( value != null ) {
				delegate.hasNonNullBound = true;
				delegate.getQueryBuilders().forEach( q -> q.lowerLimit( value ) );
			}
			return this;
		}
	}
}
