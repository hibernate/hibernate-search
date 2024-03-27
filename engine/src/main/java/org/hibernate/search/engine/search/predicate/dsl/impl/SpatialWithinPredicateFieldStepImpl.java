/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.SpatialWithinPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.SpatialWithinPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

class SpatialWithinPredicateFieldStepImpl
		implements SpatialWithinPredicateFieldStep<SpatialWithinPredicateFieldMoreStep<?, ?>> {

	private final SpatialWithinPredicateFieldMoreStepImpl.CommonState commonState;

	SpatialWithinPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new SpatialWithinPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public SpatialWithinPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new SpatialWithinPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
