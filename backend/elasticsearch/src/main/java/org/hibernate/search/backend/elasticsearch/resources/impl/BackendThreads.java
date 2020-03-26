/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.resources.impl;

import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.util.common.AssertionFailure;

public class BackendThreads {

	private final String prefix;

	private ThreadPoolProvider threadPoolProvider;
	private ScheduledExecutorService workExecutor;

	public BackendThreads(String prefix) {
		this.prefix = prefix;
	}

	public void onStart(ThreadPoolProvider threadPoolProvider) {
		if ( this.workExecutor != null ) {
			// Already started
			return;
		}
		this.threadPoolProvider = threadPoolProvider;
		// We use a scheduled executor so that we can also schedule client timeouts in the same thread pool.
		// TODO HSEARCH-3575 make the thread pool size configurable
		this.workExecutor = threadPoolProvider.newScheduledExecutor(
				Runtime.getRuntime().availableProcessors(), prefix
		);
	}

	public void onStop() {
		workExecutor.shutdownNow();
	}

	public ThreadProvider getThreadProvider() {
		checkStarted();
		return threadPoolProvider.getThreadProvider();
	}

	public ScheduledExecutorService getWorkExecutor() {
		checkStarted();
		return workExecutor;
	}

	private void checkStarted() {
		if ( workExecutor == null ) {
			throw new AssertionFailure(
					"Attempt to retrieve the executor or related information before the backend was started."
							+ " There is probably a bug in Hibernate Search, please report it."
			);
		}
	}
}
