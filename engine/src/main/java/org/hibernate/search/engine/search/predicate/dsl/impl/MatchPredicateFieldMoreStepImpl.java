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
import java.util.function.Consumer;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldMoreStep;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class MatchPredicateFieldMoreStepImpl<B>
		implements MatchPredicateFieldMoreStep<MatchPredicateFieldMoreStepImpl<B>, MatchPredicateOptionsStep<?>>,
				AbstractBooleanMultiFieldPredicateCommonState.FieldSetState<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState<B> commonState;

	private final List<String> absoluteFieldPaths;
	private final List<MatchPredicateBuilder<B>> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	MatchPredicateFieldMoreStepImpl(CommonState<B> commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = absoluteFieldPaths;
		SearchPredicateBuilderFactory<?, B> predicateFactory = commonState.getFactory();
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			predicateBuilders.add( predicateFactory.match( absoluteFieldPath ) );
		}
	}

	@Override
	public MatchPredicateFieldMoreStepImpl<B> fields(String... absoluteFieldPaths) {
		return new MatchPredicateFieldMoreStepImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public MatchPredicateFieldMoreStepImpl<B> boost(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public MatchPredicateOptionsStep<?> matching(Object value, ValueConvert convert) {
		return commonState.matching( value, convert );
	}

	@Override
	public List<String> getAbsoluteFieldPaths() {
		return absoluteFieldPaths;
	}

	@Override
	public void contributePredicateBuilders(Consumer<B> collector) {
		for ( MatchPredicateBuilder<B> predicateBuilder : predicateBuilders ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set state
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.toImplementation() );
		}
	}

	static class CommonState<B> extends AbstractBooleanMultiFieldPredicateCommonState<CommonState<B>, B, MatchPredicateFieldMoreStepImpl<B>>
			implements MatchPredicateOptionsStep<CommonState<B>> {

		CommonState(SearchPredicateBuilderFactory<?, B> builderFactory) {
			super( builderFactory );
		}

		MatchPredicateOptionsStep<?> matching(Object value, ValueConvert convert) {
			if ( value == null ) {
				throw log.matchPredicateCannotMatchNullValue( getEventContext() );
			}

			for ( MatchPredicateFieldMoreStepImpl<B> fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder<B> predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.value( value, convert );
				}
			}
			return this;
		}

		@Override
		public CommonState<B> fuzzy(int maxEditDistance, int exactPrefixLength) {
			if ( maxEditDistance < 0 || 2 < maxEditDistance ) {
				throw log.invalidFuzzyMaximumEditDistance( maxEditDistance );
			}
			if ( exactPrefixLength < 0 ) {
				throw log.invalidExactPrefixLength( exactPrefixLength );
			}

			for ( MatchPredicateFieldMoreStepImpl<B> fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder<B> predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.fuzzy( maxEditDistance, exactPrefixLength );
				}
			}
			return this;
		}

		@Override
		public CommonState<B> analyzer(String analyzerName) {
			for ( MatchPredicateFieldMoreStepImpl<B> fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder<B> predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.analyzer( analyzerName );
				}
			}
			return this;
		}

		@Override
		public CommonState<B> skipAnalysis() {
			for ( MatchPredicateFieldMoreStepImpl<B> fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder<B> predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.skipAnalysis();
				}
			}
			return this;
		}

		@Override
		protected CommonState<B> thisAsS() {
			return this;
		}
	}

}
