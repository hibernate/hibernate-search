/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.timeout.impl;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public final class TimeoutManager {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public enum Type {
		NONE,
		EXCEPTION,
		LIMIT
	}

	private final Query query;

	// timeout in nanoseconds
	private Long timeout;
	private long start;
	boolean timedOut = false;
	private Type type;

	public TimeoutManager(Query query) {
		this.query = query;
	}

	/**
	 * we start counting from this method call (if needed)
	 */
	public void start() {
		this.start = System.nanoTime();
	}

	/**
	 * @return {@code true} if the timeout was reached, {@code false} otherwise.
	 * @throws org.hibernate.search.util.common.SearchTimeoutException If the timeout was reached and
	 * a hard timeout was requested.
	 */
	public Long checkTimeLeftInMilliseconds() {
		if ( timeout == null ) {
			return null;
		}
		else {
			final long elapsedTimeNanos = getElapsedTimeInNanoseconds();
			long timeLeftNanos = timeout - elapsedTimeNanos;
			long timeLeftMillis;
			if ( timeLeftNanos % 1_000_000 == 0 ) {
				timeLeftMillis = timeLeftNanos / 1_000_000;
			}
			else {
				timeLeftMillis = ( timeLeftNanos / 1_000_000 ) + 1;
			}
			if ( timeLeftMillis <= 0 ) {
				forceTimedOut();
				// Timed out: don't return a negative number.
				return 0L;
			}
			else {
				return timeLeftMillis;
			}
		}
	}

	/**
	 * @return {@code true} if the timeout was reached in a previous call to {@link #checkTimedOut()},
	 * {@code false} otherwise.
	 */
	public boolean isTimedOut() {
		return timedOut;
	}

	/**
	 * @return {@code true} if the timeout was reached, {@code false} otherwise.
	 * @throws org.hibernate.search.util.common.SearchTimeoutException If the timeout was reached and
	 * a hard timeout was requested.
	 */
	public boolean checkTimedOut() {
		Long timeLeft = checkTimeLeftInMilliseconds();
		return timeLeft != null && timeLeft <= 0;
	}

	public void stop() {
		this.timeout = null;
		this.type = Type.NONE;
	}

	public void setTimeout(long timeout, TimeUnit timeUnit) {
		this.timeout = timeUnit.toNanos( timeout );
		//timeout of 0 means no more timeout
		if ( timeout == 0 ) {
			stop();
		}
	}

	public void forceTimedOut() {
		this.timedOut = Boolean.TRUE;
		onTimedOut();
	}

	private void onTimedOut() {
		if ( this.type == Type.EXCEPTION ) {
			throw log.timedOut( getTookTime(), query.toString() );
		}
	}

	public void raiseExceptionOnTimeout() {
		if ( this.type == Type.LIMIT ) {
			throw log.raiseExceptionOrLimitFetching();
		}
		this.type = Type.EXCEPTION;
	}

	public void limitFetchingOnTimeout() {
		if ( this.type == Type.EXCEPTION ) {
			throw log.raiseExceptionOrLimitFetching();
		}
		this.type = Type.LIMIT;
	}

	public Type getType() {
		return type;
	}

	public Duration getTookTime() {
		return Duration.ofNanos( getElapsedTimeInNanoseconds() );
	}

	private long getElapsedTimeInNanoseconds() {
		return System.nanoTime() - start;
	}
}

