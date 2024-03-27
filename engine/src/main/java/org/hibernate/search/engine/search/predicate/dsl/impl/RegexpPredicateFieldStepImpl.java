/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.RegexpPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.RegexpPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class RegexpPredicateFieldStepImpl
		implements RegexpPredicateFieldStep<RegexpPredicateFieldMoreStep<?, ?>> {

	private final RegexpPredicateFieldMoreStepImpl.CommonState commonState;

	public RegexpPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new RegexpPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public RegexpPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new RegexpPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
