/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.common.RewriteMethod;
import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.CommonQueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.QueryStringPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class QueryStringPredicateFieldMoreStepImpl
		implements
		QueryStringPredicateFieldMoreStep<QueryStringPredicateFieldMoreStepImpl, QueryStringPredicateOptionsStep<?>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState commonState;

	private final List<CommonQueryStringPredicateBuilder.FieldState> fieldStates = new ArrayList<>();

	QueryStringPredicateFieldMoreStepImpl(CommonState commonState, List<String> fieldPaths) {
		this.commonState = commonState;
		for ( String fieldPath : fieldPaths ) {
			fieldStates.add( commonState.field( fieldPath ) );
		}
	}

	@Override
	public QueryStringPredicateFieldMoreStepImpl fields(String... fieldPaths) {
		return new QueryStringPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}

	@Override
	public QueryStringPredicateFieldMoreStepImpl boost(float boost) {
		fieldStates.forEach( c -> c.boost( boost ) );
		return this;
	}

	@Override
	public QueryStringPredicateOptionsStep<?> matching(String queryString) {
		return commonState.matching( queryString );
	}


	static class CommonState
			extends
			AbstractStringQueryPredicateCommonState<CommonState,
					QueryStringPredicateOptionsStep<CommonState>,
					QueryStringPredicateBuilder>
			implements QueryStringPredicateOptionsStep<CommonState> {

		private static final Set<RewriteMethod> PARAMETERIZED_REWRITE_METHODS = EnumSet.of(
				RewriteMethod.TOP_TERMS_BOOST_N,
				RewriteMethod.TOP_TERMS_BLENDED_FREQS_N,
				RewriteMethod.TOP_TERMS_N
		);


		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
		}

		@Override
		protected QueryStringPredicateBuilder createBuilder(SearchPredicateDslContext<?> dslContext) {
			return dslContext.scope().predicateBuilders().queryString();
		}

		@Override
		public CommonState allowLeadingWildcard(boolean allowLeadingWildcard) {
			builder.allowLeadingWildcard( allowLeadingWildcard );
			return this;
		}

		@Override
		public CommonState enablePositionIncrements(boolean enablePositionIncrements) {
			builder.enablePositionIncrements( enablePositionIncrements );
			return this;
		}

		@Override
		public CommonState phraseSlop(Integer phraseSlop) {
			builder.phraseSlop( phraseSlop );
			return this;
		}

		@Override
		public CommonState rewriteMethod(RewriteMethod rewriteMethod) {
			if ( PARAMETERIZED_REWRITE_METHODS.contains( rewriteMethod ) ) {
				throw log.parameterizedRewriteMethodWithoutParameter( rewriteMethod );
			}
			builder.rewriteMethod( rewriteMethod, null );
			return this;
		}

		@Override
		public CommonState rewriteMethod(RewriteMethod rewriteMethod, int n) {
			if ( !PARAMETERIZED_REWRITE_METHODS.contains( rewriteMethod ) ) {
				throw log.nonParameterizedRewriteMethodWithParameter( rewriteMethod );
			}
			builder.rewriteMethod( rewriteMethod, n );
			return this;
		}

		@Override
		protected CommonState thisAsT() {
			return this;
		}

	}
}
