/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when defining a simple query string predicate, after at least one field was mentioned.
 */
public interface SimpleQueryStringPredicateFieldSetContext extends MultiFieldPredicateFieldSetContext<SimpleQueryStringPredicateFieldSetContext> {

	/**
	 * Define the default operator as AND.
	 * <p>
	 * By default, unless the query string contains explicit operators,
	 * documents will match if <em>any</em> term mentioned in the query string is present in the document (OR operator).
	 * This will change the default behavior,
	 * making document match if <em>all</em> terms mentioned in the query string are present in the document.
	 *
	 * @return {@code this}, for method chaining.
	 */
	SimpleQueryStringPredicateFieldSetContext withAndAsDefaultOperator();

	/**
	 * Target the given field in the simple query string predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link SimpleQueryStringPredicateContext#onField(String)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return {@code this}, for method chaining.
	 *
	 * @see SimpleQueryStringPredicateContext#onField(String)
	 */
	default SimpleQueryStringPredicateFieldSetContext orField(String absoluteFieldPath) {
		return orFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the simple query string predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link SimpleQueryStringPredicateContext#onFields(String...)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return {@code this}, for method chaining.
	 *
	 * @see SimpleQueryStringPredicateContext#onFields(String...)
	 */
	SimpleQueryStringPredicateFieldSetContext orFields(String... absoluteFieldPaths);

	/**
	 * Require at least one of the targeted fields to match the given query string.
	 *
	 * @param simpleQueryString The query string to match.
	 * @return A context allowing to get the resulting predicate.
	 */
	SearchPredicateTerminalContext matching(String simpleQueryString);

}
