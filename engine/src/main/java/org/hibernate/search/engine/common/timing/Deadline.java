/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.timing;

import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Common interface providing a deadline through the method {@link #checkRemainingTimeMillis}.
 */
@Incubating
public interface Deadline {

	/**
	 * @return The remaining time to the deadline in milliseconds.
	 * @throws org.hibernate.search.util.common.SearchTimeoutException If the deadline was reached
	 * and it's a hard deadline requiring immediate failure.
	 */
	long checkRemainingTimeMillis();

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

}
