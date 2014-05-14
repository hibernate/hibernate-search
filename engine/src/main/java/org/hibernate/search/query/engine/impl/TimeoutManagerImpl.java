/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.Query;
import org.apache.lucene.util.Counter;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.engine.spi.TimingSource;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.query.engine.spi.TimeoutManager;

/**
 * @author Emmanuel Bernard
 */
public class TimeoutManagerImpl implements TimeoutManager {
	// timeout in nanoseconds
	private Long timeout;
	private long start;
	boolean timedOut = false;
	private final Query luceneQuery;
	private Type type;
	private boolean partialResults;
	private final TimeoutExceptionFactory timeoutExceptionFactory;
	private final TimingSource timingSource;

	public TimeoutManagerImpl(Query query, TimeoutExceptionFactory timeoutExceptionFactory, TimingSource timingSource) {
		this.luceneQuery = query;
		this.timeoutExceptionFactory = timeoutExceptionFactory;
		this.timingSource = timingSource;
	}

	/** we start counting from this method call (if needed) */
	@Override
	public void start() {
		if ( timeout == null ) {
			return;
		}
		this.start = System.nanoTime();
		this.partialResults = false;
	}

	@Override
	public Long getTimeoutLeftInMilliseconds() {
		return getTimeoutLeft( 1000000 );
	}

	@Override
	public Long getTimeoutLeftInSeconds() {
		return getTimeoutLeft( 1000000000 );
	}

	private Long getTimeoutLeft(long factor) {
		if ( timeout == null ) {
			return null;
		}
		else {
			final long currentTime = System.nanoTime();
			if ( isTimedOut( currentTime ) ) {
				//0 means no limit so we return the lowest possible value
				return 0l;
			}
			long left = timeout - ( currentTime - start);
			long result;
			if ( left % factor == 0 ) {
				result = left / factor;
			}
			else {
				result = (left / factor) + 1;
			}
			if ( result <= 0 ) {
				//0 means no limit so we return the lowest possible value
				return 0l;
			}
			else {
				return result;
			}
		}
	}

	@Override
	public boolean isTimedOut() {
		if ( timeout == null ) {
			return false;
		}
		if ( timedOut ) {
			return true;
		}
		return isTimedOut( System.nanoTime() );
	}

	private boolean isTimedOut(long currentTime) {
		if ( timeout == null ) {
			return false;
		}
		if ( timedOut ) {
			return true;
		}
		else {
			final long elapsedTime = currentTime - start;
			timedOut = elapsedTime > timeout;
			if ( this.type != Type.LIMIT && timedOut ) {
				throw timeoutExceptionFactory.createTimeoutException(
						"Full-text query took longer than expected (in microsecond): " + TimeUnit.NANOSECONDS.toMicros( elapsedTime ),
						String.valueOf( luceneQuery )
				);
			}
			return timedOut;
		}
	}

	@Override
	public void stop() {
		this.timeout = null;
		this.type = Type.NONE;
		//don't reset, we need it for the query API even when the manager is stopped.
		//this.partialResults = false;
	}

	@Override
	public void setTimeout(long timeout, TimeUnit timeUnit) {
		this.timeout = timeUnit.toNanos( timeout );
		//timeout of 0 means no more timeout
		if ( timeout == 0 ) {
			stop();
		}
	}

	public void forceTimedOut() {
		this.timedOut = Boolean.TRUE;
		if ( type == Type.LIMIT ) {
			//we stop where we are return what we have
			this.partialResults = true;
		}
	}

	@Override
	public void raiseExceptionOnTimeout() {
		if ( this.type == Type.LIMIT ) {
			throw new SearchException("Cannot define both setTimeout and limitFetchingTime on a full-text query. Please report your need to the Hibernate team");
		}
		this.type = Type.EXCEPTION;
	}

	@Override
	public void limitFetchingOnTimeout() {
		if ( this.type == Type.EXCEPTION ) {
			throw new SearchException("Cannot define both setTimeout and limitFetchingTime on a full-text query. Please report your need to the Hibernate team");
		}
		this.type = Type.LIMIT;
	}

	@Override
	public void reactOnQueryTimeoutExceptionWhileExtracting(RuntimeException e) {
		if ( type == Type.LIMIT ) {
			//we stop where we are return what we have
			this.partialResults = true;
		}
		else {
			if ( e == null ) {
				e = timeoutExceptionFactory.createTimeoutException(
						"Timeout period exceeded",
						String.valueOf( luceneQuery ) );
			}
			throw e;
		}
	}

	@Override
	public boolean hasPartialResults() {
		return partialResults;
	}

	@Override
	public Type getType() {
		return type;
	}

	public Counter getLuceneTimeoutCounter() {
		this.timingSource.ensureInitialized();
		return new LuceneCounterAdapter( timingSource );
	}

	/**
	 * Converts our generic TimingSource so that Lucene can use it as a Counter
	 */
	private static final class LuceneCounterAdapter extends org.apache.lucene.util.Counter {

		private final TimingSource timingSource;

		public LuceneCounterAdapter(TimingSource timingSource) {
			this.timingSource = timingSource;
		}

		@Override
		public long addAndGet(final long delta) {
			//parameter delta is ignored as we don't use the clock ticking strategy from Lucene's threads
			//as I don't want to deal with statically referenced threads.
			return timingSource.getMonotonicTimeEstimate();
		}

		@Override
		public long get() {
			return timingSource.getMonotonicTimeEstimate();
		}

	}

}
