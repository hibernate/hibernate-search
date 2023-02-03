/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.resources.impl;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.work.spi.LuceneWorkExecutorProvider;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.common.execution.spi.DelegatingSimpleScheduledExecutor;

public class DefaultLuceneWorkExecutorProvider implements LuceneWorkExecutorProvider {

	public static final String DEFAULT_BEAN_NAME = "lucene-built-in";

	private static final OptionalConfigurationProperty<Integer> THREAD_POOL_SIZE =
			ConfigurationProperty.forKey( LuceneBackendSettings.THREAD_POOL_SIZE )
					.asIntegerStrictlyPositive()
					.build();

	@Override
	public SimpleScheduledExecutor writeExecutor(Context context) {
		int threadPoolSize = THREAD_POOL_SIZE.get( context.propertySource() )
				.orElse( Runtime.getRuntime().availableProcessors() );

		// We use a scheduled executor for write so that we perform all commits,
		// scheduled or not, in the *same* thread pool.
		return new DelegatingSimpleScheduledExecutor(
				context.threadPoolProvider().newScheduledExecutor(
						threadPoolSize,
						context.recommendedThreadNamePrefix()
				),
				context.threadPoolProvider().isScheduledExecutorBlocking()
		);
	}
}
