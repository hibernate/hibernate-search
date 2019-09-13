/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query;

/**
 * A base interface for subtypes of {@link SearchFetchable} allowing to
 * easily override the result type for all relevant methods.
 *
 * @param <H> The type of query hits.
 * @param <R> The result type (extending {@link SearchResult}).
 */
public interface ExtendedSearchFetchable<H, R extends SearchResult<H>> extends SearchFetchable<H> {

	@Override
	R fetch(Integer limit);

	@Override
	R fetch(Integer offset, Integer limit);

	@Override
	R fetchAll();

	@Deprecated
	@Override
	default R fetch() {
		return fetchAll();
	}

}
