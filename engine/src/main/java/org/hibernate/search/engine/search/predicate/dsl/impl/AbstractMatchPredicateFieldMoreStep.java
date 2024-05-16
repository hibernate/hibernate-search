/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.common.ValueConvert;
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
import org.hibernate.search.engine.search.reference.traits.predicate.MatchPredicateFieldReference;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractMatchPredicateFieldMoreStep<
		E,
		CS extends AbstractMatchPredicateFieldMoreStep.GenericCommonState<E, T, V, S>,
		S extends AbstractMatchPredicateFieldMoreStep<E, CS, S, T, V>,
		T,
		V>
		implements AbstractBooleanMultiFieldPredicateCommonState.FieldSetState {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final CS commonState;

	protected final Map<V, MatchPredicateBuilder> predicateBuilders = new LinkedHashMap<>();

	private Float fieldSetBoost;

	public static <E> MatchPredicateFieldMoreStep<E, ?, ?> create(
			SearchPredicateDslContext<?> dslContext, String[] fields) {
		return new MatchPredicateFieldMoreStepString<>(
				dslContext,
				Arrays.asList( fields )
		);
	}

	public static <E, T> MatchPredicateFieldMoreGenericStep<E, ?, ?, T, MatchPredicateFieldReference<? extends E, T>> create(
			SearchPredicateDslContext<?> dslContext, MatchPredicateFieldReference<? extends E, T>[] fields) {
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

	private static class MatchPredicateFieldMoreStepString<E>
			extends
			AbstractMatchPredicateFieldMoreStep<
					E,
					MatchPredicateFieldMoreStepString.CommonState<E>,
					MatchPredicateFieldMoreStepString<E>,
					Object,
					String>
			implements
			MatchPredicateFieldMoreStep<E,
					MatchPredicateFieldMoreStepString<E>,
					MatchPredicateFieldMoreStepString.CommonState<E>> {

		MatchPredicateFieldMoreStepString(SearchPredicateDslContext<?> dslContext, List<String> fieldPaths) {
			super( new CommonState<>( dslContext ), fieldPaths );
		}

		private MatchPredicateFieldMoreStepString(CommonState<E> commonState, List<String> fieldPaths) {
			super( commonState, fieldPaths );
		}

		@Override
		protected MatchPredicateFieldMoreStepString<E> thisAsS() {
			return this;
		}

		@Override
		protected String fieldPath(String field) {
			return field;
		}

		@Override
		public MatchPredicateFieldMoreStepString<E> field(String field) {
			return new MatchPredicateFieldMoreStepString<>( commonState, Arrays.asList( field ) );
		}

		@Override
		public MatchPredicateFieldMoreStepString<E> fields(String... fieldPaths) {
			return new MatchPredicateFieldMoreStepString<>( commonState, Arrays.asList( fieldPaths ) );
		}

		@Override
		public CommonState<E> matching(Object value, ValueConvert convert) {
			return commonState.matching( value, convert );
		}


		private static class CommonState<E>
				extends GenericCommonState<E, Object, String, MatchPredicateFieldMoreStepString<E>> {

			CommonState(SearchPredicateDslContext<?> dslContext) {
				super( dslContext );
			}

			CommonState<E> matching(Object value, ValueConvert convert) {
				Contracts.assertNotNull( value, "value" );
				Contracts.assertNotNull( convert, "convert" );

				for ( MatchPredicateFieldMoreStepString<E> fieldSetState : getFieldSetStates() ) {
					for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders.values() ) {
						predicateBuilder.value( value, convert );
					}
				}
				return this;
			}

		}
	}

	private static class MatchPredicateFieldMoreStepFieldReference<E, T>
			extends
			AbstractMatchPredicateFieldMoreStep<
					E,
					MatchPredicateFieldMoreStepFieldReference.CommonState<E, T>,
					MatchPredicateFieldMoreStepFieldReference<E, T>,
					T,
					MatchPredicateFieldReference<? extends E, T>>
			implements
			MatchPredicateFieldMoreGenericStep<E,
					MatchPredicateFieldMoreStepFieldReference<E, T>,
					MatchPredicateFieldMoreStepFieldReference.CommonState<E, T>,
					T,
					MatchPredicateFieldReference<? extends E, T>> {

		MatchPredicateFieldMoreStepFieldReference(SearchPredicateDslContext<?> dslContext,
				List<MatchPredicateFieldReference<? extends E, T>> fieldPaths) {
			super( new CommonState<>( dslContext ), fieldPaths );
		}

		private MatchPredicateFieldMoreStepFieldReference(CommonState<E, T> commonState,
				List<MatchPredicateFieldReference<? extends E, T>> fieldPaths) {
			super( commonState, fieldPaths );
		}

		@Override
		public MatchPredicateFieldMoreStepFieldReference<E, T> field(MatchPredicateFieldReference<? extends E, T> field) {
			return new MatchPredicateFieldMoreStepFieldReference<>( commonState, Collections.singletonList( field ) );
		}

		@Override
		@SuppressWarnings("unchecked")
		public MatchPredicateFieldMoreStepFieldReference<E, T> fields(
				MatchPredicateFieldReference<? extends E, T>... fieldPaths) {
			return new MatchPredicateFieldMoreStepFieldReference<>( commonState, Arrays.asList( fieldPaths ) );
		}

		@Override
		public CommonState<E, T> matching(T value) {
			return commonState.matching( value );
		}

		@Override
		protected MatchPredicateFieldMoreStepFieldReference<E, T> thisAsS() {
			return this;
		}

		@Override
		protected String fieldPath(MatchPredicateFieldReference<? extends E, T> field) {
			return field.absolutePath();
		}

		private static class CommonState<E, T>
				extends
				GenericCommonState<E,
						T,
						MatchPredicateFieldReference<? extends E, T>,
						MatchPredicateFieldMoreStepFieldReference<E, T>> {
			CommonState(SearchPredicateDslContext<?> dslContext) {
				super( dslContext );
			}

			CommonState<E, T> matching(T value) {
				Contracts.assertNotNull( value, "value" );

				for ( MatchPredicateFieldMoreStepFieldReference<E, T> fieldSetState : getFieldSetStates() ) {
					for ( Map.Entry<MatchPredicateFieldReference<? extends E, T>,
							MatchPredicateBuilder> entry : fieldSetState.predicateBuilders
									.entrySet() ) {
						entry.getValue().value( value, entry.getKey().valueConvert() );
					}
				}
				return this;
			}
		}
	}

	static class GenericCommonState<E, T, V, S extends AbstractMatchPredicateFieldMoreStep<E, ?, S, T, V>>
			extends AbstractBooleanMultiFieldPredicateCommonState<GenericCommonState<E, T, V, S>, S>
			implements MatchPredicateOptionsStep<GenericCommonState<E, T, V, S>> {

		private final MinimumShouldMatchConditionStepImpl<? extends GenericCommonState<E, T, V, S>> minimumShouldMatchStep;

		GenericCommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
			minimumShouldMatchStep = new MinimumShouldMatchConditionStepImpl<>( new MatchMinimumShouldMatchBuilder(), this );
		}

		@Override
		public GenericCommonState<E, T, V, S> fuzzy(int maxEditDistance, int exactPrefixLength) {
			if ( maxEditDistance < 0 || 2 < maxEditDistance ) {
				throw log.invalidFuzzyMaximumEditDistance( maxEditDistance );
			}
			if ( exactPrefixLength < 0 ) {
				throw log.invalidExactPrefixLength( exactPrefixLength );
			}

			for ( AbstractMatchPredicateFieldMoreStep<E, ?, S, T, V> fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders.values() ) {
					predicateBuilder.fuzzy( maxEditDistance, exactPrefixLength );
				}
			}
			return this;
		}

		@Override
		public GenericCommonState<E, T, V, S> analyzer(String analyzerName) {
			for ( AbstractMatchPredicateFieldMoreStep<E, ?, S, T, V> fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders.values() ) {
					predicateBuilder.analyzer( analyzerName );
				}
			}
			return this;
		}

		@Override
		public GenericCommonState<E, T, V, S> skipAnalysis() {
			for ( AbstractMatchPredicateFieldMoreStep<E, ?, S, T, V> fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders.values() ) {
					predicateBuilder.skipAnalysis();
				}
			}
			return this;
		}

		@Override
		protected GenericCommonState<E, T, V, S> thisAsS() {
			return this;
		}

		@Override
		public MinimumShouldMatchConditionStep<? extends GenericCommonState<E, T, V, S>> minimumShouldMatch() {
			return minimumShouldMatchStep;
		}

		@Override
		public GenericCommonState<E, T, V, S> minimumShouldMatch(
				Consumer<? super MinimumShouldMatchConditionStep<?>> constraintContributor) {
			constraintContributor.accept( minimumShouldMatchStep );
			return this;
		}

		private class MatchMinimumShouldMatchBuilder implements MinimumShouldMatchBuilder {
			@Override
			public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
				for ( AbstractMatchPredicateFieldMoreStep<E, ?, S, T, V> fieldSetState : getFieldSetStates() ) {
					for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders.values() ) {
						predicateBuilder.minimumShouldMatchNumber( ignoreConstraintCeiling, matchingClausesNumber );
					}
				}
			}

			@Override
			public void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent) {
				for ( AbstractMatchPredicateFieldMoreStep<E, ?, S, T, V> fieldSetState : getFieldSetStates() ) {
					for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders.values() ) {
						predicateBuilder.minimumShouldMatchPercent( ignoreConstraintCeiling, matchingClausesPercent );
					}
				}
			}
		}
	}

}
