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

	public Long getTimeoutLeftInMilliseconds() {
		return getTimeoutLeft( 1000000 );
	}

	private Long getTimeoutLeft(long factor) {
		if ( timeout == null ) {
			return null;
		}
		else {
			final long currentTime = System.nanoTime();
			if ( isTimedOut( currentTime ) ) {
				//0 means no limit so we return the lowest possible value
				return 0L;
			}
			long left = timeout - ( currentTime - start );
			long result;
			if ( left % factor == 0 ) {
				result = left / factor;
			}
			else {
				result = ( left / factor ) + 1;
			}
			if ( result <= 0 ) {
				//0 means no limit so we return the lowest possible value
				return 0L;
			}
			else {
				return result;
			}
		}
	}

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
				throw log.timedOut( Duration.ofNanos( elapsedTime ), query.toString() );
			}
			return timedOut;
		}
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
		long deltaNanos = System.nanoTime() - start;
		return Duration.ofNanos( deltaNanos );
	}
}

