/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "phrase" predicate definition where the phrase to match can be set.
 */
public interface PhrasePredicateMatchingStep {

	/**
	 * Require at least one of the targeted fields to match the given phrase.
	 *
	 * @param phrase The phrase to match.
	 * @return The next step.
	 */
	PhrasePredicateOptionsStep matching(String phrase);

}
