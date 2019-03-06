/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query.impl;

import java.util.List;

import org.hibernate.search.engine.search.query.spi.IndexSearchResult;
import org.hibernate.search.mapper.orm.search.query.SearchResult;

class HibernateOrmSearchResult<T> implements SearchResult<T> {

	private final IndexSearchResult<T> delegate;

	HibernateOrmSearchResult(IndexSearchResult<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public long getHitCount() {
		return delegate.getHitCount();
	}

	@Override
	public List<T> getHits() {
		return delegate.getHits();
	}
}
