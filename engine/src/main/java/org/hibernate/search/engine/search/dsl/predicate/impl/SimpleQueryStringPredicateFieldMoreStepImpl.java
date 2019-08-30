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
import java.util.stream.Collectors;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.dsl.predicate.SimpleQueryStringPredicateFieldMoreStep;
import org.hibernate.search.engine.search.dsl.predicate.SimpleQueryStringPredicateOptionsStep;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class SimpleQueryStringPredicateFieldMoreStepImpl<B>
		implements SimpleQueryStringPredicateFieldMoreStep {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState<B> commonState;

	private final List<String> absoluteFieldPaths;
	private final List<SimpleQueryStringPredicateBuilder.FieldState> fieldStates = new ArrayList<>();

	SimpleQueryStringPredicateFieldMoreStepImpl(CommonState<B> commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = absoluteFieldPaths;
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			fieldStates.add( commonState.field( absoluteFieldPath ) );
		}
	}

	@Override
	public SimpleQueryStringPredicateFieldMoreStep fields(String... absoluteFieldPaths) {
		return new SimpleQueryStringPredicateFieldMoreStepImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public SimpleQueryStringPredicateFieldMoreStep boost(float boost) {
		fieldStates.forEach( c -> c.boost( boost ) );
		return this;
	}

	@Override
	public SimpleQueryStringPredicateOptionsStep matching(String simpleQueryString) {
		return commonState.matching( simpleQueryString );
	}

	static class CommonState<B> extends AbstractPredicateFinalStep<B>
			implements SimpleQueryStringPredicateOptionsStep {

		private final SimpleQueryStringPredicateBuilder<B> builder;

		private final List<SimpleQueryStringPredicateFieldMoreStepImpl<B>> fieldSetStates = new ArrayList<>();

		CommonState(SearchPredicateBuilderFactory<?, B> builderFactory) {
			super( builderFactory );
			this.builder = builderFactory.simpleQueryString();
		}

		@Override
		protected B toImplementation() {
			return builder.toImplementation();
		}

		void add(SimpleQueryStringPredicateFieldMoreStepImpl<B> fieldSetState) {
			fieldSetStates.add( fieldSetState );
		}

		SimpleQueryStringPredicateBuilder.FieldState field(String absoluteFieldPath) {
			return builder.field( absoluteFieldPath );
		}

		private SimpleQueryStringPredicateOptionsStep matching(String simpleQueryString) {
			if ( simpleQueryString == null ) {
				throw log.simpleQueryStringCannotBeNull( collectAbsoluteFieldPaths() );
			}
			builder.simpleQueryString( simpleQueryString );
			return this;
		}

		@Override
		public CommonState<B> constantScore() {
			builder.constantScore();
			return this;
		}

		@Override
		public CommonState<B> boost(float boost) {
			builder.boost( boost );
			return this;
		}

		@Override
		public SimpleQueryStringPredicateOptionsStep defaultOperator(BooleanOperator operator) {
			builder.defaultOperator( operator );
			return this;
		}

		@Override
		public SimpleQueryStringPredicateOptionsStep analyzer(String analyzerName) {
			builder.analyzer( analyzerName );
			return this;
		}

		@Override
		public SimpleQueryStringPredicateOptionsStep skipAnalysis() {
			builder.skipAnalysis();
			return this;
		}

		private List<String> collectAbsoluteFieldPaths() {
			return fieldSetStates.stream().flatMap( f -> f.absoluteFieldPaths.stream() )
					.collect( Collectors.toList() );
		}
	}

}
