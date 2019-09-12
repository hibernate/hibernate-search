/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The final step in a "match id" predicate definition,
 * where more IDs to match can be set.
 */
public interface MatchIdPredicateMatchingMoreStep
		extends PredicateFinalStep, MatchIdPredicateMatchingStep {

}
