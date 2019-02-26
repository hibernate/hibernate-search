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

	// TODO wildcard, fuzzy. Or maybe a separate context?

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
	 * Alternative version of {@link #onField(String)} to target the given field in the match predicate.
	 * <p>
	 * Using this method it is possible to bypass any {@code DslConverter} defined on the field,
	 * in order to provide a value in {@link MatchPredicateFieldSetContext#matching(Object)} exactly as it is stored in the backend.
	 * <p>
	 * If no {@code DslConverter} are defined on the field,
	 * it will have the same behaviour of {@link #onField(String)}.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return A {@link MatchPredicateFieldSetContext} (see {@link #onField(String)} for details).
	 */
	default MatchPredicateFieldSetContext onRawField(String absoluteFieldPath) {
		return onRawFields( absoluteFieldPath );
	}

	/**
	 * Alternative version of {@link #onFields(String...)} to target the given fields in the match predicate.
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
	 * @see #onFields(String...)
	 * @see #onField(String)
	 */
	MatchPredicateFieldSetContext onRawFields(String... absoluteFieldPaths);
}
