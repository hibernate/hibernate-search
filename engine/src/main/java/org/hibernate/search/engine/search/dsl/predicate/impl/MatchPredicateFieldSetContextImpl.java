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
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateFieldSetContext;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.util.spi.LoggerFactory;


class MatchPredicateFieldSetContextImpl<N, C>
		implements MatchPredicateFieldSetContext<N>, MultiFieldPredicateCommonState.FieldSetContext<C> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState<N, C> commonState;

	private final List<String> fieldNames;
	private final List<MatchPredicateBuilder<C>> queryBuilders = new ArrayList<>();

	MatchPredicateFieldSetContextImpl(CommonState<N, C> commonState, List<String> fieldNames) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.fieldNames = fieldNames;
		SearchPredicateFactory<C> predicateFactory = commonState.getFactory();
		for ( String fieldName : fieldNames ) {
			queryBuilders.add( predicateFactory.match( fieldName ) );
		}
	}

	@Override
	public MatchPredicateFieldSetContext<N> orFields(String... fields) {
		return new MatchPredicateFieldSetContextImpl<>( commonState, Arrays.asList( fields ) );
	}

	@Override
	public MatchPredicateFieldSetContext<N> boostedTo(float boost) {
		queryBuilders.forEach( b -> b.boost( boost ) );
		return this;
	}

	@Override
	public N matching(Object value) {
		return commonState.matching( value );
	}

	@Override
	public void contributePredicateBuilders(Consumer<SearchPredicateBuilder<? super C>> collector) {
		queryBuilders.forEach( collector );
	}

	static class CommonState<N, C> extends MultiFieldPredicateCommonState<N, C, MatchPredicateFieldSetContextImpl<N, C>> {

		CommonState(SearchPredicateFactory<C> factory, Supplier<N> nextContextProvider) {
			super( factory, nextContextProvider );
		}

		public N matching(Object value) {
			if ( value == null ) {
				throw log.matchPredicateCannotMatchNullValue( collectFieldNames() );
			}
			getQueryBuilders().forEach( b -> b.value( value ) );
			return getNextContextProvider().get();
		}

		private List<String> collectFieldNames() {
			return getFieldSetContexts().stream().flatMap( f -> f.fieldNames.stream() )
					.collect( Collectors.toList() );
		}

		private Stream<MatchPredicateBuilder<C>> getQueryBuilders() {
			return getFieldSetContexts().stream().flatMap( f -> f.queryBuilders.stream() );
		}

	}

}
