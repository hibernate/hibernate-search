/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.query.impl;

import org.hibernate.search.engine.search.query.spi.SearchQuery;
import org.hibernate.search.mapper.javabean.search.query.JavaBeanSearchQuery;
import org.hibernate.search.mapper.javabean.search.query.JavaBeanSearchResult;

public class JavaBeanSearchQueryImpl<T> implements JavaBeanSearchQuery<T> {

	private final SearchQuery<T> delegate;

	public JavaBeanSearchQueryImpl(SearchQuery<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public JavaBeanSearchResult<T> execute() {
		return new JavaBeanSearchResultImpl( delegate.execute() );
	}

	@Override
	public long executeCount() {
		return delegate.executeCount();
	}

	@Override
	public void setFirstResult(Long firstResultIndex) {
		delegate.setFirstResult( firstResultIndex );
	}

	@Override
	public void setMaxResults(Long maxResultsCount) {
		delegate.setMaxResults( maxResultsCount );
	}

	@Override
	public String getQueryString() {
		return delegate.getQueryString();
	}
}
