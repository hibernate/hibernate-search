/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.logging.impl.QueryLog;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.MinimumShouldMatchConditionStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MinimumShouldMatchBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.util.common.impl.Contracts;

class MatchPredicateFieldMoreStepImpl
		implements MatchPredicateFieldMoreStep<MatchPredicateFieldMoreStepImpl, MatchPredicateOptionsStep<?>>,
		AbstractBooleanMultiFieldPredicateCommonState.FieldSetState {

	private final CommonState commonState;

	private final List<MatchPredicateBuilder> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	MatchPredicateFieldMoreStepImpl(CommonState commonState, List<String> fieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		SearchIndexScope<?> scope = commonState.scope();
		for ( String fieldPath : fieldPaths ) {
			predicateBuilders.add( scope.fieldQueryElement( fieldPath, PredicateTypeKeys.MATCH ) );
		}
	}

	@Override
	public MatchPredicateFieldMoreStepImpl fields(String... fieldPaths) {
		return new MatchPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}

	@Override
	public MatchPredicateFieldMoreStepImpl boost(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public MatchPredicateOptionsStep<?> matching(Object value, ValueModel valueModel) {
		return commonState.matching( value, valueModel );
	}

	@Override
	public void contributePredicates(Consumer<SearchPredicate> collector) {
		for ( MatchPredicateBuilder predicateBuilder : predicateBuilders ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set state
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.build() );
		}
	}

	static class CommonState extends AbstractBooleanMultiFieldPredicateCommonState<CommonState, MatchPredicateFieldMoreStepImpl>
			implements MatchPredicateOptionsStep<CommonState> {
		private final MinimumShouldMatchConditionStepImpl<CommonState> minimumShouldMatchStep;

		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
			minimumShouldMatchStep = new MinimumShouldMatchConditionStepImpl<>( new MatchMinimumShouldMatchBuilder(), this );
		}

		MatchPredicateOptionsStep<?> matching(Object value, ValueModel valueModel) {
			Contracts.assertNotNull( value, "value" );
			Contracts.assertNotNull( valueModel, "valueModel" );

			for ( MatchPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.value( value, valueModel );
				}
			}
			return this;
		}

		@Override
		public CommonState fuzzy(int maxEditDistance, int exactPrefixLength) {
			if ( maxEditDistance < 0 || 2 < maxEditDistance ) {
				throw QueryLog.INSTANCE.invalidFuzzyMaximumEditDistance( maxEditDistance );
			}
			if ( exactPrefixLength < 0 ) {
				throw QueryLog.INSTANCE.invalidExactPrefixLength( exactPrefixLength );
			}

			for ( MatchPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.fuzzy( maxEditDistance, exactPrefixLength );
				}
			}
			return this;
		}

		@Override
		public CommonState analyzer(String analyzerName) {
			for ( MatchPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.analyzer( analyzerName );
				}
			}
			return this;
		}

		@Override
		public CommonState skipAnalysis() {
			for ( MatchPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.skipAnalysis();
				}
			}
			return this;
		}

		@Override
		protected CommonState thisAsS() {
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

		private class MatchMinimumShouldMatchBuilder implements MinimumShouldMatchBuilder {
			@Override
			public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
				for ( MatchPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
					for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
						predicateBuilder.minimumShouldMatchNumber( ignoreConstraintCeiling, matchingClausesNumber );
					}
				}
			}

			@Override
			public void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent) {
				for ( MatchPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
					for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
						predicateBuilder.minimumShouldMatchPercent( ignoreConstraintCeiling, matchingClausesPercent );
					}
				}
			}
		}
	}

}
