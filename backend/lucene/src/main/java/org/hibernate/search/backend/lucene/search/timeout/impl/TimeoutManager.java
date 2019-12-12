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
import org.hibernate.search.backend.lucene.search.timeout.spi.TimingSource;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;
import org.apache.lucene.util.Counter;

/**
 * @author Emmanuel Bernard
 */
public final class TimeoutManager {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static TimeoutManager noTimeout(TimingSource timingSource, Query query) {
		return new TimeoutManager( timingSource, query, null, Type.NONE );
	}

	public static TimeoutManager softTimeout(TimingSource timingSource, Query query, long timeout, TimeUnit timeUnit) {
		return new TimeoutManager( timingSource, query, timeUnit.toMillis( timeout ), Type.LIMIT );
	}

	public static TimeoutManager hardTimeout(TimingSource timingSource, Query query, long timeout, TimeUnit timeUnit) {
		return new TimeoutManager( timingSource, query, timeUnit.toMillis( timeout ), Type.EXCEPTION );
	}

	private enum Type {
		NONE,
		EXCEPTION,
		LIMIT;
	}

	private final TimingSource timingSource;
	private final Query query;
	private final Long timeout;
	private final Type type;

	private Long start;
	boolean timedOut = false;

	private TimeoutManager(TimingSource timingSource, Query query, Long timeout, Type type) {
		this.timingSource = timingSource;
		this.query = query;
		this.timeout = timeout;
		this.type = type;

		timingSource.ensureInitialized();
	}

	/**
	 * we start counting from this method call (if needed)
	 */
	public void start() {
		this.start = timingSource.getMonotonicTimeEstimate();
	}

	public void stop() {
		this.start = null;
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
			final long elapsedTime = getElapsedTimeInMilliseconds();
			long timeLeft = timeout - elapsedTime;
			if ( timeLeft <= 0 ) {
				forceTimedOut();
				// Timed out: don't return a negative number.
				return 0L;
			}
			else {
				return timeLeft;
			}
		}
	}

	public Counter createCounter() {
		return new LuceneCounterAdapter( timingSource );
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

	public void forceTimedOut() {
		this.timedOut = Boolean.TRUE;
		onTimedOut();
	}

	private void onTimedOut() {
		if ( hasHardTimeout() ) {
			throw log.timedOut( getTookTime(), query.toString() );
		}
	}

	public boolean hasHardTimeout() {
		return this.type == Type.EXCEPTION;
	}

	public Duration getTookTime() {
		return Duration.ofMillis( getElapsedTimeInMilliseconds() );
	}

	private long getElapsedTimeInMilliseconds() {
		return timingSource.getMonotonicTimeEstimate() - start;
	}
}

