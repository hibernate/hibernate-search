/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldMoreGenericStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.reference.predicate.RangePredicateFieldReference;

public final class RangePredicateFieldStepImpl<SR>
		implements
		RangePredicateFieldStep<SR, RangePredicateFieldMoreStep<SR, ?, ?>> {

	private final SearchPredicateDslContext<?> dslContext;

	public RangePredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public RangePredicateFieldMoreStep<SR, ?, ?> fields(String... fieldPaths) {
		return AbstractRangePredicateFieldMoreStep.create( dslContext, fieldPaths );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> RangePredicateFieldMoreGenericStep<SR, ?, ?, RangePredicateFieldReference<SR, T>, T> fields(
			RangePredicateFieldReference<SR, T>... fields) {
		return AbstractRangePredicateFieldMoreStep.create( dslContext, fields );
	}

}
