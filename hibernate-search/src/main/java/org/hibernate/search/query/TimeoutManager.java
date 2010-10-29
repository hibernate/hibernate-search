package org.hibernate.search.query;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.Query;

import org.hibernate.QueryTimeoutException;

/**
 * @author Emmanuel Bernard
 */
public class TimeoutManager {
	// timeout in nanoseconds
	private Long timeout;
	private boolean bestEffort;
	private long start;
	boolean timedOut = false;
	private Query luceneQuery;

	public TimeoutManager() {
	}

	/** we start counting from this method call (if needed) */
	public void start(Query luceneQuery) {
		if ( timeout == null ) return;
		this.luceneQuery = luceneQuery;
		this.start = System.nanoTime();
	}

	public Long getTimeoutLeftInMilliseconds() {
		return getTimeoutLeft( 1000000 );
	}

	public Long getTimeoutLeftInSeconds() {
		return getTimeoutLeft(1000000000);
	}

	private Long getTimeoutLeft(long factor) {
		if (timeout == null) {
			return null;
		}
		else {
			long left = timeout - (System.nanoTime() - start);
			if ( left % factor == 0 ) {
				return left / factor;
			}
			else {
				return (left / factor) + 1;
			}
		}
	}

	public boolean isBestEffort() {
		return bestEffort;
	}

	public void setBestEffort(boolean bestEffort) {
		this.bestEffort = bestEffort;
	}

	public boolean isTimedOut() {
		if ( timeout == null ) return false;
		if ( timedOut ) {
			return true;
		}
		else {
			final long elapsedTime = System.nanoTime() - start;
			timedOut = elapsedTime > timeout;
			if ( ! bestEffort ) {
				throw new QueryTimeoutException(
						"Full-text query took longer than expected (in microsecond): " + TimeUnit.NANOSECONDS.toMicros( elapsedTime ),
						( SQLException) null,
						luceneQuery.toString()
				);
			}
			return timedOut;
		}
	}

	public void stop() {
		this.timeout = null;
	}

	public void setTimeout(long timeout, TimeUnit timeUnit) {
		this.timeout = timeUnit.toNanos( timeout );
	}
}
