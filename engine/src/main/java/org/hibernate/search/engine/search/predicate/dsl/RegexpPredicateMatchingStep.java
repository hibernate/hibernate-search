/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "regexp" predicate definition where the pattern to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface RegexpPredicateMatchingStep<N extends RegexpPredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to match the given regular expression pattern.
	 *
	 * @param regexpPattern The pattern to match.
	 * @return The next step.
	 */
	N matching(String regexpPattern);

}
