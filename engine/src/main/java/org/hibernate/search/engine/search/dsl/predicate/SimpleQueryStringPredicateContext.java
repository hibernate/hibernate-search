/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when starting to define a simple query string predicate.
 */
public interface SimpleQueryStringPredicateContext {

	/**
	 * Target the given field in the simple query string predicate.
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
	 * @return A {@link SimpleQueryStringPredicateFieldSetContext} allowing to define field-specific settings
	 * (such as the {@link SimpleQueryStringPredicateFieldSetContext#boostedTo(float) boost}),
	 * or simply to continue the definition of the simpleQueryString predicate
	 * ({@link SimpleQueryStringPredicateFieldSetContext#matching(String) simpleQueryString to match}, ...).
	 */
	default SimpleQueryStringPredicateFieldSetContext onField(String absoluteFieldPath) {
		return onFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the simple query string predicate.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * Equivalent to {@link #onField(String)} followed by multiple calls to
	 * {@link SimpleQueryStringPredicateFieldSetContext#orField(String)},
	 * the only difference being that calls to {@link SimpleQueryStringPredicateFieldSetContext#boostedTo(float)}
	 * and other field-specific settings on the returned context will only need to be done once
	 * and will apply to all the fields passed to this method.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return A {@link SimpleQueryStringPredicateFieldSetContext} (see {@link #onField(String)} for details).
	 *
	 * @see #onField(String)
	 */
	SimpleQueryStringPredicateFieldSetContext onFields(String... absoluteFieldPaths);

}
