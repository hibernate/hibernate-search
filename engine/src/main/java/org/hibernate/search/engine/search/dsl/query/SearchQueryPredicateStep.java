/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query;

import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * The step in a query definition where the predicate can be set.
 *
 * @param <N> The type of the next step, returned after a predicate is defined.
 * @param <H> The type of hits for the created query.
 * @param <PDF> The type of factory used to create predicates in {@link #predicate(Function)}.
 */
public interface SearchQueryPredicateStep<
				N extends SearchQueryOptionsStep<?, H, ?, ?>,
				H,
				PDF extends SearchPredicateFactory
		> {

	/**
	 * Set the predicate for this query.
	 * @param predicate A {@link SearchPredicate} object obtained from the search scope.
	 * @return The next step.
	 */
	N predicate(SearchPredicate predicate);

	/**
	 * Set the predicate for this query.
	 * @param predicateContributor A function that will use the factory passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @return The next step.
	 */
	N predicate(Function<? super PDF, ? extends PredicateFinalStep> predicateContributor);

}
