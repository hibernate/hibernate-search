/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The step in a "range" predicate definition where the last defined range limit can be excluded.
 *
 * @param <N> The type of the next step (returned by {@link #excludeLimit()}.
 */
public interface RangePredicateLimitExcludeStep<N> {

	/**
	 * Exclude the limit that was just defined from the range,
	 * i.e. consider that field with that exact value are outside of the range.
	 *
	 * After a {@link RangePredicateLimitsStep#below(Object)} or {@link RangePredicateFromToStep#to(Object)},
	 * this will exclude the upper bound from the range.
	 * After a {@link RangePredicateLimitsStep#above(Object)} or {@link RangePredicateLimitsStep#from(Object)},
	 * this will exclude the lower bound.
	 *
	 * @return The next step.
	 */
	N excludeLimit();

}
