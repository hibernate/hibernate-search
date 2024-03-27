/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulatedFailure {

	private static final SimulatedFailure INSTANCE = new SimulatedFailure();

	private final AtomicBoolean raiseExceptionOnNextRead = new AtomicBoolean( false );
	private final AtomicInteger raiseExceptionAfterXWrites = new AtomicInteger( -1 );

	private SimulatedFailure() {
	}

	public static void raiseExceptionOnNextRead() {
		INSTANCE.raiseExceptionOnNextRead.set( true );
	}

	public static void raiseExceptionAfterXWrites(int times) {
		INSTANCE.raiseExceptionAfterXWrites.set( times );
	}

	public static void reset() {
		INSTANCE.raiseExceptionOnNextRead.set( false );
		INSTANCE.raiseExceptionAfterXWrites.set( -1 );
	}

	public static void read() {
		if ( INSTANCE.raiseExceptionOnNextRead.compareAndSet( true, false ) ) {
			throw new SimulatedFailureException();
		}
	}

	public static void write() {
		if ( INSTANCE.raiseExceptionAfterXWrites.decrementAndGet() == 0 ) {
			throw new SimulatedFailureException();
		}
	}

	public static class SimulatedFailureException extends RuntimeException {
		private SimulatedFailureException() {
		}
	}
}
