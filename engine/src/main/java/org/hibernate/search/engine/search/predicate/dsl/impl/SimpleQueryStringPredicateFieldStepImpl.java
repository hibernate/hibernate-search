/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class SimpleQueryStringPredicateFieldStepImpl<SR>
		implements SimpleQueryStringPredicateFieldStep<SR, SimpleQueryStringPredicateFieldMoreStep<SR, ?, ?>> {

	private final SimpleQueryStringPredicateFieldMoreStepImpl.CommonState commonState;

	public SimpleQueryStringPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new SimpleQueryStringPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public SimpleQueryStringPredicateFieldMoreStep<SR, ?, ?> fields(String... fieldPaths) {
		return new SimpleQueryStringPredicateFieldMoreStepImpl<SR>( commonState, Arrays.asList( fieldPaths ) );
	}
}
