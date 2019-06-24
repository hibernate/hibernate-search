/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The step in a "simple query string" predicate definition where the query string to match can be set
 * (see the superinterface {@link SimpleQueryStringPredicateMatchingStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 */
public interface SimpleQueryStringPredicateFieldMoreStep
		extends SimpleQueryStringPredicateMatchingStep,
		MultiFieldPredicateFieldBoostStep<SimpleQueryStringPredicateFieldMoreStep> {

	/**
	 * Target the given field in the simple query string predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link SimpleQueryStringPredicateFieldStep#onField(String)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return The next step.
	 *
	 * @see SimpleQueryStringPredicateFieldStep#onField(String)
	 */
	default SimpleQueryStringPredicateFieldMoreStep orField(String absoluteFieldPath) {
		return orFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the simple query string predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link SimpleQueryStringPredicateFieldStep#onFields(String...)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return The next step.
	 *
	 * @see SimpleQueryStringPredicateFieldStep#onFields(String...)
	 */
	SimpleQueryStringPredicateFieldMoreStep orFields(String... absoluteFieldPaths);

}
