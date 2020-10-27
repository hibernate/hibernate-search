/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.timeout.spi;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.common.timing.spi.Deadline;
import org.hibernate.search.util.common.SearchTimeoutException;
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
	protected final Long timeoutValue;
	protected final TimeUnit timeoutUnit;
	protected final Long timeoutMs;
	protected final Type type;
	private final DynamicDeadline deadline;

	private Long start;

	public TimeoutManager(TimingSource timingSource, Long timeoutValue, TimeUnit timeoutUnit, Type type) {
		this.timingSource = timingSource;
		this.timeoutValue = timeoutValue;
		this.timeoutUnit = timeoutUnit;
		this.timeoutMs = timeoutUnit == null ? null : timeoutUnit.toMillis( timeoutValue );
		this.type = type;
		this.deadline = timeoutMs == null ? null : new DynamicDeadline();

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

	/**
	 * @return The deadline for the timeout defined by this timeout manager,
	 * {@code null} if no timeout is set.
	 */
	public Deadline deadlineOrNull() {
		return this.deadline;
	}

	/**
	 * @return The hard deadline defined by this timeout manager,
	 * i.e. the deadline beyond which an exception should be thrown.
	 * {@code null} if no hard timeout is set.
	 */
	public Deadline hardDeadlineOrNull() {
		if ( !hasHardTimeout() ) {
			return null;
		}
		return this.deadline;
	}

	public long timeoutBaseline() {
		return start;
	}

	/**
	 * @return {@code true} if the timeout was reached in a previous call to {@link #checkTimedOut()},
	 * {@code false} otherwise.
	 */
	public boolean isTimedOut() {
		return deadline != null && deadline.timedOut;
	}

	/**
	 * @return {@code true} if the timeout was reached, {@code false} otherwise.
	 * @throws org.hibernate.search.util.common.SearchTimeoutException If the timeout was reached and
	 * a hard timeout was requested.
	 */
	public boolean checkTimedOut() {
		if ( deadline == null ) {
			return false;
		}
		return deadline.remainingTimeMillis() <= 0;
	}

	public boolean hasHardTimeout() {
		return this.type == Type.EXCEPTION;
	}

	public Duration tookTime() {
		return Duration.ofMillis( elapsedTimeInMilliseconds() );
	}

	protected long elapsedTimeInMilliseconds() {
		return timingSource.monotonicTimeEstimate() - start;
	}

	final class DynamicDeadline implements Deadline {
		boolean timedOut = false;

		@Override
		public long remainingTimeMillis() {
			final long elapsedTime = elapsedTimeInMilliseconds();
			long timeLeft = timeoutMs - elapsedTime;
			if ( timeLeft <= 0 ) {
				forceTimeout( null );
				// Timed out: don't return a negative number.
				return 0L;
			}
			else {
				return timeLeft;
			}
		}

		@Override
		public void forceTimeout(Exception cause) {
			if ( hasHardTimeout() ) {
				throw forceTimeoutAndCreateException( cause );
			}
			else {
				this.timedOut = true;
			}
		}

		@Override
		public SearchTimeoutException forceTimeoutAndCreateException(Exception cause) {
			this.timedOut = true;
			return log.timedOut( Duration.ofNanos( timeoutUnit.toNanos( timeoutValue ) ), cause );
		}
	}
}

