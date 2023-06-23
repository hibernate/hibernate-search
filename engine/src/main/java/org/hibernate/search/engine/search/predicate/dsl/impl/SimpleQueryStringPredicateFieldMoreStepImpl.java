/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.util.common.impl.Contracts;

class SimpleQueryStringPredicateFieldMoreStepImpl
		implements SimpleQueryStringPredicateFieldMoreStep<
				SimpleQueryStringPredicateFieldMoreStepImpl,
				SimpleQueryStringPredicateOptionsStep<?>> {

	private final CommonState commonState;

	private final List<SimpleQueryStringPredicateBuilder.FieldState> fieldStates = new ArrayList<>();

	SimpleQueryStringPredicateFieldMoreStepImpl(CommonState commonState, List<String> fieldPaths) {
		this.commonState = commonState;
		for ( String fieldPath : fieldPaths ) {
			fieldStates.add( commonState.field( fieldPath ) );
		}
	}

	@Override
	public SimpleQueryStringPredicateFieldMoreStepImpl fields(String... fieldPaths) {
		return new SimpleQueryStringPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}

	@Override
	public SimpleQueryStringPredicateFieldMoreStepImpl boost(float boost) {
		fieldStates.forEach( c -> c.boost( boost ) );
		return this;
	}

	@Override
	public SimpleQueryStringPredicateOptionsStep<?> matching(String simpleQueryString) {
		return commonState.matching( simpleQueryString );
	}

	static class CommonState extends AbstractPredicateFinalStep
			implements SimpleQueryStringPredicateOptionsStep<CommonState> {

		private final SimpleQueryStringPredicateBuilder builder;

		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
			this.builder = dslContext.scope().predicateBuilders().simpleQueryString();
		}

		@Override
		protected SearchPredicate build() {
			return builder.build();
		}

		SimpleQueryStringPredicateBuilder.FieldState field(String fieldPath) {
			return builder.field( fieldPath );
		}

		private SimpleQueryStringPredicateOptionsStep<?> matching(String simpleQueryString) {
			Contracts.assertNotNull( simpleQueryString, "simpleQueryString" );
			builder.simpleQueryString( simpleQueryString );
			return this;
		}

		@Override
		public CommonState constantScore() {
			builder.constantScore();
			return this;
		}

		@Override
		public CommonState boost(float boost) {
			builder.boost( boost );
			return this;
		}

		@Override
		public CommonState defaultOperator(BooleanOperator operator) {
			builder.defaultOperator( operator );
			return this;
		}

		@Override
		public CommonState analyzer(String analyzerName) {
			builder.analyzer( analyzerName );
			return this;
		}

		@Override
		public CommonState skipAnalysis() {
			builder.skipAnalysis();
			return this;
		}

		@Override
		public CommonState flags(Set<SimpleQueryFlag> flags) {
			builder.flags( flags );
			return this;
		}

	}

}
