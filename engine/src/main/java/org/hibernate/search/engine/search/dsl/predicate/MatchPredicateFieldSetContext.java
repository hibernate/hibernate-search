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
