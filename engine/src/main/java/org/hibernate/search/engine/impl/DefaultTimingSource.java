/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.spi.TimingSource;

/**
 * Default implementation for a {@link org.hibernate.search.engine.spi.TimingSource}
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 * @since 4.1
 */
public final class DefaultTimingSource implements TimingSource {

	private static final long INVALID_TIME = -1;

	//lazily initialize it, so we don't start a thread for those who don't use timeouts
	//guarded by synchronization on this
	private Timer timer = null;
	private volatile long currentTimeApproximation = INVALID_TIME;

	@Override
	public long getMonotonicTimeEstimate() {
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
	public synchronized void ensureInitialized() {
		if ( timer == null ) {
			timer = new Timer( "HibernateSearch_QueryTimeoutMonitor", true );
			timer.schedule( new TriggerTask(), 5, 5 );
			currentTimeApproximation = currentTime();
		}
	}

	@Override
	public synchronized void stop() {
		if ( timer != null ) {
			timer.cancel();
		}
		currentTimeApproximation = INVALID_TIME;
	}

	private class TriggerTask extends TimerTask {

		@Override
		public void run() {
			DefaultTimingSource.this.currentTimeApproximation = currentTime();
		}

	}

	private static long currentTime() {
		return TimeUnit.MILLISECONDS.convert( System.nanoTime(), TimeUnit.NANOSECONDS );
	}

}
