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
 *
 * @param <S> The type actually exposed to the user for this context (may be a subtype of SearchQueryContext, with more exposed methods).
 * @param <Q> The type of the created query.
 */
public interface SearchQueryContext<S extends SearchQueryContext<? extends S, Q>, Q> {

	S routing(String routingKey);

	S routing(Collection<String> routingKeys);

	S sort(SearchSort sort);

	S sort(Consumer<? super SearchSortContainerContext> sortContributor);

	Q toQuery();

}
