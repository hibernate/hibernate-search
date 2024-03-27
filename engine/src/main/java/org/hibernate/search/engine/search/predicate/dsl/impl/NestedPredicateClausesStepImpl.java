/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.NestedPredicateClausesCollector;
import org.hibernate.search.engine.search.predicate.dsl.NestedPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;

public final class NestedPredicateClausesStepImpl
		extends AbstractSimpleBooleanPredicateClausesStep<NestedPredicateClausesStepImpl, NestedPredicateClausesCollector<?>>
		implements NestedPredicateClausesStep<NestedPredicateClausesStepImpl> {

	private final NestedPredicateBuilder builder;

	public NestedPredicateClausesStepImpl(SearchPredicateDslContext<?> dslContext, String objectFieldPath,
			SearchPredicateFactory factory) {
		super( SimpleBooleanPredicateOperator.AND, dslContext, factory );
		this.builder = dslContext.scope().fieldQueryElement( objectFieldPath, PredicateTypeKeys.NESTED );
	}

	@Override
	protected NestedPredicateClausesStepImpl self() {
		return this;
	}

	@Override
	protected SearchPredicate build() {
		builder.nested( super.build() );
		return builder.build();
	}

}
