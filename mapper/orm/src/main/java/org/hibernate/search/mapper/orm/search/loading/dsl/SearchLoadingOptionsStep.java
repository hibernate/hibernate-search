/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.dsl;

import java.util.function.Consumer;

import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;

/**
 * The DSL entry point passed to consumers in
 * {@link org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep#loading(Consumer)},
 * allowing the definition of loading options (fetch size, cache lookups, ...).
 */
public interface SearchLoadingOptionsStep {

	/**
	 * Set the fetch size for this query,
	 * i.e. the amount of entities to load for each query to the database.
	 * <p>
	 * Higher numbers mean fewer queries, but larger result sets.
	 *
	 * @param fetchSize The fetch size. Must be positive or zero.
	 * @return {@code this} for method chaining.
	 * @see Query#setFetchSize(int)
	 */
	SearchLoadingOptionsStep fetchSize(int fetchSize);

	/**
	 * Set the strategy for cache lookup before query results are loaded.
	 *
	 * @param strategy The strategy.
	 * @return {@code this} for method chaining.
	 */
	SearchLoadingOptionsStep cacheLookupStrategy(EntityLoadingCacheLookupStrategy strategy);

}
