/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query;

/**
 * A base interface for subtypes of {@link SearchQuery} allowing to
 * easily override the result type for all relevant methods.
 *
 * @param <T> The type of query hits.
 * @param <R> The result type (extending {@link SearchResult}).
 */
public interface ExtendedSearchQuery<T, R extends SearchResult<T>> extends SearchQuery<T> {

	@Override
	R fetch();

	@Override
	R fetch(Long limit);

	@Override
	R fetch(Integer limit);

	@Override
	R fetch(Long limit, Long offset);

	@Override
	R fetch(Integer limit, Integer offset);

}
