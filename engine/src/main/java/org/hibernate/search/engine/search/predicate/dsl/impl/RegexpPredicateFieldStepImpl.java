/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.RegexpPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.RegexpPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class RegexpPredicateFieldStepImpl<SR>
		implements RegexpPredicateFieldStep<SR, RegexpPredicateFieldMoreStep<SR, ?, ?>> {

	private final RegexpPredicateFieldMoreStepImpl.CommonState<SR> commonState;

	public RegexpPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new RegexpPredicateFieldMoreStepImpl.CommonState<>( dslContext );
	}

	@Override
	public RegexpPredicateFieldMoreStep<SR, ?, ?> fields(String... fieldPaths) {
		return new RegexpPredicateFieldMoreStepImpl<>( commonState, Arrays.asList( fieldPaths ) );
	}
}
