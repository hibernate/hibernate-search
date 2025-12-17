/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.search.query.spi;

import java.util.function.Function;

import jakarta.persistence.QueryTimeoutException;

import org.hibernate.ScrollableResults;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.mapper.orm.logging.impl.OrmMiscLog;
import org.hibernate.search.util.common.SearchTimeoutException;

public class HibernateOrmSearchScrollableResultsAdapter<R, H>
		implements ScrollableResults<R>, ScrollableResultsImplementor<R> {

	private final SearchScroll<H> scroll;
	private final int maxResults;
	private final Function<? super H, ? extends R> hitExtractor;
	private SearchScrollResult<H> currentChunk;
	private H currentHit;
	private int currentIndexInScroll;
	private int currentIndexInCurrentChunk;
	private boolean afterLast;
	private boolean closed;

	public HibernateOrmSearchScrollableResultsAdapter(SearchScroll<H> scroll, int maxResults,
			Function<? super H, ? extends R> hitExtractor) {
		this.scroll = scroll;
		this.maxResults = maxResults;
		this.hitExtractor = hitExtractor;
		this.currentChunk = null;
		this.currentHit = null;
		this.currentIndexInScroll = -1;
		this.currentIndexInCurrentChunk = -1;
		this.afterLast = false;
		this.closed = false;
	}

	@Override
	public boolean next() {
		checkNotClosed();
		return scroll( 1 );
	}

	@Override
	public boolean previous() {
		checkNotClosed();
		throw OrmMiscLog.INSTANCE.cannotScrollBackwards();
	}

	@Override
	public boolean scroll(int positions) {
		checkNotClosed();
		if ( positions < 0 ) {
			throw OrmMiscLog.INSTANCE.cannotScrollBackwards();
		}
		if ( afterLast ) {
			return false;
		}
		if ( positions == 0 ) {
			return currentIndexInScroll >= 0;
		}
		currentIndexInScroll += positions;
		currentIndexInCurrentChunk += positions;
		if ( currentIndexInScroll >= maxResults ) {
			afterLast();
			return false;
		}
		if ( currentChunk == null ) { // Very first call
			currentChunk = nextChunk();
		}
		int currentChunkSize = currentChunk.hits().size();
		while ( currentIndexInCurrentChunk >= currentChunkSize && currentChunk.hasHits() ) {
			currentIndexInCurrentChunk -= currentChunkSize;
			currentChunk = nextChunk();
			currentChunkSize = currentChunk.hits().size();
		}
		if ( currentIndexInCurrentChunk >= currentChunk.hits().size() ) {
			afterLast();
			return false;
		}
		currentHit = currentChunk.hits().get( currentIndexInCurrentChunk );
		if ( currentIndexInCurrentChunk == ( currentChunkSize - 1 ) ) {
			// Fetch the next chunk in order to be able to implement isLast()
			currentChunk = nextChunk();
			currentIndexInCurrentChunk = -1;
		}
		return true;
	}

	@Override
	public boolean last() {
		checkNotClosed();
		if ( afterLast ) {
			throw OrmMiscLog.INSTANCE.cannotScrollBackwards();
		}
		while ( !isLast() && !afterLast ) {
			next();
		}
		return isLast(); // May be false if the scroll has no hits
	}

	@Override
	public boolean first() {
		checkNotClosed();
		if ( currentIndexInScroll == 0 ) {
			return true;
		}
		if ( currentIndexInScroll != -1 ) {
			throw OrmMiscLog.INSTANCE.cannotScrollBackwards();
		}
		return scroll( 1 );
	}

	@Override
	public void beforeFirst() {
		checkNotClosed();
		if ( currentIndexInScroll != -1 ) {
			throw OrmMiscLog.INSTANCE.cannotScrollBackwards();
		}
	}

	@Override
	public void afterLast() {
		checkNotClosed();
		currentChunk = null;
		currentHit = null;
		currentIndexInScroll = Integer.MAX_VALUE;
		currentIndexInCurrentChunk = -1;
		afterLast = true;
	}

	@Override
	public boolean isFirst() {
		return !afterLast && currentIndexInScroll == 0;
	}

	@Override
	public boolean isLast() {
		// If we're on the last element, we should have already fetched the last (empty) chunk
		return !afterLast
				&& ( currentIndexInScroll == ( maxResults - 1 )
						|| currentChunk != null && !currentChunk.hasHits() );
	}

	@Override
	public void close() {
		if ( closed ) {
			return;
		}
		closed = true;
		try {
			scroll.close();
		}
		catch (RuntimeException e) {
			OrmMiscLog.INSTANCE.unableToCloseSearcherInScrollableResult( e );
		}
	}

	@SuppressWarnings("removal")
	@Deprecated(since = "8.0", forRemoval = true)
	@Override
	public int getRowNumber() {
		if ( afterLast ) {
			return -1;
		}
		return currentIndexInScroll;
	}

	@SuppressWarnings({ "removal", "deprecation" }) // For EJC
	@Override
	public boolean position(int position) {
		return setRowNumber( position - 1 );
	}

	@Override
	public int getPosition() {
		if ( afterLast ) {
			return -1;
		}
		return currentIndexInScroll + 1;
	}

	@SuppressWarnings("removal")
	@Deprecated(since = "8.0", forRemoval = true)
	@Override
	public boolean setRowNumber(int rowNumber) {
		checkNotClosed();

		if ( rowNumber < 0 ) {
			// Can't set the position relative to the last element if we're forward only,
			// since we don't know it's the last element until we reach it.
			throw OrmMiscLog.INSTANCE.cannotSetScrollPositionRelativeToEnd();
		}

		return scroll( rowNumber - currentIndexInScroll );
	}

	// We cannot use @Override here because this method only exists in ORM 6.1.2+
	public void setFetchSize(int i) {
		throw OrmMiscLog.INSTANCE.cannotSetFetchSize();
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public R get() {
		checkNotClosed();
		if ( currentIndexInScroll < 0 || afterLast ) {
			return null;
		}
		return hitExtractor.apply( currentHit );
	}

	private SearchScrollResult<H> nextChunk() {
		try {
			return scroll.next();
		}
		catch (SearchTimeoutException e) {
			throw new QueryTimeoutException( e );
		}
	}

	private void checkNotClosed() {
		if ( closed ) {
			throw OrmMiscLog.INSTANCE.cannotUseClosedScrollableResults();
		}
	}

}
