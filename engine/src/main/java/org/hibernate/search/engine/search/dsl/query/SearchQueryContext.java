/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query;


import java.util.Collection;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortTerminalContext;
import org.hibernate.search.engine.search.query.SearchFetchable;
import org.hibernate.search.engine.search.query.SearchQuery;

/**
 * The context used when building a query, after the search predicate has been defined.
 *
 * @param <S> The type actually exposed to the user for this context (may be a subtype of SearchQueryContext, with more exposed methods).
 * @param <H> The type of hits for the created query.
 * @param <SC> The type of contexts used to create sorts in {@link #sort(Function)}.
 */
public interface SearchQueryContext<
		S extends SearchQueryContext<? extends S, H, SC>,
		H,
		SC extends SearchSortContainerContext
		>
		extends SearchFetchable<H> {

	S routing(String routingKey);

	S routing(Collection<String> routingKeys);

	S sort(SearchSort sort);

	S sort(Function<? super SC, ? extends SearchSortTerminalContext> sortContributor);

	SearchQuery<H> toQuery();

}
