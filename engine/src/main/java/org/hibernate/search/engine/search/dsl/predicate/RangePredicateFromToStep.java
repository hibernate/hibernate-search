/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The step in a "range" predicate definition where the upper limit of the range can be set,
 * or the lower limit of the range can be excluded.
 */
public interface RangePredicateFromToStep
		extends RangePredicateToStep, RangePredicateLimitExcludeStep<RangePredicateToStep> {

}
