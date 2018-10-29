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
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateFieldSetContext;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;


class MatchPredicateFieldSetContextImpl<N, B>
		implements MatchPredicateFieldSetContext<N>, MultiFieldPredicateCommonState.FieldSetContext<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState<N, B> commonState;

	private final List<String> absoluteFieldPaths;
	private final List<MatchPredicateBuilder<B>> predicateBuilders = new ArrayList<>();

	MatchPredicateFieldSetContextImpl(CommonState<N, B> commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = absoluteFieldPaths;
		SearchPredicateFactory<?, B> predicateFactory = commonState.getFactory();
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			predicateBuilders.add( predicateFactory.match( absoluteFieldPath ) );
		}
	}

	@Override
	public MatchPredicateFieldSetContext<N> orFields(String... absoluteFieldPaths) {
		return new MatchPredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public MatchPredicateFieldSetContext<N> boostedTo(float boost) {
		predicateBuilders.forEach( b -> b.boost( boost ) );
		return this;
	}

	@Override
	public SearchPredicateTerminalContext<N> matching(Object value) {
		return commonState.matching( value );
	}

	@Override
	public void contributePredicateBuilders(Consumer<B> collector) {
		for ( MatchPredicateBuilder<B> predicateBuilder : predicateBuilders ) {
			collector.accept( predicateBuilder.toImplementation() );
		}
	}

	static class CommonState<N, B> extends MultiFieldPredicateCommonState<N, B, MatchPredicateFieldSetContextImpl<N, B>>
			implements SearchPredicateTerminalContext<N> {

		CommonState(SearchPredicateFactory<?, B> factory, Supplier<N> nextContextProvider) {
			super( factory, nextContextProvider );
		}

		public SearchPredicateTerminalContext<N> matching(Object value) {
			if ( value == null ) {
				throw log.matchPredicateCannotMatchNullValue( collectAbsoluteFieldPaths() );
			}
			getQueryBuilders().forEach( b -> b.value( value ) );
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

		private Stream<MatchPredicateBuilder<B>> getQueryBuilders() {
			return getFieldSetContexts().stream().flatMap( f -> f.predicateBuilders.stream() );
		}

	}

}
