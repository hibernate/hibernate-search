/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when starting to define a wildcard predicate.
 */
public interface WildcardPredicateContext {

	/**
	 * Target the given field in the wildcard predicate.
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
	 * @return A {@link WildcardPredicateFieldSetContext} allowing to define field-specific settings
	 * (such as the {@link WildcardPredicateFieldSetContext#boostedTo(float) boost}),
	 * or simply to continue the definition of the wildcard predicate
	 * ({@link WildcardPredicateFieldSetContext#matching(String) wildcard to match}, ...).
	 */
	default WildcardPredicateFieldSetContext onField(String absoluteFieldPath) {
		return onFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the wildcard predicate.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * Equivalent to {@link #onField(String)} followed by multiple calls to
	 * {@link WildcardPredicateFieldSetContext#orField(String)},
	 * the only difference being that calls to {@link WildcardPredicateFieldSetContext#boostedTo(float)}
	 * and other field-specific settings on the returned context will only need to be done once
	 * and will apply to all the fields passed to this method.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return A {@link WildcardPredicateFieldSetContext} (see {@link #onField(String)} for details).
	 *
	 * @see #onField(String)
	 */
	WildcardPredicateFieldSetContext onFields(String... absoluteFieldPaths);

}
