/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.resources.impl;

import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.work.spi.ElasticsearchWorkExecutorProvider;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.util.common.AssertionFailure;

public class BackendThreads {

	private static final ConfigurationProperty<
			BeanReference<? extends ElasticsearchWorkExecutorProvider>> BACKEND_WORK_EXECUTOR_PROVIDER =
					ConfigurationProperty.forKey( ElasticsearchBackendSpiSettings.Radicals.BACKEND_WORK_EXECUTOR_PROVIDER )
							.asBeanReference( ElasticsearchWorkExecutorProvider.class )
							.withDefault( ElasticsearchBackendSpiSettings.Defaults.BACKEND_WORK_EXECUTOR_PROVIDER )
							.build();
	private final String prefix;

	private ThreadPoolProvider threadPoolProvider;
	private SimpleScheduledExecutor workExecutor;

	public BackendThreads(String prefix) {
		this.prefix = prefix;
	}

	public void onStart(ConfigurationPropertySource propertySource, BeanResolver beanResolver,
			ThreadPoolProvider threadPoolProvider) {
		if ( this.workExecutor != null ) {
			// Already started
			return;
		}
		this.threadPoolProvider = threadPoolProvider;

		try ( BeanHolder<? extends ElasticsearchWorkExecutorProvider> provider = BACKEND_WORK_EXECUTOR_PROVIDER.getAndTransform(
				propertySource, beanResolver::resolve ) ) {
			this.workExecutor = provider.get().workExecutor( new ElasticsearchWorkExecutorProvider.Context() {
				@Override
				public ThreadPoolProvider threadPoolProvider() {
					return threadPoolProvider;
				}

				@Override
				public ConfigurationPropertySource propertySource() {
					return propertySource;
				}

				@Override
				public String recommendedThreadNamePrefix() {
					return prefix + " - Worker thread";
				}
			} );
		}
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
