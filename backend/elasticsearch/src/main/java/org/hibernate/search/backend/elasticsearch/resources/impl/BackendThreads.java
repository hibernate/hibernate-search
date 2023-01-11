/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.resources.impl;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.engine.common.execution.SimpleScheduledExecutor;
import org.hibernate.search.engine.common.execution.impl.DelegatingSimpleScheduledExecutor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.util.common.AssertionFailure;

public class BackendThreads {

	private static final OptionalConfigurationProperty<Integer> THREAD_POOL_SIZE =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.THREAD_POOL_SIZE )
					.asIntegerStrictlyPositive()
					.build();

	private final String prefix;

	private ThreadPoolProvider threadPoolProvider;
	private SimpleScheduledExecutor workExecutor;

	public BackendThreads(String prefix) {
		this.prefix = prefix;
	}

	public void onStart(ConfigurationPropertySource propertySource, ThreadPoolProvider threadPoolProvider) {
		if ( this.workExecutor != null ) {
			// Already started
			return;
		}
		this.threadPoolProvider = threadPoolProvider;

		int threadPoolSize = THREAD_POOL_SIZE.get( propertySource )
				.orElse( Runtime.getRuntime().availableProcessors() );
		// We use a scheduled executor so that we can also schedule client timeouts in the same thread pool.
		this.workExecutor = new DelegatingSimpleScheduledExecutor(
				threadPoolProvider.newScheduledExecutor(
						threadPoolSize, prefix + " - Worker thread"
				)
		);
	}

	public void onStop() {
		if ( workExecutor != null ) {
			workExecutor.shutdownNow();
		}
	}

	public String getPrefix() {
		return prefix;
	}

	public ThreadProvider getThreadProvider() {
		checkStarted();
		return threadPoolProvider.threadProvider();
	}

	public SimpleScheduledExecutor getWorkExecutor() {
		checkStarted();
		return workExecutor;
	}

	private void checkStarted() {
		if ( workExecutor == null ) {
			throw new AssertionFailure(
					"Attempt to retrieve the executor or related information before the backend was started."
			);
		}
	}
}
