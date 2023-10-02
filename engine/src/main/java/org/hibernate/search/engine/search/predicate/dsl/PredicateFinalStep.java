/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * The final step in a predicate definition, where the predicate can be retrieved.
 */
public interface PredicateFinalStep {

	/**
	 * Create a {@link SearchPredicate} instance
	 * matching the definition given in the previous DSL steps.
	 *
	 * @return The {@link SearchPredicate} resulting from the previous DSL steps.
	 */
	SearchPredicate toPredicate();

}
