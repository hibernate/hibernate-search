/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.NestedPredicateClausesCollector;
import org.hibernate.search.engine.search.predicate.dsl.NestedPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;

public final class NestedPredicateClausesStepImpl<SR>
		extends
		AbstractSimpleBooleanPredicateClausesStep<SR,
				NestedPredicateClausesStepImpl<SR>,
				NestedPredicateClausesCollector<SR, ?>>
		implements NestedPredicateClausesStep<SR, NestedPredicateClausesStepImpl<SR>> {

	private final NestedPredicateBuilder builder;

	public NestedPredicateClausesStepImpl(SearchPredicateDslContext<?> dslContext, String objectFieldPath,
			TypedSearchPredicateFactory<SR> factory) {
		super( SimpleBooleanPredicateOperator.AND, dslContext, factory );
		this.builder = dslContext.scope().fieldQueryElement( objectFieldPath, PredicateTypeKeys.NESTED );
	}

	@Override
	protected NestedPredicateClausesStepImpl<SR> self() {
		return this;
	}

	@Override
	protected SearchPredicate build() {
		builder.nested( super.build() );
		return builder.build();
	}

}
