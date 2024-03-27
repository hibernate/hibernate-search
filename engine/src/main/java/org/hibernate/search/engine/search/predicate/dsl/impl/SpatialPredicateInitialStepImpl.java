/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.SpatialPredicateInitialStep;
import org.hibernate.search.engine.search.predicate.dsl.SpatialWithinPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class SpatialPredicateInitialStepImpl implements SpatialPredicateInitialStep {

	private final SearchPredicateDslContext<?> dslContext;

	public SpatialPredicateInitialStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public SpatialWithinPredicateFieldStep<?> within() {
		return new SpatialWithinPredicateFieldStepImpl( dslContext );
	}
}
