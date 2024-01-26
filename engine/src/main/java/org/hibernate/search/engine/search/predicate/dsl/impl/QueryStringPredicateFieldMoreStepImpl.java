/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.common.RewriteMethod;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.MinimumShouldMatchConditionStep;
import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.CommonQueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.QueryStringPredicateBuilder;
import org.hibernate.search.util.common.impl.Contracts;
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
	public QueryStringPredicateOptionsStep<?> matching(String QueryString) {
		return commonState.matching( QueryString );
	}

	static class CommonState extends AbstractPredicateFinalStep
			implements QueryStringPredicateOptionsStep<CommonState> {

		private static final Set<RewriteMethod> PARAMETERIZED_REWRITE_METHODS = EnumSet.of(
				RewriteMethod.TOP_TERMS_BOOST_N,
				RewriteMethod.TOP_TERMS_BLENDED_FREQS_N,
				RewriteMethod.TOP_TERMS_N
		);

		private final QueryStringPredicateBuilder builder;
		private final MinimumShouldMatchConditionStepImpl<CommonState> minimumShouldMatchStep;

		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
			this.builder = dslContext.scope().predicateBuilders().queryString();
			this.minimumShouldMatchStep = new MinimumShouldMatchConditionStepImpl<>( builder, this );
		}

		@Override
		protected SearchPredicate build() {
			return builder.build();
		}

		CommonQueryStringPredicateBuilder.FieldState field(String fieldPath) {
			return builder.field( fieldPath );
		}

		private QueryStringPredicateOptionsStep<?> matching(String queryString) {
			Contracts.assertNotNull( queryString, "queryString" );
			builder.queryString( queryString );
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
		public MinimumShouldMatchConditionStep<? extends CommonState> minimumShouldMatch() {
			return minimumShouldMatchStep;
		}

		@Override
		public CommonState minimumShouldMatch(Consumer<? super MinimumShouldMatchConditionStep<?>> constraintContributor) {
			constraintContributor.accept( minimumShouldMatchStep );
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
	}
}
