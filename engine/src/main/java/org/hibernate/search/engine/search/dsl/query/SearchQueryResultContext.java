/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;

/**
 * The context used when building a query, after the search result type has been defined.
 */
public interface SearchQueryResultContext<Q> {

	SearchQueryContext<Q> predicate(SearchPredicate predicate);

	SearchQueryContext<Q> predicate(Consumer<? super SearchPredicateContainerContext<SearchPredicate>> predicateContributor);

}
