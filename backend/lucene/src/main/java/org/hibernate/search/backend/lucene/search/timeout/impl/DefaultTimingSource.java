/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.timeout.impl;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.lucene.resources.impl.BackendThreads;
import org.hibernate.search.backend.lucene.search.timeout.spi.TimingSource;

/**
 * Default implementation for a {@link TimingSource}
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 * @since 4.1
 */
public final class DefaultTimingSource implements TimingSource {

	private static final long INVALID_TIME = -1;

	private final BackendThreads threads;

	//lazily initialize it, so we don't start a thread for those who don't use timeouts
	//guarded by synchronization on this
	private ScheduledFuture<?> future;
	private volatile long currentTimeApproximation = INVALID_TIME;

	public DefaultTimingSource(BackendThreads threads) {
		this.threads = threads;
	}

	@Override
	public long monotonicTimeEstimate() {
		// This method is very performance critical:
		// it's invoked once for each Lucene match during a query collection.
		// That's why we read the volatile resorting to a Timer and
		// approximating rather than invoking
		// the currentTime() directly.
		long currentValue = currentTimeApproximation;
		if ( currentValue == INVALID_TIME ) {
			throw new IllegalStateException( "Timing source was not started" );
		}
		return currentValue;
	}

	@Override
	public void ensureInitialized() {
		if ( future != null ) {
			return;
		}
		synchronized ( this ) {
			if ( future != null ) {
				return;
			}
			future = threads.getTimingExecutor()
					.scheduleAtFixedRate( new TriggerTask(), 5, 5, TimeUnit.MILLISECONDS );
			currentTimeApproximation = currentTime();
		}
	}

	@Override
	public synchronized void stop() {
		if ( future != null ) {
			future.cancel( false );
			future = null;
		}
		currentTimeApproximation = INVALID_TIME;
	}

	private class TriggerTask implements Runnable {
		@Override
		public void run() {
			DefaultTimingSource.this.currentTimeApproximation = currentTime();
		}
	}

	private static long currentTime() {
		return TimeUnit.MILLISECONDS.convert( System.nanoTime(), TimeUnit.NANOSECONDS );
	}

}
