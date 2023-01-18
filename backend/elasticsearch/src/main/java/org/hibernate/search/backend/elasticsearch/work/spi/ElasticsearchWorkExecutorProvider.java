/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.spi;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface ElasticsearchWorkExecutorProvider {

	SimpleScheduledExecutor workExecutor(Context context);

	interface Context {
		/**
		 * @return A provider of thread pools.
		 */
		ThreadPoolProvider threadPoolProvider();

		/**
		 * Gives access to various configuration properties that might be useful during executor instantiation.
		 */
		ConfigurationPropertySource propertySource();

		/**
		 * @return recommended thread name prefix that can be passed to work executor. Recommendation is based on the
		 * instantiation context.
		 */
		String recommendedThreadNamePrefix();
	}

}
