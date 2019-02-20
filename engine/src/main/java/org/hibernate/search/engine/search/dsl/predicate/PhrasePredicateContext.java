/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when starting to define a phrase predicate.
 */
public interface PhrasePredicateContext extends SearchPredicateScoreContext<PhrasePredicateContext> {

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
	PhrasePredicateContext withSlop(int slop);

	/**
	 * Target the given field in the phrase predicate.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * Multiple fields may be targeted by the same predicate:
	 * the predicate will match if <em>any</em> targeted field matches.
	 * <p>
	 * When targeting multiple fields, those fields must have compatible types.
	 * See <a href="SearchPredicateFactoryContext.html#commonconcepts-parametertype">there</a> for more information.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return A {@link PhrasePredicateFieldSetContext} allowing to define field-specific settings
	 * (such as the {@link PhrasePredicateFieldSetContext#boostedTo(float) boost}),
	 * or simply to continue the definition of the phrase predicate
	 * ({@link PhrasePredicateFieldSetContext#matching(String) phrase to match}, ...).
	 */
	default PhrasePredicateFieldSetContext onField(String absoluteFieldPath) {
		return onFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the phrase predicate.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * Equivalent to {@link #onField(String)} followed by multiple calls to
	 * {@link PhrasePredicateFieldSetContext#orField(String)},
	 * the only difference being that calls to {@link PhrasePredicateFieldSetContext#boostedTo(float)}
	 * and other field-specific settings on the returned context will only need to be done once
	 * and will apply to all the fields passed to this method.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return A {@link PhrasePredicateFieldSetContext} (see {@link #onField(String)} for details).
	 *
	 * @see #onField(String)
	 */
	PhrasePredicateFieldSetContext onFields(String... absoluteFieldPaths);

}
