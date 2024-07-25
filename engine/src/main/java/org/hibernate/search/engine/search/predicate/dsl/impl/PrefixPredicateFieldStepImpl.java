/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.PrefixPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.PrefixPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class PrefixPredicateFieldStepImpl
		implements PrefixPredicateFieldStep<PrefixPredicateFieldMoreStep<?, ?>> {

	private final PrefixPredicateFieldMoreStepImpl.CommonState commonState;

	public PrefixPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new PrefixPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public PrefixPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new PrefixPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
