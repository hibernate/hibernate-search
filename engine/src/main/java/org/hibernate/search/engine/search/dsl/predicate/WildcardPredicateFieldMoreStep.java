/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The step in a "wildcard" predicate definition where the pattern to match can be set
 * (see the superinterface {@link WildcardPredicateMatchingStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 */
public interface WildcardPredicateFieldMoreStep
		extends WildcardPredicateMatchingStep, MultiFieldPredicateFieldBoostStep<WildcardPredicateFieldMoreStep> {

	/**
	 * Target the given field in the wildcard predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link WildcardPredicateFieldStep#onField(String)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return The next step.
	 *
	 * @see WildcardPredicateFieldStep#onField(String)
	 */
	default WildcardPredicateFieldMoreStep orField(String absoluteFieldPath) {
		return orFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the wildcard predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link WildcardPredicateFieldStep#onFields(String...)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return The next step.
	 *
	 * @see WildcardPredicateFieldStep#onFields(String...)
	 */
	WildcardPredicateFieldMoreStep orFields(String... absoluteFieldPaths);

}
