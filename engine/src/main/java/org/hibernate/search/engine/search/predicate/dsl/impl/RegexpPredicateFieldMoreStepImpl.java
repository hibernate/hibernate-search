/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.RegexpPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.RegexpPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.RegexpQueryFlag;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.RegexpPredicateBuilder;
import org.hibernate.search.util.common.impl.Contracts;

class RegexpPredicateFieldMoreStepImpl
		implements RegexpPredicateFieldMoreStep<RegexpPredicateFieldMoreStepImpl, RegexpPredicateOptionsStep<?>>,
		AbstractBooleanMultiFieldPredicateCommonState.FieldSetState {

	private final CommonState commonState;

	private final List<RegexpPredicateBuilder> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	RegexpPredicateFieldMoreStepImpl(CommonState commonState, List<String> fieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		SearchIndexScope<?> scope = commonState.scope();
		for ( String fieldPath : fieldPaths ) {
			predicateBuilders.add( scope.fieldQueryElement( fieldPath, PredicateTypeKeys.REGEXP ) );
		}
	}

	@Override
	public RegexpPredicateFieldMoreStepImpl fields(String... fieldPaths) {
		return new RegexpPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}

	@Override
	public RegexpPredicateFieldMoreStepImpl boost(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public RegexpPredicateOptionsStep<?> matching(String regexpPattern) {
		return commonState.matching( regexpPattern );
	}

	@Override
	public void contributePredicates(Consumer<SearchPredicate> collector) {
		for ( RegexpPredicateBuilder predicateBuilder : predicateBuilders ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set state
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.build() );
		}
	}

	static class CommonState
			extends AbstractBooleanMultiFieldPredicateCommonState<CommonState, RegexpPredicateFieldMoreStepImpl>
			implements RegexpPredicateOptionsStep<CommonState> {

		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
		}

		private RegexpPredicateOptionsStep<?> matching(String regexpPattern) {
			Contracts.assertNotNull( regexpPattern, "regexpPattern" );
			for ( RegexpPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( RegexpPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.pattern( regexpPattern );
				}
			}
			return this;
		}

		@Override
		protected CommonState thisAsS() {
			return this;
		}

		@Override
		public CommonState flags(Set<RegexpQueryFlag> flags) {
			for ( RegexpPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( RegexpPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.flags( flags );
				}
			}
			return this;
		}
	}

}
