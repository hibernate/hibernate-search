/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

/**
 * @param <T> The type of results.
 */
public interface IndexSearchQuery<T> {

	default IndexSearchResult<T> fetch() {
		return fetch( null, null );
	}

	IndexSearchResult<T> fetch(Long limit, Long offset);

	long fetchTotalHitCount();

	String getQueryString();

}
