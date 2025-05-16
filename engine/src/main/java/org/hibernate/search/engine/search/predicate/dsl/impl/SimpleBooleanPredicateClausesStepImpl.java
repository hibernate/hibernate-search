/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesCollector;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class SimpleBooleanPredicateClausesStepImpl<SR>
		extends
		AbstractSimpleBooleanPredicateClausesStep<SR,
				SimpleBooleanPredicateClausesStepImpl<SR>,
				SimpleBooleanPredicateClausesCollector<SR, ?>>
		implements SimpleBooleanPredicateClausesStep<SR, SimpleBooleanPredicateClausesStepImpl<SR>> {

	public SimpleBooleanPredicateClausesStepImpl(SimpleBooleanPredicateOperator operator,
			SearchPredicateDslContext<?> dslContext,
			TypedSearchPredicateFactory<SR> factory) {
		super( operator, dslContext, factory );
	}

	public SimpleBooleanPredicateClausesStepImpl(SimpleBooleanPredicateOperator operator,
			SearchPredicateDslContext<?> dslContext,
			TypedSearchPredicateFactory<SR> factory,
			SearchPredicate firstSearchPredicate,
			SearchPredicate... otherSearchPredicates) {
		this( operator, dslContext, factory );
		add( firstSearchPredicate );
		for ( SearchPredicate step : otherSearchPredicates ) {
			add( step );
		}
	}

	public SimpleBooleanPredicateClausesStepImpl(SimpleBooleanPredicateOperator operator,
			SearchPredicateDslContext<?> dslContext,
			TypedSearchPredicateFactory<SR> factory,
			PredicateFinalStep firstSearchPredicate,
			PredicateFinalStep... otherSearchPredicates) {
		this( operator, dslContext, factory );
		add( firstSearchPredicate.toPredicate() );
		for ( PredicateFinalStep step : otherSearchPredicates ) {
			add( step.toPredicate() );
		}
	}

	@Override
	protected SimpleBooleanPredicateClausesStepImpl<SR> self() {
		return this;
	}
}
