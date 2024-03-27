/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateOptionsCollector;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class BooleanPredicateClausesStepImpl
		extends AbstractBooleanPredicateClausesStep<BooleanPredicateClausesStepImpl, BooleanPredicateOptionsCollector<?>>
		implements BooleanPredicateClausesStep<BooleanPredicateClausesStepImpl> {

	public BooleanPredicateClausesStepImpl(SearchPredicateDslContext<?> dslContext,
			SearchPredicateFactory factory) {
		super( dslContext, factory );
	}

	@Override
	protected BooleanPredicateClausesStepImpl self() {
		return this;
	}

}
