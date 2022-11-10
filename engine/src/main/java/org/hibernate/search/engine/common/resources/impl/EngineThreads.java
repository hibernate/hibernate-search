/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.resources.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.util.common.impl.Closer;

public class EngineThreads {

	private final ThreadPoolProvider threadPoolProvider;
	private ScheduledExecutorService timingExecutor;

	public EngineThreads(ThreadPoolProvider threadPoolProvider) {
		this.threadPoolProvider = threadPoolProvider;
	}

	public void onStop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ExecutorService::shutdownNow, timingExecutor );
		}
	}

	public ScheduledExecutorService getTimingExecutor() {
		// Lazy initialization - not all configurations need this executor
		ScheduledExecutorService executor = timingExecutor;
		if ( executor != null ) {
			return executor;
		}
		// this sync block shouldn't be a problem for Loom:
		// - sync happens only once on init ?
		// - no I/O and simple in-memory operations
		synchronized (this) {
			if ( timingExecutor != null ) {
				return timingExecutor;
			}
			this.timingExecutor = threadPoolProvider.newScheduledExecutor(
					1, "Engine - Timing thread"
			);
			return timingExecutor;
		}
	}
}
