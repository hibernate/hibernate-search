/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.query.impl;

import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.mapper.javabean.search.query.SearchQuery;
import org.hibernate.search.mapper.javabean.search.query.SearchResult;

public class JavaBeanSearchQuery<T> implements SearchQuery<T> {

	private final IndexSearchQuery<T> delegate;

	public JavaBeanSearchQuery(IndexSearchQuery<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public SearchResult<T> getResult() {
		return new JavaBeanSearchResult( delegate.execute() );
	}

	@Override
	public long getResultSize() {
		return delegate.executeCount();
	}

	@Override
	public JavaBeanSearchQuery<T> setFirstResult(Long firstResultIndex) {
		delegate.setFirstResult( firstResultIndex );
		return this;
	}

	@Override
	public JavaBeanSearchQuery<T> setMaxResults(Long maxResultsCount) {
		delegate.setMaxResults( maxResultsCount );
		return this;
	}

	@Override
	public String getQueryString() {
		return delegate.getQueryString();
	}
}
