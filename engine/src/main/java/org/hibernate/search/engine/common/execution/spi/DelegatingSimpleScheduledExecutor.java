/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.execution.spi;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public class DelegatingSimpleScheduledExecutor implements SimpleScheduledExecutor {

	private final ScheduledExecutorService delegate;
	private final boolean blocking;

	public DelegatingSimpleScheduledExecutor(ScheduledExecutorService delegate, boolean blocking) {
		this.delegate = delegate;
		this.blocking = blocking;
	}

	@Override
	public Future<?> submit(Runnable task) {
		return delegate.submit( task );
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return delegate.schedule( command, delay, unit );
	}

	@Override
	public void shutdownNow() {
		delegate.shutdownNow();
	}

	@Override
	public boolean isBlocking() {
		return blocking;
	}
}
