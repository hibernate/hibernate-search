/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "prefix" predicate definition where the prefix string to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface PrefixPredicateMatchingStep<N extends PrefixPredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to start with the given prefix string.
	 *
	 * @param prefix The prefix a matched field should start with.
	 * @return The next step.
	 */
	N matching(String prefix);

}
