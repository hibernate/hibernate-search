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
 */
public interface SearchQueryResultContext<Q> {

	/**
	 * Set the predicate for this query.
	 * @param predicate A {@link SearchPredicate} object obtained from the search scope.
	 * @return A context allowing to define the query further.
	 */
	SearchQueryContext<Q> predicate(SearchPredicate predicate);

	/**
	 * Set the predicate for this query.
	 * @param predicateContributor A function that will use the DSL context passed in parameter to create a predicate,
	 * returning the resulting terminal context.
	 * Should generally be a lambda expression.
	 * @return A context allowing to define the query further.
	 */
	SearchQueryContext<Q> predicate(Function<? super SearchPredicateFactoryContext, SearchPredicateTerminalContext> predicateContributor);

}
