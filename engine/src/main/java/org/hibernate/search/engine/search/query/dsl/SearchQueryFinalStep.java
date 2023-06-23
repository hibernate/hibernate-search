/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl;

import org.hibernate.search.engine.search.query.SearchFetchable;
import org.hibernate.search.engine.search.query.SearchQuery;

/**
 * The final step in a query definition,
 * where the query can be {@link SearchFetchable executed} or {@link #toQuery() retrieved as an object}.
 *
 * @param <H> The type of hits for the created query.
 */
public interface SearchQueryFinalStep<H>
		extends SearchFetchable<H> {

	/**
	 * Create a {@link SearchQuery} instance
	 * matching the definition given in the previous DSL steps.
	 * <p>
	 * Calling this method is generally not necessary as most query execution methods
	 * are also implemented by this DSL step,
	 * so for example {@code .toQuery().fetch()} can be replaced with simply {@code .fetch()}.
	 *
	 * @return The {@link SearchQuery} resulting from the previous DSL steps.
	 */
	SearchQuery<H> toQuery();

}
