/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query.impl;

import org.hibernate.search.engine.search.query.spi.AbstractSearchQuery;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.query.spi.IndexSearchResult;
import org.hibernate.search.mapper.orm.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.search.query.SearchResult;

public class HibernateOrmSearchQuery<R> extends AbstractSearchQuery<R, SearchResult<R>>
		implements SearchQuery<R> {

	private final IndexSearchQuery<R> delegate;

	public HibernateOrmSearchQuery(IndexSearchQuery<R> delegate) {
		this.delegate = delegate;
	}

	@Override
	public SearchResult<R> fetch(Long limit, Long offset) {
		return doFetch( limit, offset );
	}

	@Override
	public long fetchTotalHitCount() {
		return delegate.fetchTotalHitCount();
	}

	@Override
	public String getQueryString() {
		return delegate.getQueryString();
	}

	IndexSearchQuery<R> getIndexSearchQuery() {
		return delegate;
	}

	private SearchResult<R> doFetch(Long limit, Long offset) {
		// TODO HSEARCH-3352 handle timeouts
		final IndexSearchResult<R> results = delegate.fetch( limit, offset );
		return new HibernateOrmSearchResult<>( results );
	}
}
