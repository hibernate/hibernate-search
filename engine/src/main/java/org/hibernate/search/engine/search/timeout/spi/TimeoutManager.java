/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.timeout.spi;

import java.lang.invoke.MethodHandles;
import java.time.Duration;

import org.hibernate.search.engine.common.timing.impl.TimingSource;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class TimeoutManager {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public enum Type {
		NONE,
		EXCEPTION,
		LIMIT
	}

	protected final TimingSource timingSource;
	protected final Long timeoutMs;
	protected final Type type;

	private Long start;
	boolean timedOut = false;

	protected TimeoutManager(TimingSource timingSource, Long timeoutMs, Type type) {
		this.timingSource = timingSource;
		this.timeoutMs = timeoutMs;
		this.type = type;

		timingSource.ensureInitialized();
	}

	/**
	 * we start counting from this method call (if needed)
	 */
	public void start() {
		this.start = timingSource.monotonicTimeEstimate();
	}

	public void stop() {
		this.start = null;
	}

	public long getTimeoutBaseline() {
		return start;
	}

	/**
	 * @return {@code true} if the timeout was reached, {@code false} otherwise.
	 * @throws org.hibernate.search.util.common.SearchTimeoutException If the timeout was reached and
	 * a hard timeout was requested.
	 */
	public Long checkTimeLeftInMilliseconds() {
		if ( timeoutMs == null ) {
			return null;
		}
		else {
			final long elapsedTime = getElapsedTimeInMilliseconds();
			long timeLeft = timeoutMs - elapsedTime;
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

	public boolean hasHardTimeout() {
		return this.type == Type.EXCEPTION;
	}

	public Duration getTookTime() {
		return Duration.ofMillis( getElapsedTimeInMilliseconds() );
	}

	protected void onTimedOut() {
		throw log.timedOut( timeoutMs );
	}

	protected long getElapsedTimeInMilliseconds() {
		return timingSource.monotonicTimeEstimate() - start;
	}
}

