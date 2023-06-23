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
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PhrasePredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.PhrasePredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PhrasePredicateFieldMoreStepImpl
		implements PhrasePredicateFieldMoreStep<PhrasePredicateFieldMoreStepImpl, PhrasePredicateOptionsStep<?>>,
		AbstractBooleanMultiFieldPredicateCommonState.FieldSetState {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState commonState;

	private final List<PhrasePredicateBuilder> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	PhrasePredicateFieldMoreStepImpl(CommonState commonState, List<String> fieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		SearchIndexScope<?> scope = commonState.scope();
		for ( String fieldPath : fieldPaths ) {
			predicateBuilders.add( scope.fieldQueryElement( fieldPath, PredicateTypeKeys.PHRASE ) );
		}
	}

	@Override
	public PhrasePredicateFieldMoreStepImpl fields(String... fieldPaths) {
		return new PhrasePredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}

	@Override
	public PhrasePredicateFieldMoreStepImpl boost(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public PhrasePredicateOptionsStep<?> matching(String phrase) {
		return commonState.matching( phrase );
	}

	@Override
	public void contributePredicates(Consumer<SearchPredicate> collector) {
		for ( PhrasePredicateBuilder predicateBuilder : predicateBuilders ) {
			// Fieldset states won't be accessed anymore, it's time to apply their options
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.build() );
		}
	}

	static class CommonState
			extends AbstractBooleanMultiFieldPredicateCommonState<CommonState, PhrasePredicateFieldMoreStepImpl>
			implements PhrasePredicateOptionsStep<CommonState> {

		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
		}

		private PhrasePredicateOptionsStep<?> matching(String phrase) {
			Contracts.assertNotNull( phrase, "phrase" );
			for ( PhrasePredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( PhrasePredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.phrase( phrase );
				}
			}
			return this;
		}

		@Override
		public CommonState slop(int slop) {
			if ( slop < 0 ) {
				throw log.invalidPhrasePredicateSlop( slop );
			}

			for ( PhrasePredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( PhrasePredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.slop( slop );
				}
			}
			return this;
		}

		@Override
		public CommonState analyzer(String analyzerName) {
			for ( PhrasePredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( PhrasePredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.analyzer( analyzerName );
				}
			}
			return this;
		}

		@Override
		public CommonState skipAnalysis() {
			for ( PhrasePredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( PhrasePredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
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
