/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.timing.spi;

/**
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 * @since 4.1
 */
public interface TimingSource {

	/**
	 * Returns an approximation of {@link System#nanoTime()}.
	 * Performance should be preferred over accuracy by the implementation, but the value is monotonic
	 * and expresses time in milliseconds, however, subsequent invocations could return the same value.
	 *
	 * @return an increasing value related to time in milliseconds. Only meaningful to compare time intervals, with no guarantees of high precision.
	 */
	long monotonicTimeEstimate();

	/**
	 * Invoked on engine integration shutdown. There is no start method as it's expected to be lazily initialized
	 */
	void stop();

	/**
	 * Needs to be invoked at least once before {@link #monotonicTimeEstimate()} can be used.
	 * Safe to be invoked multiple times.
	 */
	void ensureTimeEstimateIsInitialized();

	/**
	 * Returns current system time in nanoseconds.
	 * Accuracy is preferred over performance by the implementation.
	 *
	 * @return high precision current time in nanoseconds.
	 */
	long nanoTime();

}
