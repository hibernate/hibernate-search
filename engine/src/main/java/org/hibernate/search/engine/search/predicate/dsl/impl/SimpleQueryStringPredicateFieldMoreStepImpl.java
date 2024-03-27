/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.CommonQueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;

class SimpleQueryStringPredicateFieldMoreStepImpl
		implements SimpleQueryStringPredicateFieldMoreStep<
				SimpleQueryStringPredicateFieldMoreStepImpl,
				SimpleQueryStringPredicateOptionsStep<?>> {

	private final CommonState commonState;

	private final List<CommonQueryStringPredicateBuilder.FieldState> fieldStates = new ArrayList<>();

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
	public SimpleQueryStringPredicateOptionsStep<?> matching(String queryString) {
		return commonState.matching( queryString );
	}

	static class CommonState
			extends
			AbstractStringQueryPredicateCommonState<CommonState,
					SimpleQueryStringPredicateOptionsStep<CommonState>,
					SimpleQueryStringPredicateBuilder>
			implements SimpleQueryStringPredicateOptionsStep<CommonState> {

		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
		}

		@Override
		protected SimpleQueryStringPredicateBuilder createBuilder(SearchPredicateDslContext<?> dslContext) {
			return dslContext.scope().predicateBuilders().simpleQueryString();
		}

		@Override
		public CommonState flags(Set<SimpleQueryFlag> flags) {
			builder.flags( flags );
			return this;
		}

		@Override
		protected CommonState thisAsT() {
			return this;
		}

	}

}
