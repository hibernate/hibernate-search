/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.WildcardPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.WildcardPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.util.common.impl.Contracts;

class WildcardPredicateFieldMoreStepImpl<SR>
		implements WildcardPredicateFieldMoreStep<SR, WildcardPredicateFieldMoreStepImpl<SR>, WildcardPredicateOptionsStep<?>>,
		AbstractBooleanMultiFieldPredicateCommonState.FieldSetState {

	private final CommonState<SR> commonState;

	private final List<WildcardPredicateBuilder> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	WildcardPredicateFieldMoreStepImpl(CommonState<SR> commonState, List<String> fieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		SearchIndexScope<?> scope = commonState.scope();
		for ( String fieldPath : fieldPaths ) {
			predicateBuilders.add( scope.fieldQueryElement( fieldPath, PredicateTypeKeys.WILDCARD ) );
		}
	}

	@Override
	public WildcardPredicateFieldMoreStepImpl<SR> fields(String... fieldPaths) {
		return new WildcardPredicateFieldMoreStepImpl<>( commonState, Arrays.asList( fieldPaths ) );
	}

	@Override
	public WildcardPredicateFieldMoreStepImpl<SR> boost(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public WildcardPredicateOptionsStep<?> matching(String wildcardPattern) {
		return commonState.matching( wildcardPattern );
	}

	@Override
	public void contributePredicates(Consumer<SearchPredicate> collector) {
		for ( WildcardPredicateBuilder predicateBuilder : predicateBuilders ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set state
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.build() );
		}
	}

	static class CommonState<SR>
			extends AbstractBooleanMultiFieldPredicateCommonState<CommonState<SR>, WildcardPredicateFieldMoreStepImpl<SR>>
			implements WildcardPredicateOptionsStep<CommonState<SR>> {

		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
		}

		private WildcardPredicateOptionsStep<?> matching(String wildcardPattern) {
			Contracts.assertNotNull( wildcardPattern, "wildcardPattern" );
			for ( WildcardPredicateFieldMoreStepImpl<SR> fieldSetState : getFieldSetStates() ) {
				for ( WildcardPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.pattern( wildcardPattern );
				}
			}
			return this;
		}

		@Override
		protected CommonState<SR> thisAsS() {
			return this;
		}
	}

}
