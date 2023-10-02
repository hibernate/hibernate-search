/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.timing.spi;

import java.lang.invoke.MethodHandles;
import java.time.Duration;

import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class StaticDeadline implements Deadline {

	/**
	 * @param milliseconds The number of milliseconds until the deadline.
	 * @return An immutable {@link Deadline} which does not track the passing time.
	 * {@link #checkRemainingTimeMillis()} will always return the same value
	 * and will never throw an exception.
	 */
	public static Deadline ofMilliseconds(long milliseconds) {
		return new StaticDeadline( milliseconds );
	}

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final long remainingTimeMillis;

	private StaticDeadline(long remainingTimeMillis) {
		this.remainingTimeMillis = remainingTimeMillis;
	}

	@Override
	public long checkRemainingTimeMillis() {
		return remainingTimeMillis;
	}

	@Override
	public void forceTimeout(Exception cause) {
		throw forceTimeoutAndCreateException( cause );
	}

	@Override
	public SearchTimeoutException forceTimeoutAndCreateException(Exception cause) {
		return log.timedOut( Duration.ofMillis( remainingTimeMillis ), cause );
	}
}
