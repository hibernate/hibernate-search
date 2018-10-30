/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query;


import java.util.Collection;
import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

/**
 * The context used when building a query, after the search predicate has been defined.
 */
public interface SearchQueryContext<Q> {

	SearchQueryContext<Q> routing(String routingKey);

	SearchQueryContext<Q> routing(Collection<String> routingKeys);

	SearchQueryContext<Q> sort(SearchSort sort);

	SearchQueryContext<Q> sort(Consumer<? super SearchSortContainerContext> sortContributor);

	Q build();

}
