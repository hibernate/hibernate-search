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
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class MatchPredicateFieldMoreStepImpl
		implements MatchPredicateFieldMoreStep<MatchPredicateFieldMoreStepImpl, MatchPredicateOptionsStep<?>>,
		AbstractBooleanMultiFieldPredicateCommonState.FieldSetState {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
	public MatchPredicateOptionsStep<?> matching(Object value, ValueConvert convert) {
		return commonState.matching( value, convert );
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

		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
		}

		MatchPredicateOptionsStep<?> matching(Object value, ValueConvert convert) {
			Contracts.assertNotNull( value, "value" );
			Contracts.assertNotNull( convert, "convert" );

			for ( MatchPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( MatchPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.value( value, convert );
				}
			}
			return this;
		}

		@Override
		public CommonState fuzzy(int maxEditDistance, int exactPrefixLength) {
			if ( maxEditDistance < 0 || 2 < maxEditDistance ) {
				throw log.invalidFuzzyMaximumEditDistance( maxEditDistance );
			}
			if ( exactPrefixLength < 0 ) {
				throw log.invalidExactPrefixLength( exactPrefixLength );
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
	}

}
