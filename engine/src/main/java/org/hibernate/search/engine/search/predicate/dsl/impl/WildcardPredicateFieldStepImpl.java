/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.WildcardPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.WildcardPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class WildcardPredicateFieldStepImpl
		implements WildcardPredicateFieldStep<WildcardPredicateFieldMoreStep<?, ?>> {

	private final WildcardPredicateFieldMoreStepImpl.CommonState commonState;

	public WildcardPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new WildcardPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public WildcardPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new WildcardPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
