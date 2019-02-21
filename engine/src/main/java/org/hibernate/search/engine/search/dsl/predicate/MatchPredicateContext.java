/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * The context used when starting to define a match predicate.
 */
public interface MatchPredicateContext extends SearchPredicateScoreContext<MatchPredicateContext> {

	/**
	 * Enable fuzziness for this match predicate; only works for text fields.
	 * <p>
	 * Fuzziness allows to match documents that do not contain the value to match,
	 * but a close value, for example with one letter that differs.
	 *
	 * @return {@code this}, for method chaining.
	 * @see #fuzzy(int, int)
	 */
	default MatchPredicateContext fuzzy() {
		return fuzzy( 2, 0 );
	}

	/**
	 * Enable fuzziness for this match predicate; only works for text fields.
	 * <p>
	 * Fuzziness allows to match documents that do not contain the value to match,
	 * but a close value, for example with one letter that differs.
	 *
	 * @param maxEditDistance The maximum value of the edit distance, which defines how permissive the fuzzy predicate will be.
	 * @return {@code this}, for method chaining.
	 * @see #fuzzy(int, int)
	 */
	default MatchPredicateContext fuzzy(int maxEditDistance) {
		return fuzzy( maxEditDistance, 0 );
	}

	/**
	 * Enable fuzziness for this match predicate; only works for text fields.
	 * <p>
	 * Fuzziness allows to match documents that do not contain the value to match,
	 * but a close value, for example with one letter that differs.
	 *
	 * @param maxEditDistance The maximum value of the edit distance, which defines how permissive the fuzzy predicate will be.
	 * <p>
	 * Roughly speaking, the edit distance is the number of changes between two terms: switching characters, removing them, ...
	 * <p>
	 * If zero, then fuzziness is completely disabled.
	 * The other accepted values, {@code 1} and {@code 2}, are increasingly fuzzy.
	 * @param exactPrefixLength Length of the prefix that has to match exactly, i.e. for which fuzziness will not be allowed.
	 * <p>
	 * A non-zero value is recommended if the index contains a large amount of distinct terms.
	 * @return {@code this}, for method chaining.
	 */
	MatchPredicateContext fuzzy(int maxEditDistance, int exactPrefixLength);

	/**
	 * Target the given field in the match predicate.
	 * <p>
	 * Multiple fields may be targeted by the same predicate:
	 * the predicate will match if <em>any</em> targeted field matches.
	 * <p>
	 * When targeting multiple fields, those fields must have compatible types.
	 * See <a href="SearchPredicateFactoryContext.html#commonconcepts-parametertype">there</a> for more information.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return A {@link MatchPredicateFieldSetContext} allowing to define field-specific settings
	 * (such as the {@link MatchPredicateFieldSetContext#boostedTo(float) boost}),
	 * or simply to continue the definition of the match predicate
	 * ({@link MatchPredicateFieldSetContext#matching(Object) value to match}, ...).
	 */
	default MatchPredicateFieldSetContext onField(String absoluteFieldPath) {
		return onFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the match predicate.
	 * <p>
	 * Equivalent to {@link #onField(String)} followed by multiple calls to
	 * {@link MatchPredicateFieldSetContext#orField(String)},
	 * the only difference being that calls to {@link MatchPredicateFieldSetContext#boostedTo(float)}
	 * and other field-specific settings on the returned context will only need to be done once
	 * and will apply to all the fields passed to this method.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return A {@link MatchPredicateFieldSetContext} (see {@link #onField(String)} for details).
	 *
	 * @see #onField(String)
	 */
	MatchPredicateFieldSetContext onFields(String ... absoluteFieldPaths);

	/**
	 * Target the given <strong>raw</strong> field in the match predicate.
	 * <p>
	 * Using this method instead of {@link #onField(String)} will disable some of the conversion applied to
	 * arguments to {@link MatchPredicateFieldSetContext#matching(Object)},
	 * allowing to pass values directly to the backend.
	 * <p>
	 * See <a href="SearchPredicateFactoryContext.html#commonconcepts-rawfield">there</a> for more information
	 * about raw fields.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return A {@link MatchPredicateFieldSetContext} (see {@link #onField(String)} for details).
	 */
	default MatchPredicateFieldSetContext onRawField(String absoluteFieldPath) {
		return onRawFields( absoluteFieldPath );
	}

	/**
	 * Target the given <strong>raw</strong> fields in the match predicate.
	 * <p>
	 * Equivalent to {@link #onRawField(String)} followed by multiple calls to
	 * {@link MatchPredicateFieldSetContext#orRawField(String)},
	 * the only difference being that calls to {@link MatchPredicateFieldSetContext#boostedTo(float)}
	 * and other field-specific settings on the returned context will only need to be done once
	 * and will apply to all the fields passed to this method.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return A {@link MatchPredicateFieldSetContext} (see {@link #onField(String)} for details).
	 *
	 * @see #onRawField(String)
	 */
	MatchPredicateFieldSetContext onRawFields(String... absoluteFieldPaths);
}
