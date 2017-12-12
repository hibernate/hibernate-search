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

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.MatchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.SearchPredicateBuilder;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateFieldSetContext;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class MatchPredicateFieldSetContextImpl<N>
		implements MatchPredicateFieldSetContext<N>, MultiFieldPredicateCommonState.FieldSetContext {

	private final CommonState<N> commonState;

	private final List<MatchPredicateBuilder> queryBuilders = new ArrayList<>();

	public MatchPredicateFieldSetContextImpl(CommonState<N> commonState, List<String> fieldNames) {
		this.commonState = commonState;
		this.commonState.add( this );
		ElasticsearchSearchPredicateFactory clauseFactory =
				commonState.getTargetContext().getSearchPredicateFactory();
		for ( String fieldName : fieldNames ) {
			queryBuilders.add( clauseFactory.match( fieldName ) );
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
	public void contribute(Consumer<JsonObject> collector) {
		queryBuilders.stream().map( SearchPredicateBuilder::build ).forEach( collector );
	}

	public static class CommonState<N> extends MultiFieldPredicateCommonState<N, MatchPredicateFieldSetContextImpl<N>> {

		public CommonState(SearchTargetContext targetContext, Supplier<N> nextContextProvider) {
			super( targetContext, nextContextProvider );
		}

		public N matching(Object value) {
			getQueryBuilders().forEach( b -> b.value( value ) );
			return getNextContextProvider().get();
		}

		private Stream<MatchPredicateBuilder> getQueryBuilders() {
			return getFieldSetContexts().stream().flatMap( f -> f.queryBuilders.stream() );
		}

	}

}
