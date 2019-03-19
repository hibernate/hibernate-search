/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when a range predicate is fully defined,
 * just after the last limit was defined.
 * <p>
 * Allows to {@link #excludeLimit() exclude the last limit},
 * to set options or to {@link #toPredicate() retrieve the predicate}.
 */
public interface RangePredicateLimitTerminalContext extends RangePredicateTerminalContext {

	/**
	 * Exclude the limit that was just defined from the range.
	 *
	 * After a {@link RangePredicateFieldSetContext#below(Object)} or {@link RangePredicateFromContext#to(Object)} will exclude the upper bound from the range.
	 * After a {@link RangePredicateFieldSetContext#above(Object)} will exclude the lower bound.
	 *
	 * @return A context to get the resulting predicate.
	 */
	RangePredicateTerminalContext excludeLimit();

}
