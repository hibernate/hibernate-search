/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when defining a wildcard predicate, after at least one field was mentioned.
 */
public interface WildcardPredicateFieldSetContext extends MultiFieldPredicateFieldSetContext<WildcardPredicateFieldSetContext> {

	/**
	 * Target the given field in the wildcard predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link WildcardPredicateContext#onField(String)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return {@code this}, for method chaining.
	 *
	 * @see WildcardPredicateContext#onField(String)
	 */
	default WildcardPredicateFieldSetContext orField(String absoluteFieldPath) {
		return orFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the wildcard predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link WildcardPredicateContext#onFields(String...)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return {@code this}, for method chaining.
	 *
	 * @see WildcardPredicateContext#onFields(String...)
	 */
	WildcardPredicateFieldSetContext orFields(String... absoluteFieldPaths);

	/**
	 * Require at least one of the targeted fields to match the given wildcard pattern.
	 *
	 * @param wildcardPattern The pattern to match. Supported wildcards are {@code *},
	 * which matches any character sequence (including the empty one), and {@code ?},
	 * which matches any single character. {@code \} is the escape character.
	 * @return A context allowing to get the resulting predicate.
	 */
	WildcardPredicateTerminalContext matching(String wildcardPattern);

}
