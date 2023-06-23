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

import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.impl.TimeHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class TimeoutManager {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static TimeoutManager of(TimingSource timingSource, Long timeout, TimeUnit timeUnit,
			boolean exceptionOnTimeout) {
		if ( timeout != null && timeUnit != null ) {
			if ( exceptionOnTimeout ) {
				return TimeoutManager.hardTimeout( timingSource, timeout, timeUnit );
			}
			else {
				return TimeoutManager.softTimeout( timingSource, timeout, timeUnit );
			}
		}
		return TimeoutManager.noTimeout( timingSource );
	}

	public static TimeoutManager noTimeout(TimingSource timingSource) {
		return new TimeoutManager( timingSource, null, null, Type.NONE );
	}

	public static TimeoutManager softTimeout(TimingSource timingSource, long timeout, TimeUnit timeUnit) {
		return new TimeoutManager( timingSource, timeout, timeUnit, Type.LIMIT );
	}

	public static TimeoutManager hardTimeout(TimingSource timingSource, long timeout, TimeUnit timeUnit) {
		return new TimeoutManager( timingSource, timeout, timeUnit, Type.EXCEPTION );
	}

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

	private Long monotonicTimeEstimateStart;
	private Long nanoTimeStart;

	public TimeoutManager(TimingSource timingSource, Long timeoutValue, TimeUnit timeoutUnit, Type type) {
		this.timingSource = timingSource;
		this.timeoutValue = timeoutValue;
		this.timeoutUnit = timeoutUnit;
		this.timeoutMs = TimeHelper.toMillisecondsRoundedUp( timeoutValue, timeoutUnit );
		this.type = type;
		this.deadline = timeoutMs == null ? null : new DynamicDeadline();

		if ( requireMonotonicTimeEstimate() ) {
			timingSource.ensureTimeEstimateIsInitialized();
		}
	}

	/**
	 * we start counting from this method call (if needed)
	 */
	public void start() {
		if ( requireMonotonicTimeEstimate() ) {
			monotonicTimeEstimateStart = timingSource.monotonicTimeEstimate();
		}

		nanoTimeStart = timingSource.nanoTime();
	}

	public void stop() {
		monotonicTimeEstimateStart = null;
		nanoTimeStart = null;
	}

	public TimingSource timingSource() {
		return timingSource;
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
		return monotonicTimeEstimateStart;
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
		return deadline.checkRemainingTimeMillis() <= 0;
	}

	public boolean hasHardTimeout() {
		return this.type == Type.EXCEPTION;
	}

	/**
	 * Returns the time passed from the start with high precision.
	 * This method may be performance expensive.
	 *
	 * @return high precision duration of took time.
	 */
	public Duration tookTime() {
		return Duration.ofNanos( timingSource.nanoTime() - nanoTimeStart );
	}

	protected long elapsedTimeEstimateMillis() {
		return timingSource.monotonicTimeEstimate() - monotonicTimeEstimateStart;
	}

	private boolean requireMonotonicTimeEstimate() {
		return !Type.NONE.equals( type );
	}

	final class DynamicDeadline implements Deadline {
		boolean timedOut = false;

		@Override
		public long checkRemainingTimeMillis() {
			final long elapsedTime = elapsedTimeEstimateMillis();
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

