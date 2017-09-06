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
import org.hibernate.search.backend.elasticsearch.search.clause.impl.QueryClauseBuilder;
import org.hibernate.search.backend.elasticsearch.search.clause.impl.RangeQueryClauseBuilder;
import org.hibernate.search.engine.search.dsl.RangeClauseFieldSetContext;
import org.hibernate.search.engine.search.dsl.RangeClauseFromContext;
import org.hibernate.search.engine.search.dsl.RangeClauseTerminalContext;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class RangeClauseFieldSetContextImpl<N>
		implements RangeClauseFieldSetContext<N>, MultiFieldQueryClauseCommonState.FieldSetContext {

	private final CommonState<N> commonState;

	private final List<RangeQueryClauseBuilder> queryBuilders = new ArrayList<>();

	public RangeClauseFieldSetContextImpl(CommonState<N> commonState, List<String> fieldNames) {
		this.commonState = commonState;
		this.commonState.add( this );
		ElasticsearchClauseFactory clauseFactory =
				commonState.getTargetContext().getClauseFactory();
		for ( String fieldName : fieldNames ) {
			queryBuilders.add( clauseFactory.range( fieldName ) );
		}
	}

	@Override
	public RangeClauseFieldSetContext<N> orFields(String... fields) {
		return new RangeClauseFieldSetContextImpl<>( commonState, Arrays.asList( fields ) );
	}

	@Override
	public RangeClauseFieldSetContext<N> boostedTo(float boost) {
		queryBuilders.forEach( b -> b.boost( boost ) );
		return this;
	}

	@Override
	public RangeClauseFromContext<N> from(Object value) {
		return commonState.from( value );
	}

	@Override
	public RangeClauseTerminalContext<N> above(Object value) {
		return commonState.above( value );
	}

	@Override
	public RangeClauseTerminalContext<N> below(Object value) {
		return commonState.below( value );
	}

	@Override
	public void contribute(Consumer<JsonObject> collector) {
		queryBuilders.stream().map( QueryClauseBuilder::build ).forEach( collector );
	}

	public static class CommonState<N> extends MultiFieldQueryClauseCommonState<N, RangeClauseFieldSetContextImpl<N>> {

		public CommonState(QueryTargetContext targetContext, Supplier<N> nextContextProvider) {
			super( targetContext, nextContextProvider );
		}

		public RangeClauseFromContext<N> from(Object value) {
			RangeClauseTerminalContext<N> above = above( value );
			return new RangeClauseFromContext<N>() {
				@Override
				public RangeClauseFromContext<N> excludeLimit() {
					above.excludeLimit();
					return this;
				}

				@Override
				public RangeClauseTerminalContext<N> to(Object value) {
					return below( value );
				}
			};
		}

		public RangeClauseTerminalContext<N> above(Object value) {
			getQueryBuilders().forEach( q -> q.lowerLimit( value ) );
			return new RangeClauseTerminalContext<N>() {
				@Override
				public RangeClauseTerminalContext<N> excludeLimit() {
					getQueryBuilders().forEach( q -> q.excludeLowerLimit() );
					return this;
				}

				@Override
				public N end() {
					return getNextContextProvider().get();
				}
			};
		}

		public RangeClauseTerminalContext<N> below(Object value) {
			getQueryBuilders().forEach( q -> q.upperLimit( value ) );
			return new RangeClauseTerminalContext<N>() {
				@Override
				public RangeClauseTerminalContext<N> excludeLimit() {
					getQueryBuilders().forEach( q -> q.excludeUpperLimit() );
					return this;
				}

				@Override
				public N end() {
					return getNextContextProvider().get();
				}
			};
		}

		private Stream<RangeQueryClauseBuilder> getQueryBuilders() {
			return getFieldSetContexts().stream().flatMap( f -> f.queryBuilders.stream() );
		}

	}

}
