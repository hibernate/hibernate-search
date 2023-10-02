/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "phrase" predicate definition where the phrase to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface PhrasePredicateMatchingStep<N extends PhrasePredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to match the given phrase.
	 *
	 * @param phrase The phrase to match.
	 * @return The next step.
	 */
	N matching(String phrase);

}
