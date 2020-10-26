/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.timing.spi;

/**
 * Common interface providing a deadline through the method {@link #remainingTimeToHardTimeout}.
 */
public interface Deadline {

	/**
	 * If no hard timeout is defined, returns {@code null}.
	 *
	 * @return the remaining time to hard timeout in milliseconds
	 * @throws org.hibernate.search.util.common.SearchTimeoutException If the timeout was reached and
	 * a hard timeout was requested.
	 */
	Long remainingTimeToHardTimeout();

	/**
	 * @param milliseconds The number of milliseconds until the deadline.
	 * @return An immutable {@link Deadline} which does not track the passing time.
	 * {@link #remainingTimeToHardTimeout()} will always return the same value.
	 */
	static Deadline ofMilliseconds(long milliseconds) {
		return new StaticDeadline( milliseconds );
	}

}
