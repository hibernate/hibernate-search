/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.search.engine.logging.impl.QueryLog;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldMoreGenericStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.MinimumShouldMatchConditionStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MinimumShouldMatchBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.reference.predicate.MatchPredicateFieldReference;
import org.hibernate.search.util.common.impl.Contracts;

abstract class AbstractMatchPredicateFieldMoreStep<
		CS extends AbstractMatchPredicateFieldMoreStep.GenericCommonState<T, V, S>,
		S extends AbstractMatchPredicateFieldMoreStep<CS, S, T, V>,
		T,
		V>
		implements AbstractBooleanMultiFieldPredicateCommonState.FieldSetState {

	protected final CS commonState;

	protected final Map<V, MatchPredicateBuilder> predicateBuilders = new LinkedHashMap<>();

	private Float fieldSetBoost;

	public static MatchPredicateFieldMoreStep<?, ?> create(
			SearchPredicateDslContext<?> dslContext, String[] fields) {
		return new MatchPredicateFieldMoreStepString(
				dslContext,
				Arrays.asList( fields )
		);
	}

	public static <SR, T> MatchPredicateFieldMoreGenericStep<?, ?, T, MatchPredicateFieldReference<SR, T>> create(
			SearchPredicateDslContext<?> dslContext, MatchPredicateFieldReference<SR, T>[] fields) {
		return new MatchPredicateFieldMoreStepFieldReference<>(
				dslContext,
				Arrays.asList( fields )
		);
	}

	protected AbstractMatchPredicateFieldMoreStep(CS commonState, List<V> fieldPaths) {
		this.commonState = commonState;
		this.commonState.add( thisAsS() );
		SearchIndexScope<?> scope = commonState.scope();
		for ( V fieldPath : fieldPaths ) {
			predicateBuilders.put( fieldPath, scope.fieldQueryElement( fieldPath( fieldPath ), PredicateTypeKeys.MATCH ) );
		}
	}

	protected abstract S thisAsS();

	protected abstract String fieldPath(V field);

	public S boost(float boost) {
		this.fieldSetBoost = boost;
		return thisAsS();
	}

	@Override
	public void contributePredicates(Consumer<SearchPredicate> collector) {
		for ( MatchPredicateBuilder predicateBuilder : predicateBuilders.values() ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set state
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.build() );
		}
	}

	private static class MatchPredicateFieldMoreStepString
			extends
			AbstractMatchPredicateFieldMoreStep<MatchPredicateFieldMoreStepString.CommonState,
					MatchPredicateFieldMoreStepString,
					Object,
					String>
			implements
			MatchPredicateFieldMoreStep<MatchPredicateFieldMoreStepString, MatchPredicateFieldMoreStepString.CommonState> {

		MatchPredicateFieldMoreStepString(SearchPredicateDslContext<?> dslContext, List<String> fieldPaths) {
			super( new CommonState( dslContext ), fieldPaths );
		}

		private MatchPredicateFieldMoreStepString(CommonState commonState, List<String> fieldPaths) {
			super( commonState, fieldPaths );
		}

		@Override
		protected MatchPredicateFieldMoreStepString thisAsS() {
			return this;
		}

		@Override
		protected String fieldPath(String field) {
			return field;
		}

		@Override
		public MatchPredicateFieldMoreStepString field(String field) {
			return new MatchPredicateFieldMoreStepString( commonState, Arrays.asList( field ) );
		}

		@Override
		public MatchPredicateFieldMoreStepString fields(String... fieldPaths) {
			return new MatchPredicateFieldMoreStepString( commonState, Arrays.asList( fieldPaths ) );
		}

		@Override
		public CommonState matching(Object value, ValueModel valueModel) {
			return commonState.matching( value, valueModel );
		}


		private static class CommonState extends GenericCommonState<Object, String, MatchPredicateFieldMoreStepString> {

			CommonState(SearchPredicateDslContext<?> dslContext) {
				super( dslContext );
			}

			CommonState matching(Object value, ValueModel valueModel) {
				Contracts.assertNotNull( value, "value" );
				Contracts.assertNotNull( valueModel, "valueModel" );

				for ( MatchPredicateFieldMoreStepString fieldSetState : getFieldSetStates() ) {
					for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders.values() ) {
						predicateBuilder.value( value, valueModel );
					}
				}
				return this;
			}

		}
	}

	private static class MatchPredicateFieldMoreStepFieldReference<SR, T>
			extends
			AbstractMatchPredicateFieldMoreStep<MatchPredicateFieldMoreStepFieldReference.CommonState<SR, T>,
					MatchPredicateFieldMoreStepFieldReference<SR, T>,
					T,
					MatchPredicateFieldReference<SR, T>>
			implements
			MatchPredicateFieldMoreGenericStep<MatchPredicateFieldMoreStepFieldReference<SR, T>,
					MatchPredicateFieldMoreStepFieldReference.CommonState<SR, T>,
					T,
					MatchPredicateFieldReference<SR, T>> {

		MatchPredicateFieldMoreStepFieldReference(SearchPredicateDslContext<?> dslContext,
				List<MatchPredicateFieldReference<SR, T>> fieldPaths) {
			super( new CommonState<>( dslContext ), fieldPaths );
		}

		private MatchPredicateFieldMoreStepFieldReference(CommonState<SR, T> commonState,
				List<MatchPredicateFieldReference<SR, T>> fieldPaths) {
			super( commonState, fieldPaths );
		}

		@Override
		public MatchPredicateFieldMoreStepFieldReference<SR, T> field(MatchPredicateFieldReference<SR, T> field) {
			return new MatchPredicateFieldMoreStepFieldReference<>( commonState, Collections.singletonList( field ) );
		}

		@Override
		@SuppressWarnings("unchecked")
		public MatchPredicateFieldMoreStepFieldReference<SR, T> fields(MatchPredicateFieldReference<SR, T>... fieldPaths) {
			return new MatchPredicateFieldMoreStepFieldReference<>( commonState, Arrays.asList( fieldPaths ) );
		}

		@Override
		public CommonState<SR, T> matching(T value) {
			return commonState.matching( value );
		}

		@Override
		protected MatchPredicateFieldMoreStepFieldReference<SR, T> thisAsS() {
			return this;
		}

		@Override
		protected String fieldPath(MatchPredicateFieldReference<SR, T> field) {
			return field.absolutePath();
		}

		private static class CommonState<SR, T>
				extends
				GenericCommonState<T, MatchPredicateFieldReference<SR, T>, MatchPredicateFieldMoreStepFieldReference<SR, T>> {
			CommonState(SearchPredicateDslContext<?> dslContext) {
				super( dslContext );
			}

			CommonState<SR, T> matching(T value) {
				Contracts.assertNotNull( value, "value" );

				for ( MatchPredicateFieldMoreStepFieldReference<SR, T> fieldSetState : getFieldSetStates() ) {
					for ( Map.Entry<MatchPredicateFieldReference<SR, T>,
							MatchPredicateBuilder> entry : fieldSetState.predicateBuilders
									.entrySet() ) {
						entry.getValue().value( value, entry.getKey().valueModel() );
					}
				}
				return this;
			}
		}
	}

	static class GenericCommonState<T, V, S extends AbstractMatchPredicateFieldMoreStep<?, S, T, V>>
			extends AbstractBooleanMultiFieldPredicateCommonState<GenericCommonState<T, V, S>, S>
			implements MatchPredicateOptionsStep<GenericCommonState<T, V, S>> {

		private final MinimumShouldMatchConditionStepImpl<? extends GenericCommonState<T, V, S>> minimumShouldMatchStep;

		GenericCommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
			minimumShouldMatchStep = new MinimumShouldMatchConditionStepImpl<>( new MatchMinimumShouldMatchBuilder(), this );
		}

		@Override
		public GenericCommonState<T, V, S> fuzzy(int maxEditDistance, int exactPrefixLength) {
			if ( maxEditDistance < 0 || 2 < maxEditDistance ) {
				throw QueryLog.INSTANCE.invalidFuzzyMaximumEditDistance( maxEditDistance );
			}
			if ( exactPrefixLength < 0 ) {
				throw QueryLog.INSTANCE.invalidExactPrefixLength( exactPrefixLength );
			}

			for ( AbstractMatchPredicateFieldMoreStep<?, S, T, V> fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders.values() ) {
					predicateBuilder.fuzzy( maxEditDistance, exactPrefixLength );
				}
			}
			return this;
		}

		@Override
		public GenericCommonState<T, V, S> analyzer(String analyzerName) {
			for ( AbstractMatchPredicateFieldMoreStep<?, S, T, V> fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders.values() ) {
					predicateBuilder.analyzer( analyzerName );
				}
			}
			return this;
		}

		@Override
		public GenericCommonState<T, V, S> skipAnalysis() {
			for ( AbstractMatchPredicateFieldMoreStep<?, S, T, V> fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders.values() ) {
					predicateBuilder.skipAnalysis();
				}
			}
			return this;
		}

		@Override
		protected GenericCommonState<T, V, S> thisAsS() {
			return this;
		}

		@Override
		public MinimumShouldMatchConditionStep<? extends GenericCommonState<T, V, S>> minimumShouldMatch() {
			return minimumShouldMatchStep;
		}

		@Override
		public GenericCommonState<T, V, S> minimumShouldMatch(
				Consumer<? super MinimumShouldMatchConditionStep<?>> constraintContributor) {
			constraintContributor.accept( minimumShouldMatchStep );
			return this;
		}

		private class MatchMinimumShouldMatchBuilder implements MinimumShouldMatchBuilder {
			@Override
			public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
				for ( AbstractMatchPredicateFieldMoreStep<?, S, T, V> fieldSetState : getFieldSetStates() ) {
					for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders.values() ) {
						predicateBuilder.minimumShouldMatchNumber( ignoreConstraintCeiling, matchingClausesNumber );
					}
				}
			}

			@Override
			public void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent) {
				for ( AbstractMatchPredicateFieldMoreStep<?, S, T, V> fieldSetState : getFieldSetStates() ) {
					for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders.values() ) {
						predicateBuilder.minimumShouldMatchPercent( ignoreConstraintCeiling, matchingClausesPercent );
					}
				}
			}
		}
	}

}
