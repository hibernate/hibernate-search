/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.TermsPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.TermsPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class TermsPredicateFieldStepImpl implements TermsPredicateFieldStep<TermsPredicateFieldMoreStep<?, ?>> {

	private final TermsPredicateFieldMoreStepImpl.CommonState commonState;

	public TermsPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new TermsPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public TermsPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new TermsPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
