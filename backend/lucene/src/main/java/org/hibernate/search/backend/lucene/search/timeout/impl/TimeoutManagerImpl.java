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
import org.hibernate.search.backend.lucene.search.timeout.spi.TimeoutManager;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public class TimeoutManagerImpl implements TimeoutManager {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Query query;

	// timeout in nanoseconds
	private Long timeout;
	private long start;
	boolean timedOut = false;
	private Type type;

	public TimeoutManagerImpl(Query query) {
		this.query = query;
	}

	/**
	 * we start counting from this method call (if needed)
	 */
	@Override
	public void start() {
		this.start = System.nanoTime();
	}

	@Override
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
				throw log.timedOut( Duration.ofNanos( elapsedTime ), query.toString() );
			}
			return timedOut;
		}
	}

	@Override
	public void stop() {
		this.timeout = null;
		this.type = Type.NONE;
	}

	@Override
	public void setTimeout(long timeout, TimeUnit timeUnit) {
		this.timeout = timeUnit.toNanos( timeout );
		//timeout of 0 means no more timeout
		if ( timeout == 0 ) {
			stop();
		}
	}

	@Override
	public void forceTimedOut() {
		this.timedOut = Boolean.TRUE;
	}

	@Override
	public void raiseExceptionOnTimeout() {
		if ( this.type == Type.LIMIT ) {
			throw log.raiseExceptionOrLimitFetching();
		}
		this.type = Type.EXCEPTION;
	}

	@Override
	public void limitFetchingOnTimeout() {
		if ( this.type == Type.EXCEPTION ) {
			throw log.raiseExceptionOrLimitFetching();
		}
		this.type = Type.LIMIT;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public Duration getTookTime() {
		long deltaNanos = System.nanoTime() - start;
		return Duration.ofNanos( deltaNanos );
	}
}

