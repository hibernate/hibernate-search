/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.TermsPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.TermsPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.TermsPredicateBuilder;
import org.hibernate.search.util.common.impl.Contracts;

class TermsPredicateFieldMoreStepImpl
		implements TermsPredicateFieldMoreStep<TermsPredicateFieldMoreStepImpl, TermsPredicateOptionsStep<?>>,
		AbstractBooleanMultiFieldPredicateCommonState.FieldSetState {

	private final CommonState commonState;

	private final List<TermsPredicateBuilder> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	TermsPredicateFieldMoreStepImpl(CommonState commonState, List<String> fieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		SearchIndexScope<?> scope = commonState.scope();
		for ( String fieldPath : fieldPaths ) {
			predicateBuilders.add( scope.fieldQueryElement( fieldPath, PredicateTypeKeys.TERMS ) );
		}
	}

	@Override
	public TermsPredicateFieldMoreStepImpl fields(String... fieldPaths) {
		return new TermsPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}

	@Override
	public TermsPredicateFieldMoreStepImpl boost(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public TermsPredicateOptionsStep<?> matchingAny(Collection<?> terms, ValueConvert convert) {
		return commonState.matchingAny( terms, convert );
	}

	@Override
	public TermsPredicateOptionsStep<?> matchingAll(Collection<?> terms, ValueConvert convert) {
		return commonState.matchingAll( terms, convert );
	}

	@Override
	public void contributePredicates(Consumer<SearchPredicate> collector) {
		for ( TermsPredicateBuilder predicateBuilder : predicateBuilders ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set state
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.build() );
		}
	}

	static class CommonState
			extends AbstractBooleanMultiFieldPredicateCommonState<CommonState, TermsPredicateFieldMoreStepImpl>
			implements TermsPredicateOptionsStep<CommonState> {

		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
		}

		private TermsPredicateOptionsStep<?> matchingAny(Collection<?> terms, ValueConvert convert) {
			Contracts.assertNotNullNorEmpty( terms, "terms" );
			Contracts.assertNotNull( convert, "convert" );

			for ( TermsPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( TermsPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.matchingAny( terms, convert );
				}
			}
			return this;
		}

		private TermsPredicateOptionsStep<?> matchingAll(Collection<?> terms, ValueConvert convert) {
			Contracts.assertNotNullNorEmpty( terms, "terms" );
			Contracts.assertNotNull( convert, "convert" );

			for ( TermsPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( TermsPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.matchingAll( terms, convert );
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
