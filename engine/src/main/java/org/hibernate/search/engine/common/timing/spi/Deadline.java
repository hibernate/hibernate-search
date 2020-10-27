/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.timing.spi;

import org.hibernate.search.util.common.SearchTimeoutException;

/**
 * Common interface providing a deadline through the method {@link #remainingTimeMillis}.
 */
public interface Deadline {

	/**
	 * @return The remaining time to the deadline in milliseconds.
	 * @throws org.hibernate.search.util.common.SearchTimeoutException If the deadline was reached
	 * and it's a hard deadline requiring immediate failure.
	 */
	long remainingTimeMillis();

	/**
	 * @param cause The cause of the timeout, or {@code null}.
	 * @throws org.hibernate.search.util.common.SearchTimeoutException If the deadline is
	 * a hard deadline requiring immediate failure.
	 */
	void forceTimeout(Exception cause);

	/**
	 * @param cause The cause of the timeout, or {@code null}.
	 * @return An exception to be thrown on timeout.
	 */
	SearchTimeoutException forceTimeoutAndCreateException(Exception cause);

	/**
	 * @param milliseconds The number of milliseconds until the deadline.
	 * @return An immutable {@link Deadline} which does not track the passing time.
	 * {@link #remainingTimeMillis()} will always return the same value
	 * and will never throw an exception.
	 */
	static Deadline ofMilliseconds(long milliseconds) {
		return new StaticDeadline( milliseconds );
	}

}
