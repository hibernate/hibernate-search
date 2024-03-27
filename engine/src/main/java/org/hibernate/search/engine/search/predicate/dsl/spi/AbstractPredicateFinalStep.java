/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.spi;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;

/**
 * An abstract base for {@link PredicateFinalStep} implementations.
 */
public abstract class AbstractPredicateFinalStep implements PredicateFinalStep {

	protected final SearchPredicateDslContext<?> dslContext;

	private SearchPredicate predicateResult;

	public AbstractPredicateFinalStep(SearchPredicateDslContext<?> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public SearchPredicate toPredicate() {
		if ( predicateResult == null ) {
			predicateResult = build();
		}
		return predicateResult;
	}

	protected abstract SearchPredicate build();
}
