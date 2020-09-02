/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.timeout.spi;

/**
 * Common interface providing a deadline through the method {@link #remainingTimeToHardTimeout}.
 */
public interface RequestDeadline {

	/**
	 * If no hard timeout is defined, returns {@code null}.
	 *
	 * @return the remaining time to hard timeout in milliseconds
	 * @throws org.hibernate.search.util.common.SearchTimeoutException If the timeout was reached and
	 * a hard timeout was requested.
	 */
	Long remainingTimeToHardTimeout();

	/**
	 * Simple implementation of {@link RequestDeadline} which does not need to track the passed time.
	 * The {@code remainingTimeToHardTimeout} is immutable here.
	 */
	final class ImmutableRequestDeadline implements RequestDeadline {

		private final long remainingTimeToHardTimeout;

		public ImmutableRequestDeadline(long remainingTimeToHardTimeout) {
			this.remainingTimeToHardTimeout = remainingTimeToHardTimeout;
		}

		@Override
		public Long remainingTimeToHardTimeout() {
			return remainingTimeToHardTimeout;
		}
	}
}
