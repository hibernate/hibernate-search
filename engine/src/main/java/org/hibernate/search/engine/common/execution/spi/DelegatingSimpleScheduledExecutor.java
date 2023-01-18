/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	public DelegatingSimpleScheduledExecutor(ScheduledExecutorService executorService) {
		this.delegate = executorService;
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
}
