/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when defining a range predicate,
 * after the lower bound was provided but before the upper bound was provided.
 */
public interface RangePredicateFromContext {

	/**
	 * Require at least one of the targeted fields to be "lower than" the given value,
	 * in addition to being "higher than" the value provided to the
	 * former <code>{@link RangePredicateFieldSetContext#from(Object) from}</code> call.
	 *
	 * @param value The upper bound of the range. May be null, in which case the range has no upper bound,
	 * but this is only possible if the lower bound ({@link RangePredicateFieldSetContext#from(Object)})
	 * was not null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See <a href="SearchPredicateContainerContext.html#commonconcepts-parametertype">there</a> for more information.
	 * @param inclusion Whether the upper bound should be included in or excluded from the range.
	 * @return A context allowing to set the upper bound of the range.
	 */
	SearchPredicateTerminalContext to(Object value, RangeBoundInclusion inclusion);

	/**
	 * Require at least one of the targeted fields to be "lower than" the given value,
	 * in addition to being "higher than" the value provided to the
	 * former <code>{@link RangePredicateFieldSetContext#from(Object) from}</code> call.
	 * <p>
	 * Calling this method is equivalent to calling
	 * <code>{@link #to(Object, RangeBoundInclusion) to(value, RangeBoundInclusion.INCLUDED)}</code>.
	 *
	 * @param value The lower bound of the range (included)
	 * (see {@link #to(Object, RangeBoundInclusion)} for details about null-ness and type).
	 * @return A context allowing to set the upper bound of the range.
	 *
	 * @see #to(Object, RangeBoundInclusion)
	 */
	default SearchPredicateTerminalContext to(Object value) {
		return to( value, RangeBoundInclusion.INCLUDED );
	}

}
