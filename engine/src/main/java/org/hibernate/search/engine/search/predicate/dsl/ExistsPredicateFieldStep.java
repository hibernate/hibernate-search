/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;


/**
 * The initial step in an "exists" predicate definition, where the target field can be set.
 *
 * @param <N> The type of the next step.
 */
public interface ExistsPredicateFieldStep<N extends ExistsPredicateOptionsStep<?>> {

	/**
	 * Target the given field in the "exists" predicate.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 */
	N field(String fieldPath);

}
