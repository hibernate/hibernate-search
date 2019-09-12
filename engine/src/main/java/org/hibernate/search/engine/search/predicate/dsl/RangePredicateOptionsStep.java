/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The final step in a "range" predicate definition, where optional parameters can be set.
 */
public interface RangePredicateOptionsStep
		extends PredicateFinalStep, PredicateBoostStep<RangePredicateOptionsStep> {

}
