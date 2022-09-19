/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query.spi;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import jakarta.persistence.QueryTimeoutException;

import org.hibernate.ScrollableResults;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.type.Type;

public class HibernateOrmSearchScrollableResultsAdapter<H> implements ScrollableResults, ScrollableResultsImplementor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchScroll<H> scroll;
	private final int maxResults;
	private final ScrollHitExtractor<? super H> hitExtractor;
	private SearchScrollResult<H> currentChunk;
	private H currentHit;
	private int currentIndexInScroll;
	private int currentIndexInCurrentChunk;
	private boolean afterLast;
	private boolean closed;

	public HibernateOrmSearchScrollableResultsAdapter(SearchScroll<H> scroll, int maxResults,
			ScrollHitExtractor<? super H> hitExtractor) {
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
		throw log.cannotScrollBackwards();
	}

	@Override
	public boolean scroll(int positions) {
		checkNotClosed();
		if ( positions < 0 ) {
			throw log.cannotScrollBackwards();
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
			throw log.cannotScrollBackwards();
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
			throw log.cannotScrollBackwards();
		}
		return scroll( 1 );
	}

	@Override
	public void beforeFirst() {
		checkNotClosed();
		if ( currentIndexInScroll != -1 ) {
			throw log.cannotScrollBackwards();
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
		return !afterLast && (
				currentIndexInScroll == (maxResults - 1)
				|| currentChunk != null && !currentChunk.hasHits()
		);
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
			log.unableToCloseSearcherInScrollableResult( e );
		}
	}

	@Override
	public int getRowNumber() {
		if ( afterLast ) {
			return -1;
		}
		return currentIndexInScroll;
	}

	@Override
	public boolean setRowNumber(int rowNumber) {
		checkNotClosed();

		if ( rowNumber < 0 ) {
			// Can't set the position relative to the last element if we're forward only,
			// since we don't know it's the last element until we reach it.
			throw log.cannotSetScrollPositionRelativeToEnd();
		}

		return scroll( rowNumber - currentIndexInScroll );
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public Object[] get() {
		checkNotClosed();
		if ( currentIndexInScroll < 0 || afterLast ) {
			return null;
		}
		return hitExtractor.toArray( currentHit );
	}

	@Override
	public Object get(int i) {
		checkNotClosed();
		if ( currentIndexInScroll < 0 || afterLast ) {
			return null;
		}
		return hitExtractor.toElement( currentHit, i );
	}

	@Override
	public Type getType(int i) {
		throw columnTypesNotSupported();
	}

	@Override
	public Integer getInteger(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public Long getLong(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public Float getFloat(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public Boolean getBoolean(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public Double getDouble(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public Short getShort(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public Byte getByte(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public Character getCharacter(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public byte[] getBinary(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public String getText(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public Blob getBlob(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public Clob getClob(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public String getString(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public BigDecimal getBigDecimal(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public BigInteger getBigInteger(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public Date getDate(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public Locale getLocale(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public Calendar getCalendar(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public TimeZone getTimeZone(int col) {
		throw columnTypesNotSupported();
	}

	@Override
	public int getNumberOfTypes() {
		throw columnTypesNotSupported();
	}

	private SearchScrollResult<H> nextChunk() {
		try {
			return scroll.next();
		}
		catch (SearchTimeoutException e) {
			throw new QueryTimeoutException( e );
		}
	}

	private UnsupportedOperationException columnTypesNotSupported() {
		return new UnsupportedOperationException( "column types not supported in Hibernate Search queries" );
	}

	private void checkNotClosed() {
		if ( closed ) {
			throw log.cannotUseClosedScrollableResults();
		}
	}

	public interface ScrollHitExtractor<H> {
		Object[] toArray(H hit);
		Object toElement(H hit, int index);

		@SuppressWarnings({ "unchecked", "rawtypes" }) // The instance works for any H
		static <H> ScrollHitExtractor<H> singleObject() {
			return (ScrollHitExtractor) SingleObjectScrollHitExtractor.INSTANCE;
		}
	}

	private static final class SingleObjectScrollHitExtractor<H>
			implements HibernateOrmSearchScrollableResultsAdapter.ScrollHitExtractor<H> {

		private static final SingleObjectScrollHitExtractor<Object> INSTANCE = new SingleObjectScrollHitExtractor<>();

		@Override
		public Object[] toArray(H hit) {
			return new Object[] { hit };
		}

		@Override
		public Object toElement(H hit, int index) {
			if ( index > 0 ) {
				throw new IndexOutOfBoundsException();
			}
			return hit;
		}
	}
}
