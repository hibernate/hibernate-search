/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.resources.impl;

import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.util.common.AssertionFailure;

public class BackendThreads {

	private static final OptionalConfigurationProperty<Integer> THREAD_POOL_SIZE =
			ConfigurationProperty.forKey( LuceneBackendSettings.THREAD_POOL_SIZE )
					.asInteger()
					.build();

	private final String prefix;

	private ThreadPoolProvider threadPoolProvider;
	private ScheduledExecutorService writeExecutor;

	public BackendThreads(String prefix) {
		this.prefix = prefix;
	}

	public void onStart(ConfigurationPropertySource propertySource, ThreadPoolProvider threadPoolProvider) {
		if ( this.writeExecutor != null ) {
			// Already started
			return;
		}
		this.threadPoolProvider = threadPoolProvider;

		int threadPoolSize = THREAD_POOL_SIZE.get( propertySource )
				.orElse( Runtime.getRuntime().availableProcessors() );
		// We use a scheduled executor for write so that we perform all commits,
		// scheduled or not, in the *same* thread pool.
		this.writeExecutor = threadPoolProvider.newScheduledExecutor(
				threadPoolSize, prefix + " - Worker thread"
		);
	}

	public void onStop() {
		writeExecutor.shutdownNow();
	}

	public ThreadProvider getThreadProvider() {
		checkStarted();
		return threadPoolProvider.getThreadProvider();
	}

	public ScheduledExecutorService getWriteExecutor() {
		checkStarted();
		return writeExecutor;
	}

	private void checkStarted() {
		if ( writeExecutor == null ) {
			throw new AssertionFailure(
					"Attempt to retrieve the executor or related information before the backend was started."
							+ " There is probably a bug in Hibernate Search, please report it."
			);
		}
	}
}
