/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.timeout.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
	public static final int NANOS_IN_ONE_MILLI = 1000000;

	private final Query query;

	// timeout in nanoseconds
	private Long timeout;
	private long start;
	boolean timedOut = false;
	private Type type;
	private boolean partialResults;

	public TimeoutManagerImpl(Query query) {
		this.query = query;
	}

	/**
	 * we start counting from this method call (if needed)
	 */
	@Override
	public void start() {
		this.start = System.nanoTime();
		if ( timeout == null ) {
			return;
		}
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

	@Override
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
	public void reactOnQueryTimeoutExceptionWhileExtracting(RuntimeException e) {
		if ( type == Type.LIMIT ) {
			//we stop where we are return what we have
			this.partialResults = true;
		}
		else {
			if ( e == null ) {
				e = log.timeoutPeriodExceeded( query.toString() );
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

	@Override
	public Integer getTookTimeInMilliseconds() {
		return BigDecimal.valueOf( System.nanoTime() - start )
				.divide( BigDecimal.valueOf( NANOS_IN_ONE_MILLI ), RoundingMode.CEILING )
				.intValue();
	}
}

