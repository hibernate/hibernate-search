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

import org.hibernate.search.backend.elasticsearch.search.clause.impl.ElasticsearchClauseFactory;
import org.hibernate.search.backend.elasticsearch.search.clause.impl.MatchQueryClauseBuilder;
import org.hibernate.search.backend.elasticsearch.search.clause.impl.QueryClauseBuilder;
import org.hibernate.search.engine.search.dsl.MatchClauseFieldSetContext;
import org.hibernate.search.engine.search.dsl.MatchClauseTerminalContext;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class MatchClauseFieldSetContextImpl<N>
		implements MatchClauseFieldSetContext<N>, MultiFieldQueryClauseCommonState.FieldSetContext {

	private final CommonState<N> commonState;

	private final List<MatchQueryClauseBuilder> queryBuilders = new ArrayList<>();

	public MatchClauseFieldSetContextImpl(CommonState<N> commonState, List<String> fieldNames) {
		this.commonState = commonState;
		this.commonState.add( this );
		ElasticsearchClauseFactory clauseFactory =
				commonState.getTargetContext().getClauseFactory();
		for ( String fieldName : fieldNames ) {
			queryBuilders.add( clauseFactory.match( fieldName ) );
		}
	}

	@Override
	public MatchClauseFieldSetContext<N> orFields(String... fields) {
		return new MatchClauseFieldSetContextImpl<>( commonState, Arrays.asList( fields ) );
	}

	@Override
	public MatchClauseFieldSetContext<N> boostedTo(float boost) {
		queryBuilders.forEach( b -> b.boost( boost ) );
		return this;
	}

	@Override
	public MatchClauseTerminalContext<N> matching(Object value) {
		return commonState.matching( value );
	}

	@Override
	public void contribute(Consumer<JsonObject> collector) {
		queryBuilders.stream().map( QueryClauseBuilder::build ).forEach( collector );
	}

	public static class CommonState<N> extends MultiFieldQueryClauseCommonState<N, MatchClauseFieldSetContextImpl<N>>
			implements MatchClauseTerminalContext<N> {

		public CommonState(QueryTargetContext targetContext, Supplier<N> nextContextProvider) {
			super( targetContext, nextContextProvider );
		}

		public MatchClauseTerminalContext<N> matching(Object value) {
			getQueryBuilders().forEach( b -> b.value( value ) );
			return this;
		}

		private Stream<MatchQueryClauseBuilder> getQueryBuilders() {
			return getFieldSetContexts().stream().flatMap( f -> f.queryBuilders.stream() );
		}

		@Override
		public N end() {
			return getNextContextProvider().get();
		}

	}

}
