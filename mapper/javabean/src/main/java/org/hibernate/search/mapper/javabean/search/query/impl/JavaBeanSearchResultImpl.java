/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.query.impl;

import java.util.List;

import org.hibernate.search.engine.search.query.spi.SearchResult;
import org.hibernate.search.mapper.javabean.search.query.JavaBeanSearchResult;

class JavaBeanSearchResultImpl<T> implements JavaBeanSearchResult<T> {

	private final SearchResult<T> delegate;

	JavaBeanSearchResultImpl(SearchResult<T> delegate) {
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
