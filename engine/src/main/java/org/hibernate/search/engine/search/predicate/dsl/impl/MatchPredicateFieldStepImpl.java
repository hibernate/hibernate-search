/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldMoreGenericStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.reference.predicate.MatchPredicateFieldReference;

public final class MatchPredicateFieldStepImpl<SR> implements MatchPredicateFieldStep<SR, MatchPredicateFieldMoreStep<?, ?>> {

	private final SearchPredicateDslContext<?> dslContext;

	public MatchPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public MatchPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return AbstractMatchPredicateFieldMoreStep.create( dslContext, fieldPaths );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> MatchPredicateFieldMoreGenericStep<?, ?, T, MatchPredicateFieldReference<? super SR, T>> fields(
			MatchPredicateFieldReference<? super SR, T>... fields) {
		return AbstractMatchPredicateFieldMoreStep.create( dslContext, fields );
	}
}
