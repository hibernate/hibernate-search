package org.hibernate.search.query;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.Query;

import org.hibernate.QueryTimeoutException;
import org.hibernate.search.SearchException;

/**
 * @author Emmanuel Bernard
 */
public class TimeoutManager {
	// timeout in nanoseconds
	private Long timeout;
	private long start;
	boolean timedOut = false;
	private Query luceneQuery;
	private Type type;
	private boolean partialResults;


	public TimeoutManager() {
	}

	/** we start counting from this method call (if needed) */
	public void start(Query luceneQuery) {
		if ( timeout == null ) return;
		this.luceneQuery = luceneQuery;
		this.start = System.nanoTime();
		this.partialResults = false;
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

	public boolean isTimedOut() {
		if ( timeout == null ) return false;
		if ( timedOut ) {
			return true;
		}
		return isTimedOut( System.nanoTime() );
	}

	private boolean isTimedOut(long currentTime) {
		if ( timeout == null ) return false;
		if ( timedOut ) {
			return true;
		}
		else {
			final long elapsedTime = currentTime - start;
			timedOut = elapsedTime > timeout;
			if ( this.type != Type.LIMIT && timedOut ) {
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
		this.type = Type.NONE;
		//don't reset, we need it for the query API even when the manager is stopped.
		//this.partialResults = false;
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
		if ( type == Type.LIMIT) {
			//we stop where we are return what we have
			this.partialResults = true;
		}
	}

	public void raiseExceptionOnTimeout() {
		if ( this.type == Type.LIMIT ) {
			throw new SearchException("Cannot define both setTimeout and limitFetchingTime on a full-text query. Please report your need to the Hibernate team");
		}
		this.type = Type.EXCEPTION;
	}

	public void limitFetchingOnTimeout() {
		if ( this.type == Type.EXCEPTION ) {
			throw new SearchException("Cannot define both setTimeout and limitFetchingTime on a full-text query. Please report your need to the Hibernate team");
		}
		this.type = Type.LIMIT;
	}

	public void reactOnQueryTimeoutExceptionWhileExtracting(QueryTimeoutException e) {
		if ( type == Type.LIMIT) {
			//we stop where we are return what we have
			this.partialResults = true;
		}
		else {
			if ( e == null) {
				e = new QueryTimeoutException( "Timeout period exceeded", (SQLException) null, luceneQuery.toString() );
			}
			throw e;
		}
	}

	public boolean hasPartialResults() {
		return partialResults;
	}

	public Type getType() {
		return type;
	}

	public static enum Type {
		NONE,
		EXCEPTION,
		LIMIT
	}
}
