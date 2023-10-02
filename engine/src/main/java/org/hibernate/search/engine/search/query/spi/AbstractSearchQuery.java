/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
 * @param <H> The type of query hits.
 * @param <R> The result type (extending {@link SearchResult}).
 */
public abstract class AbstractSearchQuery<H, R extends SearchResult<H>> implements SearchQueryImplementor<H> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + queryString() + ")";
	}

	@Override
	public R fetchAll() {
		return fetch( null, null );
	}

	@Override
	public R fetch(Integer limit) {
		return fetch( null, limit );
	}

	@Override
	public abstract R fetch(Integer offset, Integer limit);

	@Override
	public List<H> fetchAllHits() {
		return fetchHits( null, null );
	}

	@Override
	public List<H> fetchHits(Integer limit) {
		return fetchHits( null, limit );
	}

	@Override
	public Optional<H> fetchSingleHit() {
		// We don't need to fetch more than two elements to detect a problem
		List<H> hits = fetchHits( 2 );
		int fetchedHitCount = hits.size();
		if ( fetchedHitCount == 0 ) {
			return Optional.empty();
		}
		else if ( fetchedHitCount > 1 ) {
			throw log.nonSingleHit();
		}
		else {
			return Optional.of( hits.get( 0 ) );
		}
	}

}
