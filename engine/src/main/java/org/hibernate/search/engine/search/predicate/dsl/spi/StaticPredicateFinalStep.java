/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.spi;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;

public final class StaticPredicateFinalStep implements PredicateFinalStep {
	private final SearchPredicate predicate;

	public StaticPredicateFinalStep(SearchPredicate predicate) {
		this.predicate = predicate;
	}

	@Override
	public SearchPredicate toPredicate() {
		return predicate;
	}
}
