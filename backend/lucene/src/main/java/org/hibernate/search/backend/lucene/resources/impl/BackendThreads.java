/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.resources.impl;

import org.hibernate.search.backend.lucene.cfg.spi.LuceneBackendSpiSettings;
import org.hibernate.search.backend.lucene.work.spi.LuceneWorkExecutorProvider;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;

public class BackendThreads {

	private static final ConfigurationProperty<
			BeanReference<? extends LuceneWorkExecutorProvider>> BACKEND_WORK_EXECUTOR_PROVIDER =
					ConfigurationProperty.forKey( LuceneBackendSpiSettings.Radicals.BACKEND_WORK_EXECUTOR_PROVIDER )
							.asBeanReference( LuceneWorkExecutorProvider.class )
							.withDefault( LuceneBackendSpiSettings.Defaults.BACKEND_WORK_EXECUTOR_PROVIDER )
							.build();
	private final String prefix;

	private ThreadPoolProvider threadPoolProvider;
	private SimpleScheduledExecutor writeExecutor;

	public BackendThreads(String prefix) {
		this.prefix = prefix;
	}

	public void onStart(ConfigurationPropertySource propertySource, BeanResolver beanResolver,
			ThreadPoolProvider threadPoolProvider) {
		if ( this.writeExecutor != null ) {
			// Already started
			return;
		}
		this.threadPoolProvider = threadPoolProvider;


		try ( BeanHolder<? extends LuceneWorkExecutorProvider> provider = BACKEND_WORK_EXECUTOR_PROVIDER.getAndTransform(
				propertySource, beanResolver::resolve ) ) {
			this.writeExecutor = provider.get().writeExecutor( new LuceneWorkExecutorProvider.Context() {
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
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( SimpleScheduledExecutor::shutdownNow, writeExecutor );
		}
	}

	public ThreadProvider getThreadProvider() {
		checkStarted();
		return threadPoolProvider.threadProvider();
	}

	public SimpleScheduledExecutor getWriteExecutor() {
		checkStarted();
		return writeExecutor;
	}

	private void checkStarted() {
		if ( writeExecutor == null ) {
			throw new AssertionFailure(
					"Attempt to retrieve the executor or related information before the backend was started."
			);
		}
	}
}
