/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.testsupport.concurrency;

import java.util.concurrent.TimeUnit;

/**
 * A utility allowing to re-execute a piece of code in the current thread until it "works".
 * <p>
 * Meant to be used with lambdas.
 *
 * @see #pollAssertion(ThrowingRunnable)
 *
 * @author Yoann Rodiere
 */
public final class Poller {

	public static Poller milliseconds(long timeoutMs, long stepMs) {
		return create( timeoutMs, stepMs, TimeUnit.MILLISECONDS );
	}

	public static Poller nanoseconds(long timeoutNanos, long stepNanos) {
		return create( timeoutNanos, stepNanos, TimeUnit.NANOSECONDS );
	}

	private static Poller create(long timeoutValue, long stepValue, TimeUnit timeUnit) {
		return new Poller( timeUnit.toNanos( timeoutValue ), timeUnit.toNanos( stepValue ) );
	}

	private final long timeoutValueNanos;
	private final long stepValueNanos;

	private Poller(long timeoutValueNanos, long stepValueNanos) {
		this.timeoutValueNanos = timeoutValueNanos;
		this.stepValueNanos = stepValueNanos;
	}

	/**
	 * Run the given statement repeatedly until it doesn't throw any {@link AssertionError},
	 * or throw the {@link AssertionError} if the timeout is reached.
	 */
	public <E extends Exception> void pollAssertion(ThrowingRunnable<E> runnable) throws E {
		long initialNanoTime = System.nanoTime();
		long deadline = initialNanoTime + timeoutValueNanos;

		int nbOfFailedAttempts = 0;

		AssertionError assertionError;
		do {
			sleepStep();

			try {
				runnable.run();
				return; // Don't wait any more: the assertion passed, we're good to go.
			}
			catch (AssertionError e) {
				/*
				 * Only keep the very last assertion error, because it's probably the most relevant:
				 * the runnable may perform multiple assertions, and for instance the first assertion
				 * might fail for 2s, then be okay, the second assertion may become okay after 3s, etc.
				 * In the end what we want to know is which assertions was failing when we timed out.
				 */
				assertionError = e;
				++nbOfFailedAttempts;
			}
		}
		while ( System.nanoTime() < deadline );

		long timeSpentNanos = System.nanoTime() - initialNanoTime;
		throw newAssertionPollingError( assertionError, nbOfFailedAttempts, timeSpentNanos );
	}

	private void sleepStep() {
		try {
			TimeUnit.NANOSECONDS.sleep( stepValueNanos );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException( "Interrupted while waiting for an assertion to pass.", e );
		}
	}

	public interface ThrowingRunnable<E extends Exception> {
		void run() throws E;
	}

	private AssertionError newAssertionPollingError(AssertionError lastAssertionError, int nbOfFailedAttempts,
			long timeSpentNanos) {
		AssertionError error = new AssertionError(
				"Assertion failed even after " + nbOfFailedAttempts + " attempts in " + timeSpentNanos + "ns : "
						+ lastAssertionError.getMessage()
		);
		error.initCause( lastAssertionError );
		return error;
	}
}
