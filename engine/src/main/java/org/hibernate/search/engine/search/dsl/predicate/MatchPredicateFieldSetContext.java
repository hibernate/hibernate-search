/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when defining a match predicate, after at least one field was mentioned.
 */
public interface MatchPredicateFieldSetContext extends MultiFieldPredicateFieldSetContext<MatchPredicateFieldSetContext> {

	/**
	 * Target the given field in the match predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link MatchPredicateContext#onField(String)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return {@code this}, for method chaining.
	 *
	 * @see MatchPredicateContext#onField(String)
	 */
	default MatchPredicateFieldSetContext orField(String absoluteFieldPath) {
		return orFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the match predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link MatchPredicateContext#onFields(String...)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return {@code this}, for method chaining.
	 *
	 * @see MatchPredicateContext#onFields(String...)
	 */
	MatchPredicateFieldSetContext orFields(String ... absoluteFieldPaths);

	/**
	 * Alternative version of {@link #orField(String)} to target the given field in the match predicate.
	 * <p>
	 * Using this method it is possible to bypass any {@code DslConverter} defined on the field,
	 * in order to provide a value in {@link MatchPredicateFieldSetContext#matching(Object)} exactly as it is stored in the backend.
	 * <p>
	 * If no {@code DslConverter} are defined on the field,
	 * it will have the same behaviour of {@link #orField(String)}.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return {@code this}, for method chaining.
	 *
	 * @see MatchPredicateContext#onRawField(String)
	 */
	default MatchPredicateFieldSetContext orRawField(String absoluteFieldPath) {
		return orRawFields( absoluteFieldPath );
	}

	/**
	 * Alternative version of {@link #orFields(String...)} to target the given fields in the match predicate.
	 * <p>
	 * Equivalent to {@link #orRawField(String)} followed by multiple calls to
	 * {@link MatchPredicateFieldSetContext#orRawField(String)},
	 * the only difference being that calls to {@link MatchPredicateFieldSetContext#boostedTo(float)}
	 * and other field-specific settings on the returned context will only need to be done once
	 * and will apply to all the fields passed to this method.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return {@code this}, for method chaining.
	 *
	 * @see MatchPredicateContext#onRawFields(String...)
	 */
	MatchPredicateFieldSetContext orRawFields(String... absoluteFieldPaths);

	/**
	 * Require at least one of the targeted fields to match the given value.
	 *
	 * @param value The value to match.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See <a href="SearchPredicateFactoryContext.html#commonconcepts-parametertype">there</a> for more information.
	 * @return A context allowing to get the resulting predicate.
	 */
	SearchPredicateTerminalContext matching(Object value);

}
