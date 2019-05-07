/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query;

import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;

/**
 * The context used when building a query, after the search result type has been defined.
 *
 * @param <N> The type of the next context, returned after a predicate is defined.
 * @param <T> The type of hits for the created query.
 * @param <PC> The type of contexts used to create predicates in {@link #predicate(Function)}.
 */
public interface SearchQueryResultContext<
		N extends SearchQueryContext<? extends N, T, ?>,
		T,
		PC extends SearchPredicateFactoryContext
		> {

	/**
	 * Set the predicate for this query.
	 * @param predicate A {@link SearchPredicate} object obtained from the search scope.
	 * @return A context allowing to define the query further.
	 */
	N predicate(SearchPredicate predicate);

	/**
	 * Set the predicate for this query.
	 * @param predicateContributor A function that will use the DSL context passed in parameter to create a predicate,
	 * returning the resulting terminal context.
	 * Should generally be a lambda expression.
	 * @return A context allowing to define the query further.
	 */
	N predicate(Function<? super PC, SearchPredicateTerminalContext> predicateContributor);

}
