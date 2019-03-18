/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when a phrase predicate is fully defined.
 * <p>
 * Allows to set options or to {@link #toPredicate() retrieve the predicate}.
 */
public interface PhrasePredicateTerminalContext extends SearchPredicateTerminalContext {

	// TODO HSEARCH-3312 allow analyzer/normalizer override

	/**
	 * Sets the slop, which defines how permissive the phrase predicate will be.
	 * <p>
	 * If zero (the default), then the predicate will only match the exact phrase given (after analysis).
	 * Higher values are increasingly permissive, allowing unexpected words or words that switched position.
	 * <p>
	 * The slop represents the number of edit operations that can be applied to the phrase to match,
	 * where each edit operation moves one word by one position.
	 * So {@code quick fox} with a slop of 1 can become {@code quick * fox}, where {@code <word>} can be any word.
	 * {@code quick fox} with a slop of 2 can become {@code quick <word> fox}, or {@code quick <word1> <word2> fox}
	 * or even {@code fox quick} (two operations: moved {@code fox} to the left and {@code quick} to the right).
	 * And similarly for higher slops and for phrases with more words.
	 *
	 * @param slop The slop value
	 * @return {@code this}, for method chaining.
	 */
	PhrasePredicateTerminalContext withSlop(int slop);

}
