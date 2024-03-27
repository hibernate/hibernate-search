/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class MatchPredicateFieldStepImpl implements MatchPredicateFieldStep<MatchPredicateFieldMoreStep<?, ?>> {

	private final MatchPredicateFieldMoreStepImpl.CommonState commonState;

	public MatchPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new MatchPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public MatchPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new MatchPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
