/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.PrefixPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.PrefixPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class PrefixPredicateFieldStepImpl<SR>
		implements PrefixPredicateFieldStep<SR, PrefixPredicateFieldMoreStep<SR, ?, ?>> {

	private final PrefixPredicateFieldMoreStepImpl.CommonState<SR> commonState;

	public PrefixPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new PrefixPredicateFieldMoreStepImpl.CommonState<>( dslContext );
	}

	@Override
	public PrefixPredicateFieldMoreStep<SR, ?, ?> fields(String... fieldPaths) {
		return new PrefixPredicateFieldMoreStepImpl<>( commonState, Arrays.asList( fieldPaths ) );
	}
}
