/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.PhrasePredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.PhrasePredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class PhrasePredicateFieldStepImpl<SR>
		implements PhrasePredicateFieldStep<SR, PhrasePredicateFieldMoreStep<SR, ?, ?>> {

	private final PhrasePredicateFieldMoreStepImpl.CommonState<SR> commonState;

	public PhrasePredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new PhrasePredicateFieldMoreStepImpl.CommonState<SR>( dslContext );
	}

	@Override
	public PhrasePredicateFieldMoreStep<SR, ?, ?> fields(String... fieldPaths) {
		return new PhrasePredicateFieldMoreStepImpl<SR>( commonState, Arrays.asList( fieldPaths ) );
	}
}
