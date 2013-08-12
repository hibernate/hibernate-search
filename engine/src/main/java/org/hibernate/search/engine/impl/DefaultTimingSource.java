/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.engine.impl;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.spi.TimingSource;

/**
 * Default implementation for a {@link org.hibernate.search.engine.spi.TimingSource}
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
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
