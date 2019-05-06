/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import javax.persistence.TypedQuery;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.Query;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.query.spi.IndexSearchResult;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableObjectLoadingOptions;
import org.hibernate.search.mapper.orm.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.search.query.SearchResult;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class HibernateOrmSearchQuery<R> implements SearchQuery<R> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexSearchQuery<R> delegate;
	private final SessionImplementor sessionImplementor;
	private final MutableObjectLoadingOptions loadingOptions;

	private HibernateOrmSearchQueryAdapter<R> adapter;

	public HibernateOrmSearchQuery(IndexSearchQuery<R> delegate, SessionImplementor sessionImplementor,
			MutableObjectLoadingOptions loadingOptions) {
		this.delegate = delegate;
		this.sessionImplementor = sessionImplementor;
		this.loadingOptions = loadingOptions;
	}

	@Override
	public String toString() {
		return "HibernateOrmSearchQuery(" + delegate.getQueryString() + ")";
	}

	@Override
	public TypedQuery<R> toJpaQuery() {
		return toOrmQuery();
	}

	@Override
	public Query<R> toOrmQuery() {
		if ( adapter == null ) {
			adapter = new HibernateOrmSearchQueryAdapter<>( this, sessionImplementor, loadingOptions );
		}
		return adapter;
	}

	@Override
	public SearchResult<R> fetch(Long limit, Long offset) {
		return doFetch( limit, offset );
	}

	@Override
	public List<R> fetchHits(Long limit, Long offset) {
		return fetch( limit, offset ).getHits();
	}

	@Override
	public long fetchTotalHitCount() {
		return delegate.fetchTotalHitCount();
	}

	@Override
	public Optional<R> fetchSingleHit() {
		// We don't need to fetch more than two elements to detect a problem
		SearchResult<R> result = fetch( 2L );
		List<R> hits = result.getHits();
		int fetchedHitCount = result.getHits().size();
		if ( fetchedHitCount == 0 ) {
			return Optional.empty();
		}
		else if ( fetchedHitCount > 1 ) {
			throw log.nonSingleHit( result.getTotalHitCount() );
		}
		else {
			return Optional.of( hits.get( 0 ) );
		}
	}

	IndexSearchQuery<R> getIndexSearchQuery() {
		return delegate;
	}

	String getQueryString() {
		return delegate.getQueryString();
	}

	private SearchResult<R> doFetch(Long limit, Long offset) {
		// TODO HSEARCH-3352 handle timeouts
		final IndexSearchResult<R> results = delegate.fetch( limit, offset );
		return new HibernateOrmSearchResult<>( results );
	}
}
