/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

public interface DisjunctionMaxPredicateBuilder extends SearchPredicateBuilder {

	void disjunct(SearchPredicate clause);

	/**
	 * @param tieBreaker The multiplier applied to the scores of the non-maximum scoring disjuncts.
	 */
	void tieBreaker(float tieBreaker);

	boolean hasClause();
}
