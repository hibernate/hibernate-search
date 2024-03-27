/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class RangePredicateFieldStepImpl implements RangePredicateFieldStep<RangePredicateFieldMoreStep<?, ?>> {

	private final RangePredicateFieldMoreStepImpl.CommonState commonState;

	public RangePredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new RangePredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public RangePredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new RangePredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
