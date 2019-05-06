/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * An abstract base for implementations of {@link SearchQuery}.
 *
 * @param <T> The type of query hits.
 * @param <R> The result type (extending {@link SearchResult}).
 */
public abstract class AbstractSearchQuery<T, R extends SearchResult<T>> implements SearchQuery<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getQueryString() + ")";
	}

	@Override
	public R fetch() {
		return fetch( (Long) null, null );
	}

	@Override
	public R fetch(Long limit) {
		return fetch( limit, null );
	}

	@Override
	public R fetch(Integer limit) {
		return fetch( limit == null ? null : limit.longValue(), null );
	}

	@Override
	public abstract R fetch(Long limit, Long offset);

	@Override
	public R fetch(Integer limit, Integer offset) {
		return fetch( limit == null ? null : (long) limit, offset == null ? null : (long) offset );
	}

	@Override
	public List<T> fetchHits() {
		return fetchHits( (Long) null, null );
	}

	@Override
	public List<T> fetchHits(Long limit) {
		return fetchHits( limit, null );
	}

	@Override
	public List<T> fetchHits(Integer limit) {
		return fetchHits( limit == null ? null : limit.longValue(), null );
	}

	@Override
	public List<T> fetchHits(Long limit, Long offset) {
		return fetch( limit, offset ).getHits();
	}

	@Override
	public List<T> fetchHits(Integer limit, Integer offset) {
		return fetchHits( limit == null ? null : (long) limit, offset == null ? null : (long) offset );
	}

	@Override
	public Optional<T> fetchSingleHit() {
		// We don't need to fetch more than two elements to detect a problem
		R result = fetch( 2L );
		List<T> hits = result.getHits();
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

}
