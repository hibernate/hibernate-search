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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class SimpleQueryStringPredicateFieldMoreStepImpl
		implements SimpleQueryStringPredicateFieldMoreStep<
				SimpleQueryStringPredicateFieldMoreStepImpl,
				SimpleQueryStringPredicateOptionsStep<?>
		> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState commonState;

	private final List<String> absoluteFieldPaths;
	private final List<SimpleQueryStringPredicateBuilder.FieldState> fieldStates = new ArrayList<>();

	SimpleQueryStringPredicateFieldMoreStepImpl(CommonState commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = absoluteFieldPaths;
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			fieldStates.add( commonState.field( absoluteFieldPath ) );
		}
	}

	@Override
	public SimpleQueryStringPredicateFieldMoreStepImpl fields(String... absoluteFieldPaths) {
		return new SimpleQueryStringPredicateFieldMoreStepImpl( commonState, Arrays.asList( absoluteFieldPaths ) );
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

		private final List<SimpleQueryStringPredicateFieldMoreStepImpl> fieldSetStates = new ArrayList<>();

		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
			this.builder = dslContext.builderFactory().simpleQueryString();
		}

		@Override
		protected SearchPredicate build() {
			return builder.build();
		}

		void add(SimpleQueryStringPredicateFieldMoreStepImpl fieldSetState) {
			fieldSetStates.add( fieldSetState );
		}

		SimpleQueryStringPredicateBuilder.FieldState field(String absoluteFieldPath) {
			return builder.field( absoluteFieldPath );
		}

		private SimpleQueryStringPredicateOptionsStep<?> matching(String simpleQueryString) {
			if ( simpleQueryString == null ) {
				throw log.simpleQueryStringCannotBeNull( collectAbsoluteFieldPaths() );
			}
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

		private List<String> collectAbsoluteFieldPaths() {
			return fieldSetStates.stream().flatMap( f -> f.absoluteFieldPaths.stream() )
					.collect( Collectors.toList() );
		}

		@Override
		public CommonState flags(Set<SimpleQueryFlag> flags) {
			builder.flags( flags );
			return this;
		}

	}

}
