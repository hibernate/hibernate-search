/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The step in a "range" predicate definition where the limits of the range to match can be set
 * (see the superinterface {@link RangePredicateLimitsStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 */
public interface RangePredicateFieldMoreStep
		extends RangePredicateLimitsStep, MultiFieldPredicateFieldBoostStep<RangePredicateFieldMoreStep> {

	/**
	 * Target the given field in the range predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link RangePredicateFieldStep#onField(String)} for more information about targeting fields.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return The next step.
	 *
	 * @see RangePredicateFieldStep#onField(String)
	 */
	default RangePredicateFieldMoreStep orField(String absoluteFieldPath) {
		return orFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the range predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link RangePredicateFieldStep#onFields(String...)} for more information about targeting fields.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return The next step.
	 *
	 * @see RangePredicateFieldStep#onFields(String...)
	 */
	RangePredicateFieldMoreStep orFields(String ... absoluteFieldPaths);

}
